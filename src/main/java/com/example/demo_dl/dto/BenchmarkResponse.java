package com.example.demo_dl.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BenchmarkResponse {
    private Long startTimeMs;
    private Long endTimeMs;
    private Long durationMs;
    private Integer totalRequests;
    private Long transactionsPerSecond;
    private Integer validationCount;
}
