package com.example.domain.api.ans_api_module.nlp.metrics;

import com.example.domain.api.ans_api_module.nlp.punctuation_rule.correction_models.Correction;
import com.example.domain.api.ans_api_module.nlp.punctuation_rule.correction_models.CorrectionType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class StatisticsService {
    private final CorrectionMetrics metrics;
    private final Path statsDirectory;
    private final ObjectMapper objectMapper;
    private final ScheduledExecutorService scheduler;

    private final Map<CorrectionType, AtomicLong> correctionsCache = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> ruleUsageCache = new ConcurrentHashMap<>();
    private final AtomicLong segmentsProcessed = new AtomicLong();
    private final AtomicLong totalErrors = new AtomicLong();
    private final Map<String, AtomicLong> errorCounters = new ConcurrentHashMap<>();
    private final List<Long> processingTimes = new CopyOnWriteArrayList<>();

    public StatisticsService(CorrectionMetrics metrics,
                             @Value("${correction.stats.directory}") String statsDir) {
        this.metrics = metrics;
        this.statsDirectory = Paths.get(statsDir);
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        this.scheduler = Executors.newSingleThreadScheduledExecutor();

        initialize();
    }

    private void initialize() {
        try {
            Files.createDirectories(statsDirectory);
            Arrays.stream(CorrectionType.values())
                    .forEach(type -> correctionsCache.put(type, new AtomicLong(0)));

            scheduler.scheduleAtFixedRate(
                    this::flushStatistics,
                    1, 1, TimeUnit.MINUTES
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to initialize stats directory", e);
        }
    }

    @PreDestroy
    public void shutdown() {
        scheduler.shutdown();
        flushStatistics();
    }

    private synchronized void flushStatistics() {
        try {
            Map<CorrectionType, Long> corrections = correctionsCache.entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            e -> e.getValue().getAndSet(0)
                    ));

            if (!corrections.isEmpty()) {
                writeToFile("corrections", corrections);
            }

            Map<String, Long> ruleUsage = ruleUsageCache.entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            e -> e.getValue().getAndSet(0)
                    ));

            if (!ruleUsage.isEmpty()) {
                writeToFile("rule_usage", ruleUsage);
            }

            Map<String, Long> errorStats = errorCounters.entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            e -> e.getValue().getAndSet(0)
                    ));

            if (!errorStats.isEmpty()) {
                writeToFile("errors", errorStats);
            }

            if (!processingTimes.isEmpty()) {
                LongSummaryStatistics stats = processingTimes.stream()
                        .mapToLong(Long::longValue)
                        .summaryStatistics();

                Map<String, Long> timeStats = Map.of(
                        "count", stats.getCount(),
                        "sum", stats.getSum(),
                        "min", stats.getMin(),
                        "max", stats.getMax(),
                        "avg", (long) stats.getAverage()
                );

                writeToFile("processing_times", timeStats);
                processingTimes.clear();
            }

        } catch (IOException e) {
            System.err.println("Failed to save statistics: " + e.getMessage());
        }
    }

    private void writeToFile(String prefix, Object data) throws IOException {
        String timestamp = Instant.now().toString().replace(":", "-");
        Path filePath = statsDirectory.resolve(prefix + "_" + timestamp + ".json");

        try (OutputStream os = Files.newOutputStream(filePath,
                StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
            objectMapper.writeValue(os, data);
        }
    }

    public void recordCorrections(List<Correction> corrections) {
        corrections.forEach(correction -> {
            correctionsCache.get(correction.getType()).incrementAndGet();
            metrics.incrementCorrectionType(correction.getType());
        });
    }

    public void recordSegmentCorrections(List<Correction> corrections) {
        recordCorrections(corrections);
    }

    public void incrementSegmentProcessed() {
        segmentsProcessed.incrementAndGet();
        metrics.incrementSegmentProcessed();
    }

    public void incrementErrors(String errorType) {
        totalErrors.incrementAndGet();
        errorCounters.computeIfAbsent(errorType, k -> new AtomicLong()).incrementAndGet();
        metrics.incrementErrors(errorType);
    }

    public void recordProcessingTime(long processingTimeMs) {
        processingTimes.add(processingTimeMs);
        metrics.recordProcessingTime(processingTimeMs);
    }

    public void recordRuleUsage(String ruleName, long processingTimeMs) {
        ruleUsageCache.computeIfAbsent(ruleName, k -> new AtomicLong(0))
                .incrementAndGet();
        metrics.recordRuleUsage(ruleName, processingTimeMs);
    }

    public Map<CorrectionType, Long> getCurrentCorrectionStats() {
        return correctionsCache.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().get()
                ));
    }
}
