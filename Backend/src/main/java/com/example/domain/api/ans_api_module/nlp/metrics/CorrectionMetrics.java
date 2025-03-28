package com.example.domain.api.ans_api_module.nlp.metrics;

import com.example.domain.api.ans_api_module.nlp.punctuation_rule.correction_models.Correction;
import com.example.domain.api.ans_api_module.nlp.punctuation_rule.correction_models.CorrectionType;
import io.jsonwebtoken.io.IOException;
import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Component
public class CorrectionMetrics {
    private static final String METRICS_PREFIX = "correction.";

    private final MeterRegistry meterRegistry;
    private final Path metricsLogFile;
    private final ScheduledExecutorService metricsWriter = Executors.newSingleThreadScheduledExecutor();

    private final AtomicLong totalRequests = new AtomicLong();
    private final AtomicLong totalCorrections = new AtomicLong();
    private final AtomicLong segmentsProcessed = new AtomicLong();
    private final AtomicLong totalErrors = new AtomicLong();

    private final EnumMap<CorrectionType, AtomicLong> correctionCounters = new EnumMap<>(CorrectionType.class);
    private final ConcurrentHashMap<String, AtomicLong> errorCounters = new ConcurrentHashMap<>();

    private DistributionSummary processingTimeSummary;

    public CorrectionMetrics(
            @Value("${correction.stats.metrics.file:correction_metrics.log}") String logPath,
            @Autowired(required = false) MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.metricsLogFile = Paths.get(logPath);
        initializeCounters();
    }

    @PostConstruct
    private void init() {
        initializeMetrics();
        startMetricsWriter();
        logSystemStatus();
    }

    private void initializeCounters() {
        Arrays.stream(CorrectionType.values())
                .forEach(type -> correctionCounters.put(type, new AtomicLong()));
    }

    private void initializeMetrics() {
        if (meterRegistry != null) {
            this.processingTimeSummary = createDistributionSummary("processing.time", "milliseconds");
            registerBasicGauges();
            registerTypeSpecificGauges();
        }
    }

    private DistributionSummary createDistributionSummary(String name, String unit) {
        return DistributionSummary.builder(METRICS_PREFIX + name)
                .baseUnit(unit)
                .register(meterRegistry);
    }

    private void registerBasicGauges() {
        registerGauge("requests.total", totalRequests);
        registerGauge("total.count", totalCorrections);
        registerGauge("segments.processed", segmentsProcessed);
        registerGauge("errors.total", totalErrors);
    }

    private void registerTypeSpecificGauges() {
        correctionCounters.forEach((type, counter) ->
                registerGauge("type." + type.name().toLowerCase(), counter)
        );
    }

    private void registerGauge(String name, AtomicLong counter) {
        Gauge.builder(METRICS_PREFIX + name, counter::get)
                .register(meterRegistry);
    }

    private void startMetricsWriter() {
        try {
            Files.createDirectories(metricsLogFile.getParent());
            metricsWriter.scheduleAtFixedRate(this::logMetricsToFile, 1, 1, TimeUnit.MINUTES);
        } catch (IOException | java.io.IOException e) {
            throw new IllegalStateException("Failed to initialize metrics logging", e);
        }
    }

    private void logSystemStatus() {
        System.out.println(meterRegistry != null
                ? "Metrics system initialized with MeterRegistry"
                : "Running in metrics logging mode only - no MeterRegistry available");
    }

    @PreDestroy
    public void shutdown() {
        gracefullyShutdownExecutor();
        logMetricsToFile();
    }

    private void gracefullyShutdownExecutor() {
        metricsWriter.shutdown();
        try {
            if (!metricsWriter.awaitTermination(1, TimeUnit.SECONDS)) {
                metricsWriter.shutdownNow();
            }
        } catch (InterruptedException e) {
            metricsWriter.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private synchronized void logMetricsToFile() {
        try {
            String logEntry = createMetricsLogEntry();
            Files.writeString(metricsLogFile, logEntry,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException | java.io.IOException e) {
            System.err.println("Metrics logging failed: " + e.getMessage());
        }
    }

    private String createMetricsLogEntry() {
        return String.format("[%s] Metrics Snapshot:%n" +
                        "Requests: %d | Corrections: %d | Segments: %d | Errors: %d%n" +
                        "Correction Types: %s%n" +
                        "Error Types: %s%n%n",
                Instant.now(),
                totalRequests.get(),
                totalCorrections.get(),
                segmentsProcessed.get(),
                totalErrors.get(),
                formatCounterMap(correctionCounters),
                formatCounterMap(errorCounters));
    }

    private String formatCounterMap(Map<?, AtomicLong> counters) {
        return counters.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue().get())
                .collect(Collectors.joining(", "));
    }

    public void incrementRequests() {
        totalRequests.incrementAndGet();
    }

    public void incrementCorrectionType(CorrectionType type) {
        correctionCounters.get(type).incrementAndGet();
        totalCorrections.incrementAndGet();
    }

    public void recordSegmentCorrections(List<Correction> corrections) {
        Optional.ofNullable(corrections).ifPresent(list ->
                list.forEach(c -> incrementCorrectionType(c.getType()))
        );
    }

    public void incrementSegmentProcessed() {
        segmentsProcessed.incrementAndGet();
    }

    public void incrementErrors(String errorType) {
        totalErrors.incrementAndGet();
        errorCounters.computeIfAbsent(errorType, k -> new AtomicLong()).incrementAndGet();
    }

    public void recordProcessingTime(long processingTimeMs) {
        if (processingTimeSummary != null) {
            processingTimeSummary.record(processingTimeMs);
        }
    }

    public void recordRuleUsage(String ruleName, long processingTimeMs) {
        if (meterRegistry != null) {
            Timer.builder(METRICS_PREFIX + "rule.time")
                    .tag("rule", ruleName)
                    .register(meterRegistry)
                    .record(processingTimeMs, TimeUnit.MILLISECONDS);
        }
    }

    public long getTotalRequests() {
        return totalRequests.get();
    }

    public long getTotalCorrections() {
        return totalCorrections.get();
    }

    public long getSegmentsProcessed() {
        return segmentsProcessed.get();
    }

    public long getTotalErrors() {
        return totalErrors.get();
    }
}
