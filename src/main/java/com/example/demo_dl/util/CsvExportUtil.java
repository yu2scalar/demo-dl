package com.example.demo_dl.util;

import com.example.demo_dl.dto.BenchmarkResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Utility class for exporting benchmark results to CSV files
 */
@Slf4j
public class CsvExportUtil {

    private static final String CSV_HEADER = "contractId,threads,assets,totalTransactions,successfulTransactions,"
            + "failedTransactions,throughputPerMinute,avgLatencyMs,p50LatencyMs,p95LatencyMs,p99LatencyMs,"
            + "maxLatencyMs,minLatencyMs,durationSeconds,errorTypes,testTimestamp\n";

    /**
     * Export multiple benchmark results to a single CSV file
     *
     * @param results List of benchmark results
     * @param outputDirectory Directory to save CSV file
     * @param contractId Contract ID being tested
     * @param threads Number of threads used
     * @param timestamp Test suite timestamp
     * @return CSV file name
     */
    public static String exportMultipleToCsv(
            List<BenchmarkResponse> results,
            String outputDirectory,
            String contractId,
            Integer threads,
            String timestamp) throws IOException {

        // Create output directory if it doesn't exist
        Files.createDirectories(Paths.get(outputDirectory));

        // Generate CSV file name: ContractId-Threads-Timestamp.csv
        String fileName = String.format("%s-%d-%s.csv",
                sanitizeFileName(contractId),
                threads,
                timestamp);
        String filePath = outputDirectory + "/" + fileName;

        try (FileWriter writer = new FileWriter(filePath)) {
            // Write header
            writer.write(CSV_HEADER);

            // Write each result as a row
            for (BenchmarkResponse result : results) {
                writer.write(toCSVRow(result));
            }
        }

        log.info("Exported {} results to CSV: {}", results.size(), filePath);
        return fileName;
    }

    /**
     * Export a single benchmark result to CSV
     *
     * @param result Benchmark result
     * @param outputDirectory Directory to save CSV file
     * @return CSV file name
     */
    public static String exportSingleToCsv(
            BenchmarkResponse result,
            String outputDirectory) throws IOException {

        // Create output directory if it doesn't exist
        Files.createDirectories(Paths.get(outputDirectory));

        // Generate CSV file name
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        String fileName = String.format("%s-%d-%d-%s.csv",
                sanitizeFileName(result.getContractId()),
                result.getThreads(),
                result.getAssets(),
                timestamp);
        String filePath = outputDirectory + "/" + fileName;

        try (FileWriter writer = new FileWriter(filePath)) {
            // Write header
            writer.write(CSV_HEADER);

            // Write result row
            writer.write(toCSVRow(result));
        }

        log.info("Exported benchmark result to CSV: {}", filePath);
        return fileName;
    }

    /**
     * Convert BenchmarkResponse to CSV row
     */
    private static String toCSVRow(BenchmarkResponse result) {
        return String.format("%s,%d,%d,%d,%d,%d,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%d,\"%s\",%s\n",
                escapeCsv(result.getContractId()),
                result.getThreads(),
                result.getAssets(),
                result.getTotalTransactions(),
                result.getSuccessfulTransactions(),
                result.getFailedTransactions(),
                result.getThroughputPerMinute(),
                result.getAvgLatencyMs(),
                result.getP50LatencyMs(),
                result.getP95LatencyMs(),
                result.getP99LatencyMs(),
                result.getMaxLatencyMs(),
                result.getMinLatencyMs(),
                result.getDurationSeconds(),
                formatErrorTypes(result.getErrorTypes()),
                result.getTestTimestamp()
        );
    }

    /**
     * Format error types map as a string for CSV
     * Example: "ClientException:150,TimeoutException:5"
     */
    private static String formatErrorTypes(Map<String, Long> errorTypes) {
        if (errorTypes == null || errorTypes.isEmpty()) {
            return "";
        }

        return errorTypes.entrySet().stream()
                .map(entry -> entry.getKey() + ":" + entry.getValue())
                .collect(Collectors.joining(","));
    }

    /**
     * Escape CSV special characters
     */
    private static String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        // Escape quotes by doubling them
        return value.replace("\"", "\"\"");
    }

    /**
     * Sanitize file name by removing invalid characters
     */
    private static String sanitizeFileName(String fileName) {
        if (fileName == null) {
            return "unknown";
        }
        // Replace invalid file name characters with underscore
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
