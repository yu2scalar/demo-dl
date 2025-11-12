package com.example.demo_dl.controller;

import com.example.demo_dl.dto.ApiResponse;
import com.example.demo_dl.dto.BenchmarkRequest;
import com.example.demo_dl.dto.BenchmarkResponse;
import com.example.demo_dl.service.BenchmarkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/benchmark")
@RequiredArgsConstructor
public class BenchmarkController {

    private final BenchmarkService benchmarkService;

    /**
     * Run enhanced multi-threaded benchmark test
     * POST /api/benchmark/run
     *
     * Tests contract performance with configurable:
     * - Thread counts (concurrency levels) - supports multiple thread configurations
     * - Asset pool sizes (contention levels)
     * - Duration-based testing (not request count)
     * - Latency percentiles (p50, p95, p99)
     * - CSV export with detailed metrics
     *
     * Example request:
     * {
     *   "contractId": "UserUpdaterV1_0_0",
     *   "baseArgument": "{\"userName\":\"user_{assetId}\",\"userAddress\":\"addr\",\"email\":\"test@example.com\",\"phoneNo\":\"123\"}",
     *   "threads": [16, 32, 64],
     *   "assets": [1, 2, 4, 8, 16, 32, 64],
     *   "durationSeconds": 60,
     *   "rampUpSeconds": 10,
     *   "exportCsv": true,
     *   "csvOutputDirectory": "./benchmark-results"
     * }
     *
     * @param request benchmark configuration
     * @return list of benchmark results (one per thread/asset combination)
     */
    @PostMapping("/run")
    public ApiResponse<List<BenchmarkResponse>> runBenchmark(@RequestBody BenchmarkRequest request) {
        try {
            log.info("Starting enhanced benchmark: contractId={}, threads={}, assets={}, duration={}s",
                    request.getContractId(),
                    request.getThreads(),
                    request.getAssets(),
                    request.getDurationSeconds());

            // Execute benchmark suite
            List<BenchmarkResponse> results = benchmarkService.executeBenchmark(request);

            log.info("Benchmark suite completed: {} tests executed", results.size());

            return ApiResponse.success(
                    String.format("Benchmark completed: %d tests executed", results.size()),
                    results
            );

        } catch (Exception e) {
            log.error("Benchmark failed", e);
            return ApiResponse.error("Benchmark failed: " + e.getMessage());
        }
    }
}
