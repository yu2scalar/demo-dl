package com.example.demo_dl.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BenchmarkRequest {

    // Contract to benchmark
    private String contractId;

    // Base argument template (use {assetId} placeholder for asset ID)
    private String baseArgument;

    // Thread configurations to test (supports multiple thread counts)
    @Builder.Default
    private List<Integer> threads = List.of(32);

    // Asset pool sizes to test (tests contention levels)
    @Builder.Default
    private List<Integer> assets = List.of(1, 2, 4, 8, 16, 32, 64);

    // Test duration
    @Builder.Default
    private Integer durationSeconds = 60;

    // Ramp-up period (gradual thread spawn time)
    @Builder.Default
    private Integer rampUpSeconds = 10;

    // CSV export
    @Builder.Default
    private Boolean exportCsv = true;

    @Builder.Default
    private String csvOutputDirectory = "./benchmark-results";

    // Deprecated fields for backward compatibility
    @Deprecated
    private Integer requestCount; // Use durationSeconds instead

    @Deprecated
    private Integer validationRatio; // Not used in new implementation
}
