package com.example.demo_dl.model;

import com.example.demo_dl.dto.BenchmarkResponse;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Thread-safe statistics collector for benchmark tests
 * Tracks transaction counts, latencies, and errors in real-time
 */
@Getter
public class BenchmarkStatistics {

    private final String contractId;
    private final Integer threads;
    private final Integer assets;

    // Core metrics - thread-safe counters
    private final AtomicLong totalTransactions = new AtomicLong(0);
    private final AtomicLong successfulTransactions = new AtomicLong(0);
    private final AtomicLong failedTransactions = new AtomicLong(0);

    // Latency tracking (in nanoseconds) - thread-safe list
    private final List<Long> latencies = new CopyOnWriteArrayList<>();

    // Error tracking - thread-safe map
    private final ConcurrentHashMap<String, AtomicLong> errorCounts = new ConcurrentHashMap<>();

    // Time tracking
    private long startTime;
    private long endTime;

    public BenchmarkStatistics(String contractId, Integer threads, Integer assets) {
        this.contractId = contractId;
        this.threads = threads;
        this.assets = assets;
    }

    /**
     * Mark test start time
     */
    public void startTest() {
        this.startTime = System.currentTimeMillis();
    }

    /**
     * Mark test end time
     */
    public void endTest() {
        this.endTime = System.currentTimeMillis();
    }

    /**
     * Record a successful transaction
     *
     * @param latencyNanos latency in nanoseconds
     */
    public void recordSuccess(long latencyNanos) {
        totalTransactions.incrementAndGet();
        successfulTransactions.incrementAndGet();
        latencies.add(latencyNanos);
    }

    /**
     * Record a failed transaction
     *
     * @param errorType the exception class name or error type
     */
    public void recordFailure(String errorType) {
        totalTransactions.incrementAndGet();
        failedTransactions.incrementAndGet();
        errorCounts.computeIfAbsent(errorType, k -> new AtomicLong()).incrementAndGet();
    }

    /**
     * Calculate final results and return BenchmarkResponse
     */
    public BenchmarkResponse calculateResults() {
        long durationMs = endTime - startTime;
        long durationSeconds = durationMs / 1000;

        // Convert error counts to Map<String, Long>
        Map<String, Long> errorTypes = errorCounts.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().get()
                ));

        // Calculate latency statistics
        LatencyStats latencyStats = calculateLatencyStats();

        // Calculate throughput (transactions per minute)
        double throughputPerMinute = durationSeconds > 0
            ? (successfulTransactions.get() * 60.0) / durationSeconds
            : 0.0;

        return BenchmarkResponse.builder()
                .contractId(contractId)
                .threads(threads)
                .assets(assets)
                .startTimeMs(startTime)
                .endTimeMs(endTime)
                .durationMs(durationMs)
                .durationSeconds(durationSeconds)
                .totalTransactions(totalTransactions.get())
                .successfulTransactions(successfulTransactions.get())
                .failedTransactions(failedTransactions.get())
                .throughputPerMinute(throughputPerMinute)
                .avgLatencyMs(latencyStats.avg)
                .p50LatencyMs(latencyStats.p50)
                .p95LatencyMs(latencyStats.p95)
                .p99LatencyMs(latencyStats.p99)
                .maxLatencyMs(latencyStats.max)
                .minLatencyMs(latencyStats.min)
                .errorTypes(errorTypes)
                .testTimestamp(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")))
                .build();
    }

    /**
     * Calculate latency percentiles and statistics
     */
    private LatencyStats calculateLatencyStats() {
        if (latencies.isEmpty()) {
            return new LatencyStats(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        }

        // Sort latencies for percentile calculation
        List<Long> sortedLatencies = new ArrayList<>(latencies);
        Collections.sort(sortedLatencies);

        int size = sortedLatencies.size();

        // Convert nanoseconds to milliseconds
        double min = sortedLatencies.get(0) / 1_000_000.0;
        double max = sortedLatencies.get(size - 1) / 1_000_000.0;
        double avg = sortedLatencies.stream()
                .mapToDouble(l -> l / 1_000_000.0)
                .average()
                .orElse(0.0);

        // Calculate percentiles
        double p50 = getPercentile(sortedLatencies, 50);
        double p95 = getPercentile(sortedLatencies, 95);
        double p99 = getPercentile(sortedLatencies, 99);

        return new LatencyStats(avg, p50, p95, p99, max, min);
    }

    /**
     * Calculate percentile value from sorted list
     *
     * @param sortedLatencies sorted list of latencies in nanoseconds
     * @param percentile percentile to calculate (0-100)
     * @return percentile value in milliseconds
     */
    private double getPercentile(List<Long> sortedLatencies, int percentile) {
        if (sortedLatencies.isEmpty()) {
            return 0.0;
        }

        int size = sortedLatencies.size();
        int index = (int) Math.ceil((percentile / 100.0) * size) - 1;
        index = Math.max(0, Math.min(index, size - 1));

        return sortedLatencies.get(index) / 1_000_000.0;
    }

    /**
     * Inner class to hold latency statistics
     */
    private static class LatencyStats {
        final double avg;
        final double p50;
        final double p95;
        final double p99;
        final double max;
        final double min;

        LatencyStats(double avg, double p50, double p95, double p99, double max, double min) {
            this.avg = avg;
            this.p50 = p50;
            this.p95 = p95;
            this.p99 = p99;
            this.max = max;
            this.min = min;
        }
    }
}
