package com.example.demo_dl.service;

import com.example.demo_dl.dto.BenchmarkRequest;
import com.example.demo_dl.dto.BenchmarkResponse;
import com.example.demo_dl.model.BenchmarkStatistics;
import com.example.demo_dl.util.CsvExportUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Benchmark service for testing ScalarDL contract performance
 * Supports multi-threaded execution with configurable asset pool sizes
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BenchmarkService {

    private final ScalarDLService scalarDLService;

    /**
     * Execute full benchmark suite with multiple thread counts and asset pool sizes
     *
     * @param request Benchmark configuration
     * @return List of results for each thread/asset combination tested
     */
    public List<BenchmarkResponse> executeBenchmark(BenchmarkRequest request) throws Exception {
        List<BenchmarkResponse> allResults = new ArrayList<>();
        String suiteTimestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));

        log.info("Starting benchmark suite: contractId={}, thread patterns={}, asset patterns={}",
                request.getContractId(), request.getThreads(), request.getAssets());

        // Calculate total tests
        int totalTests = request.getThreads().size() * request.getAssets().size();
        int currentTest = 0;

        // Test each combination of thread count and asset pool size
        for (Integer threadCount : request.getThreads()) {
            for (Integer assetCount : request.getAssets()) {
                currentTest++;
                log.info("Testing with threads={}, assets={} ({}/{})",
                        threadCount, assetCount, currentTest, totalTests);

                // Run single test
                BenchmarkResponse result = runSingleTest(request, threadCount, assetCount);
                allResults.add(result);

                // Wait between tests (except after the last one)
                if (currentTest < totalTests) {
                    int delaySeconds = 5;
                    log.info("Waiting {} seconds before next test...", delaySeconds);
                    Thread.sleep(delaySeconds * 1000L);
                }
            }
        }

        // Export all results to CSV file(s)
        if (Boolean.TRUE.equals(request.getExportCsv()) && !allResults.isEmpty()) {
            // Group results by thread count for CSV export
            for (Integer threadCount : request.getThreads()) {
                List<BenchmarkResponse> threadResults = allResults.stream()
                        .filter(r -> r.getThreads().equals(threadCount))
                        .toList();

                if (!threadResults.isEmpty()) {
                    String csvFileName = CsvExportUtil.exportMultipleToCsv(
                            threadResults,
                            request.getCsvOutputDirectory(),
                            request.getContractId(),
                            threadCount,
                            suiteTimestamp
                    );
                    log.info("Exported results for {} threads to: {}", threadCount, csvFileName);

                    // Set CSV file name in results
                    for (BenchmarkResponse result : threadResults) {
                        result.setCsvFileName(csvFileName);
                    }
                }
            }
        }

        log.info("Benchmark suite completed. Total tests: {}", allResults.size());
        return allResults;
    }

    /**
     * Run a single benchmark test with specified thread count and asset pool size
     */
    private BenchmarkResponse runSingleTest(BenchmarkRequest request, int threadCount, int assetCount) throws InterruptedException {
        BenchmarkStatistics stats = new BenchmarkStatistics(
                request.getContractId(),
                threadCount,
                assetCount
        );

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        AtomicBoolean running = new AtomicBoolean(false);
        AtomicInteger assetIdCounter = new AtomicInteger(0);

        log.info("Starting test: contract={}, threads={}, assets={}, duration={}s",
                request.getContractId(), threadCount, assetCount, request.getDurationSeconds());

        try {
            // Ramp-up phase: gradually spawn threads
            long rampUpIntervalMs = request.getRampUpSeconds() > 0
                    ? (request.getRampUpSeconds() * 1000L) / threadCount
                    : 0;

            for (int i = 0; i < threadCount; i++) {
                final int threadIndex = i;

                executorService.submit(() -> {
                    try {
                        // Wait for test to start
                        while (!running.get()) {
                            Thread.sleep(10);
                        }

                        // Execute contracts continuously until test ends
                        while (running.get()) {
                            try {
                                long startTime = System.nanoTime();

                                // Select asset ID (round-robin through asset pool)
                                int assetId = assetIdCounter.getAndIncrement() % assetCount;

                                // Prepare contract argument with asset ID
                                String contractArgument = prepareContractArgument(
                                        request.getBaseArgument(),
                                        assetId
                                );

                                // Execute contract
                                scalarDLService.executeContract(
                                        request.getContractId(),
                                        contractArgument
                                );

                                // Record success
                                long latency = System.nanoTime() - startTime;
                                stats.recordSuccess(latency);

                            } catch (Exception e) {
                                // Record failure
                                String errorType = e.getClass().getSimpleName();
                                stats.recordFailure(errorType);
                            }
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });

                // Ramp-up delay between thread spawns
                if (i < threadCount - 1 && rampUpIntervalMs > 0) {
                    Thread.sleep(rampUpIntervalMs);
                }
            }

            // Wait for ramp-up to complete
            Thread.sleep(100);

            // Start test
            stats.startTest();
            running.set(true);
            log.info("Test started, running for {} seconds", request.getDurationSeconds());

            // Run for specified duration
            Thread.sleep(request.getDurationSeconds() * 1000L);

            // Stop test
            running.set(false);
            stats.endTest();
            log.info("Test completed");

        } finally {
            // Shutdown executor
            executorService.shutdown();
            executorService.awaitTermination(30, TimeUnit.SECONDS);
        }

        // Calculate and return results
        BenchmarkResponse result = stats.calculateResults();
        log.info("Results: total={}, success={}, failed={}, throughput={:.2f}/min, p95={:.2f}ms",
                result.getTotalTransactions(),
                result.getSuccessfulTransactions(),
                result.getFailedTransactions(),
                result.getThroughputPerMinute(),
                result.getP95LatencyMs());

        return result;
    }

    /**
     * Prepare contract argument by replacing {assetId} placeholder
     *
     * @param baseArgument Base argument template
     * @param assetId Asset ID to insert
     * @return Contract argument with asset ID
     */
    private String prepareContractArgument(String baseArgument, int assetId) {
        if (baseArgument == null) {
            return "{}";
        }
        return baseArgument.replace("{assetId}", String.valueOf(assetId));
    }
}
