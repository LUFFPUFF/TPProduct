package com.example.domain.api.ans_api_module.nlp.speller.service;

import com.example.domain.api.ans_api_module.nlp.metrics.CorrectionMetrics;
import com.example.domain.api.ans_api_module.nlp.metrics.StatisticsService;
import com.example.domain.api.ans_api_module.nlp.punctuation_rule.correction.PunctuationCorrector;
import com.example.domain.api.ans_api_module.nlp.punctuation_rule.correction.TextAnalysis;
import com.example.domain.api.ans_api_module.nlp.punctuation_rule.correction_models.*;
import com.example.domain.api.ans_api_module.nlp.speller.response.SpellerResponse;
import com.example.domain.api.ans_api_module.nlp.speller.client.SpellCheckerClient;
import io.micrometer.core.annotation.Timed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Service
@ConditionalOnProperty(name = "correction.grammar.enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class GrammarCorrectionService {
    private static final int PROCESSING_TIMEOUT_MS = 500;

    private final SpellCheckerClient spellCheckerClient;
    private final TextAnalysis textAnalyzer;
    private final StatisticsService statsService;
    private final PunctuationCorrector punctuationCorrector;
    private final CorrectionMetrics metrics;
    private final ExecutorService executor;

    @Autowired
    public GrammarCorrectionService(SpellCheckerClient spellCheckerClient,
                                    TextAnalysis textAnalyzer,
                                    StatisticsService statsService,
                                    PunctuationCorrector punctuationCorrector,
                                    CorrectionMetrics metrics,
                                    @Qualifier("correctionExecutor") ExecutorService executor) {
        this.spellCheckerClient = spellCheckerClient;
        this.textAnalyzer = textAnalyzer;
        this.statsService = statsService;
        this.punctuationCorrector = punctuationCorrector;
        this.metrics = metrics;
        this.executor = executor;
    }

    public CorrectedText correctText(String text, String lang) {
        if (text == null || text.isBlank()) {

        }

        try {
            String preprocessed = preprocessText(text);

            List<TextSegment> segments = textAnalyzer.segment(preprocessed);

            List<CorrectedSegment> correctedSegments = new ArrayList<>();
            for (TextSegment segment : segments) {
                CorrectedSegment corrected = processSegment(segment, lang);
                if (corrected != null) {
                    correctedSegments.add(corrected);
                }
            }

            return assembleResult(correctedSegments);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    private CorrectedSegment processSegment(TextSegment segment, String lang) {
        try {
            String original = segment.getText();

            String spellCorrected = correctSpelling(original, lang);

            CorrectionResult punctuationResult = punctuationCorrector.correct(spellCorrected, lang);
            String punctCorrected = punctuationResult.correctedText();

            String grammarCorrected = applyGrammarRules(punctCorrected, textAnalyzer);

            List<Correction> corrections = new ArrayList<>();
            corrections.addAll(TextDiff.compare(original, spellCorrected, CorrectionType.SPELLING));
            corrections.addAll(punctuationResult.corrections());
            corrections.addAll(TextDiff.compare(punctCorrected, grammarCorrected, CorrectionType.GRAMMAR));

            return new CorrectedSegment(
                    segment.getStartPos(),
                    segment.getEndPos(),
                    grammarCorrected,
                    corrections
            );

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private CorrectedSegment processSingleSegment(TextSegment segment, String lang, TextAnalysis analysis) {
        try {
            String original = segment.getText();

            String spellCorrected = correctSpelling(original, lang);

            CorrectionResult punctuationResult = punctuationCorrector.correct(spellCorrected, lang);
            String punctuationCorrected = punctuationResult.correctedText();

            String grammarChecked = applyGrammarRules(punctuationCorrected, analysis);

            List<Correction> corrections = mergeCorrections(
                    original,
                    spellCorrected,
                    punctuationResult,
                    grammarChecked
            );

            metrics.incrementSegmentProcessed();
            statsService.recordSegmentCorrections(corrections);

            return new CorrectedSegment(
                    segment.getStartPos(),
                    segment.getEndPos(),
                    grammarChecked,
                    corrections
            );
        } catch (Exception e) {
            metrics.incrementErrors("segment_processing");
            System.out.println("Segment processing failed: " + e.getMessage());
            return new CorrectedSegment(
                    segment.getStartPos(),
                    segment.getEndPos(),
                    segment.getText(),
                    List.of()
            );
        }
    }

    private CorrectedSegment handleFuture(CompletableFuture<CorrectedSegment> future) {
        try {
            return future.get(PROCESSING_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            metrics.incrementErrors("timeout");
            return null;
        } catch (Exception e) {
            metrics.incrementErrors("segment_error");
            return null;
        }
    }

    private String preprocessText(String text) {
        return text.replaceAll("\\s+", " ")
                .replaceAll("\u00A0", " ")
                .replaceAll("\\s([.,!?;:])", "$1")
                .trim();
    }

    private String applyGrammarRules(String text, TextAnalysis analysis) {
        //TODO логика проверки граматики
        return text;
    }

    private List<Correction> mergeCorrections(
            String originalText,
            String spellCorrected,
            CorrectionResult punctuationResult,
            String finalText
    ) {
        List<Correction> allCorrections = new ArrayList<>();

        allCorrections.addAll(TextDiff.compare(originalText, spellCorrected, CorrectionType.SPELLING));

        allCorrections.addAll(punctuationResult.corrections());

        allCorrections.addAll(TextDiff.compare(punctuationResult.correctedText(), finalText, CorrectionType.GRAMMAR));

        return allCorrections;
    }

    private String correctSpelling(String text, String lang) {
        List<SpellerResponse> errors = spellCheckerClient.checkText(text, lang);
        return applySpellingCorrections(text, errors);
    }

    private String applySpellingCorrections(String text, List<SpellerResponse> errors) {
        if (errors.isEmpty() || text == null) {
            return text;
        }

        List<SpellerResponse> sortedErrors = errors.stream()
                .sorted(Comparator.comparingInt(SpellerResponse::getPos))
                .toList();

        StringBuilder corrected = new StringBuilder(text);
        int offset = 0;

        for (SpellerResponse error : sortedErrors) {
            if (error.getS() != null && !error.getS().isEmpty()) {
                String bestCorrection = chooseBestCorrection(error, corrected.toString());

                int startPos = error.getPos() + offset;
                int endPos = startPos + error.getLen();

                if (startPos >= 0 && endPos <= corrected.length()) {
                    corrected.replace(startPos, endPos, bestCorrection);

                    offset += bestCorrection.length() - error.getLen();
                }
            }
        }

        return corrected.toString();
    }

    private String chooseBestCorrection(SpellerResponse error, String context) {
        return error.getS().stream()
                .min(Comparator.comparingInt(suggestion ->
                        TextDiff.contextualDistance(suggestion, context, error.getPos())))
                .orElse(error.getWord());
    }

    private CorrectedText assembleResult(List<CorrectedSegment> segments) {
        if (segments == null || segments.isEmpty()) {
            throw new RuntimeException();
        }

        StringBuilder fullText = new StringBuilder();
        List<Correction> allCorrections = new ArrayList<>();
        int offset = 0;

        for (CorrectedSegment segment : segments) {
            if (segment != null && segment.getText() != null) {
                fullText.append(segment.getText());

                if (segment.getCorrections() != null) {
                    int finalOffset = offset;
                    segment.getCorrections().forEach(correction ->
                            allCorrections.add(correction.withAdjustedPosition(finalOffset))
                    );
                }

                offset += segment.getText().length();
            }
        }

        return new CorrectedText(fullText.toString(), allCorrections);
    }
}
