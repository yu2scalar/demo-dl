package com.example.demo_dl.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BenchmarkResponse {

    // Test configuration
    private String contractId;
    private Integer threads;
    private Integer assets;

    // Transaction statistics
    private Long totalTransactions;
    private Long successfulTransactions;
    private Long failedTransactions;

    // Performance metrics
    private Double throughputPerMinute;
    private Double avgLatencyMs;
    private Double p50LatencyMs;
    private Double p95LatencyMs;
    private Double p99LatencyMs;
    private Double maxLatencyMs;
    private Double minLatencyMs;

    // Test duration
    private Long startTimeMs;
    private Long endTimeMs;
    private Long durationMs;
    private Long durationSeconds;

    // Error breakdown
    private Map<String, Long> errorTypes;

    // Metadata
    private String testTimestamp;
    private String csvFileName;

    // Deprecated fields for backward compatibility
    @Deprecated
    public Integer getTotalRequests() {
        return totalTransactions != null ? totalTransactions.intValue() : null;
    }

    @Deprecated
    public Long getTransactionsPerSecond() {
        return durationSeconds != null && durationSeconds > 0
            ? (successfulTransactions != null ? successfulTransactions / durationSeconds : 0L)
            : 0L;
    }

    @Deprecated
    public Integer getValidationCount() {
        return 0; // Not used in new implementation
    }
}
