package com.example.demo_dl.controller;

import com.example.demo_dl.dto.ApiResponse;
import com.example.demo_dl.dto.BenchmarkRequest;
import com.example.demo_dl.dto.BenchmarkResponse;
import com.example.demo_dl.service.ScalarDLService;
import com.scalar.dl.client.exception.ClientException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/api/benchmark")
@RequiredArgsConstructor
public class BenchmarkController {

    private final ScalarDLService scalarDLService;

    /**
     * Run benchmark test
     * POST /api/benchmark/run
     *
     * Based on CmdFileHash.java benchmark feature from hellodl sample
     *
     * @param request benchmark configuration
     * @return benchmark results including throughput metrics
     */
    @PostMapping("/run")
    public ApiResponse<BenchmarkResponse> runBenchmark(@RequestBody BenchmarkRequest request) {
        try {
            log.info("Starting benchmark: {} requests with validation ratio {}",
                    request.getRequestCount(), request.getValidationRatio());

            long startTime = System.currentTimeMillis();
            int validationCount = 0;

            // Execute multiple contract requests
            for (int i = 0; i < request.getRequestCount(); i++) {
                // Prepare argument with iteration number
                String contractArgument = prepareContractArgument(request.getBaseArgument(), i);

                // Execute contract
                scalarDLService.executeContract(request.getContractId(), contractArgument);

                // Validate based on ratio
                if (shouldValidate(request.getValidationRatio(), i)) {
                    String assetId = extractAssetIdFromArgument(contractArgument);
                    if (assetId != null) {
                        scalarDLService.validateLedger(assetId, null, null);
                        validationCount++;
                    }
                }

                // Log progress every 100 requests
                if ((i + 1) % 100 == 0) {
                    log.info("Progress: {}/{} requests completed", i + 1, request.getRequestCount());
                }
            }

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            BenchmarkResponse response = BenchmarkResponse.builder()
                    .startTimeMs(startTime)
                    .endTimeMs(endTime)
                    .durationMs(duration)
                    .totalRequests(request.getRequestCount())
                    .transactionsPerSecond(duration > 0 ? (request.getRequestCount() * 1000L) / duration : 0)
                    .validationCount(validationCount)
                    .build();

            log.info("Benchmark completed: {} tx/s over {} ms",
                    response.getTransactionsPerSecond(), response.getDurationMs());

            return ApiResponse.success("Benchmark completed successfully", response);

        } catch (ClientException | IOException e) {
            log.error("Benchmark failed", e);
            return ApiResponse.error("Benchmark failed: " + e.getMessage());
        }
    }

    /**
     * Prepare contract argument with iteration number
     * Replaces placeholders like {iteration} in the base argument
     */
    private String prepareContractArgument(String baseArgument, int iteration) {
        if (baseArgument == null) {
            return "{}";
        }
        return baseArgument.replace("{iteration}", String.valueOf(iteration));
    }

    /**
     * Determine if validation should be performed for this iteration
     *
     * @param validationRatio 0 = never, 1 = always, N = once per N requests
     * @param iteration current iteration number
     * @return true if validation should be performed
     */
    private boolean shouldValidate(Integer validationRatio, int iteration) {
        if (validationRatio == null || validationRatio == 0) {
            return false;
        }
        if (validationRatio == 1) {
            return true;
        }
        return iteration % validationRatio == 0;
    }

    /**
     * Extract asset ID from contract argument JSON
     * Attempts to find common field names: assetId, asset_id, fileId, file_id, id
     */
    private String extractAssetIdFromArgument(String contractArgument) {
        try {
            // Simple regex-based extraction (could use JSON parser for robustness)
            String[] patterns = {
                "\"assetId\"\\s*:\\s*\"([^\"]+)\"",
                "\"asset_id\"\\s*:\\s*\"([^\"]+)\"",
                "\"fileId\"\\s*:\\s*\"([^\"]+)\"",
                "\"file_id\"\\s*:\\s*\"([^\"]+)\"",
                "\"id\"\\s*:\\s*\"([^\"]+)\""
            };

            for (String pattern : patterns) {
                java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
                java.util.regex.Matcher m = p.matcher(contractArgument);
                if (m.find()) {
                    return m.group(1);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to extract asset ID from argument: {}", contractArgument, e);
        }
        return null;
    }
}
