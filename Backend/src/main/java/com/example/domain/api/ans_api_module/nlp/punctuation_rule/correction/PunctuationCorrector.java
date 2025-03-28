package com.example.domain.api.ans_api_module.nlp.punctuation_rule.correction;

import com.example.domain.api.ans_api_module.nlp.punctuation_rule.correction_models.*;
import com.example.domain.api.ans_api_module.nlp.punctuation_rule.rule.CorrectionRule;
import com.example.domain.api.ans_api_module.nlp.punctuation_rule.rule.comma.*;
import com.example.domain.api.ans_api_module.nlp.punctuation_rule.rule.dot.AbbreviationDotCorrectionRule;
import com.example.domain.api.ans_api_module.nlp.punctuation_rule.rule.dot.SentenceEndingDotCorrectionRule;
import com.example.domain.api.ans_api_module.nlp.punctuation_rule.rule.question.ExclamationMarkCorrectionRule;
import com.example.domain.api.ans_api_module.nlp.punctuation_rule.rule.question.QuestionMarkCorrectionRule;
import io.micrometer.core.annotation.Timed;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
@ConditionalOnProperty(name = "correction.enabled", havingValue = "true")
public class PunctuationCorrector {
    private final CorrectionPipeline pipeline;
    private final TextAnalysis textAnalyzer;
    private final ExecutorService executor;
    private final CorrectionCache cache;

    @Autowired
    public PunctuationCorrector(TextAnalysis textAnalyzer,
                                CorrectionCache cache,
                                @Qualifier("correctionExecutor") ExecutorService executor,
                                @Value("#{'${correction.rules.active}'.split(', ')}") List<String> activeRules,
                                @Value("#{'${correction.rules.order}'.split(', ')}") List<String> ruleOrder) {
        this.textAnalyzer = textAnalyzer;
        this.cache = cache;
        this.executor = executor;
        this.pipeline = new CorrectionPipeline(createRules(activeRules), ruleOrder);
    }

    @Timed(value = "correction.time", description = "Time spent on text correction")
    public CorrectionResult correct(String text, String lang) {
        if (text == null || text.isBlank()) {
            return CorrectionResult.empty(text);
        }

        String cacheKey = createCacheKey(text, lang);
        CorrectionResult cached = cache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        try {
            List<TextSegment> segments = textAnalyzer.segment(text);
            List<CompletableFuture<SegmentCorrection>> futures = segments.stream()
                    .map(segment -> CompletableFuture.supplyAsync(
                            () -> processSegment(segment, lang), executor))
                    .toList();

            List<SegmentCorrection> results = futures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList());

            CorrectionResult result = assembleResult(text, results);
            cache.put(cacheKey, result);

            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private List<CorrectionRule> createRules(List<String> activeRules) {
        Map<String, CorrectionRule> allRules = Map.of(
                "commaBeforeConjunction", new CommaBeforeConjunctionRule(),
                "commaAfterAddress", new CommaAfterAddressRule(),
                "commaListCorrection", new CommaInListsCorrectionRule(),
                "fixCommaSpacingCorrection", new FixCommaSpacingCorrectionRule(),
                "removeExtraCorrection", new RemoveExtraCommasRule(),
                "abbreviationDotCorrection", new AbbreviationDotCorrectionRule(),
                "sentenceEndingDotCorrection", new SentenceEndingDotCorrectionRule(),
                "exclamationMarkCorrection", new ExclamationMarkCorrectionRule(),
                "questionMarkCorrection", new QuestionMarkCorrectionRule()
        );

        return activeRules.stream()
                .map(allRules::get)
                .filter(Objects::nonNull)
                .toList();
    }

    private SegmentCorrection processSegment(TextSegment segment, String lang) {
        TextAnalysis analysis = textAnalyzer.analyze(segment.getText(), lang);
        String corrected = pipeline.apply(segment.getText(), analysis);
        List<Correction> corrections = TextDiff.compare(segment.getText(), corrected, CorrectionType.PUNCTUATION)
                .stream()
                .map(correction -> correction.withAdjustedPosition(segment.getStartPos()))
                .toList();

        return new SegmentCorrection(segment, corrected, corrections);
    }

    private CorrectionResult assembleResult(String original, List<SegmentCorrection> segments) {
        StringBuilder textBuilder = new StringBuilder();
        List<Correction> allCorrections = new ArrayList<>();
        int offset = 0;

        for (SegmentCorrection segment : segments) {
            textBuilder.append(segment.correctedText());

            for (Correction correction : segment.corrections()) {
                allCorrections.add(correction.withAdjustedPosition(offset));
            }

            offset += segment.correctedText().length();
        }

        return new CorrectionResult(textBuilder.toString(), allCorrections);
    }

    private String createCacheKey(String text, String lang) {
        return STR."\{lang}|\{text.hashCode()}";
    }

    private <T> T getFuture(Future<T> future) {
        try {
            return future.get(500, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
