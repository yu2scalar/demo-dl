# ScalarDL Enhanced Benchmark Guide

## Overview

The enhanced benchmark system provides comprehensive performance testing for ScalarDL contracts with multi-threaded execution, asset pool management, and detailed latency metrics.

## Key Features

- **Multi-threaded execution**: Configurable concurrent threads (1-256) - **supports multiple thread counts per test**
- **Asset pool testing**: Test different contention levels by varying asset counts
- **Duration-based testing**: Run for specified time periods instead of request counts
- **Detailed metrics**: Latency percentiles (p50, p95, p99), throughput, error breakdown
- **CSV export**: Automatic export of results for analysis (one CSV per thread count)
- **Ramp-up support**: Gradual thread spawning to stabilize initial conditions
- **Comprehensive testing**: Run threads × assets combinations in a single request

## Key Concept: Assets vs Threads

The relationship between **threads** and **assets** determines contention:

```
threads=32, assets=4   → High contention (8 threads per asset)
threads=32, assets=32  → Medium contention (1:1 ratio)
threads=32, assets=64  → Low contention (2 assets per thread)
```

**Why it matters**: ScalarDL cannot execute contracts in parallel on the same asset ID. When multiple threads try to update the same asset, conflicts occur and some transactions fail.

## Usage

### Option 1: Shell Script (Recommended)

```bash
# Default configuration (threads=[32], assets=1,2,4,8,16,32,64)
./run-benchmark.sh

# Test with multiple thread counts
./run-benchmark.sh --threads "16,32,64" --duration 120 --assets "1,4,16,64"

# Test specific contract with multiple thread counts
./run-benchmark.sh --contract MyContractV1_0_0 --threads "8,16,32" --duration 30

# Show help
./run-benchmark.sh --help
```

**Script Options:**
- `--contract CONTRACT_ID`: Contract to benchmark (default: UserUpdaterV1_0_0)
- `--threads NUMS`: Comma-separated thread counts to test (default: "32")
- `--assets NUMS`: Comma-separated asset pool sizes (default: "1,2,4,8,16,32,64")
- `--duration SEC`: Test duration in seconds (default: 60)
- `--rampup SEC`: Ramp-up period in seconds (default: 10)
- `--output DIR`: Output directory (default: ./benchmark-results)

### Option 2: Direct API Call

```bash
curl -X POST http://localhost:8080/api/benchmark/run \
  -H 'Content-Type: application/json' \
  -d '{
  "contractId": "UserUpdaterV1_0_0",
  "baseArgument": "{\"userName\":\"user_{assetId}\",\"userAddress\":\"123 Main St\",\"email\":\"test@example.com\",\"phoneNo\":\"+1-555-0123\"}",
  "threads": [16, 32, 64],
  "assets": [1, 2, 4, 8, 16, 32, 64],
  "durationSeconds": 60,
  "rampUpSeconds": 10,
  "exportCsv": true,
  "csvOutputDirectory": "./benchmark-results"
}'
```

## Request Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| **contractId** | String | Required | Contract to benchmark |
| **baseArgument** | String | Required | JSON template (use `{assetId}` placeholder) |
| **threads** | List<Integer> | [32] | Thread counts to test (supports multiple) |
| **assets** | List<Integer> | [1,2,4,8,16,32,64] | Asset pool sizes to test |
| **durationSeconds** | Integer | 60 | Test duration per thread/asset combination |
| **rampUpSeconds** | Integer | 10 | Gradual thread spawn period |
| **exportCsv** | Boolean | true | Export results to CSV |
| **csvOutputDirectory** | String | ./benchmark-results | CSV output directory |

## Response Format

```json
{
  "success": true,
  "message": "Benchmark completed: 7 tests executed",
  "data": [
    {
      "contractId": "UserUpdaterV1_0_0",
      "threads": 32,
      "assets": 1,
      "totalTransactions": 1523,
      "successfulTransactions": 450,
      "failedTransactions": 1073,
      "throughputPerMinute": 450.5,
      "avgLatencyMs": 85.3,
      "p50LatencyMs": 72.1,
      "p95LatencyMs": 145.2,
      "p99LatencyMs": 203.4,
      "maxLatencyMs": 312.5,
      "minLatencyMs": 25.1,
      "durationSeconds": 60,
      "errorTypes": {"ClientException": 1073},
      "testTimestamp": "20251110-143022",
      "csvFileName": "UserUpdaterV1_0_0-32-20251110-143022.csv"
    },
    ...
  ]
}
```

## Metrics Explained

### Transaction Metrics
- **totalTransactions**: Total attempted transactions
- **successfulTransactions**: Successfully committed transactions
- **failedTransactions**: Failed transactions (usually due to conflicts)

### Performance Metrics
- **throughputPerMinute**: Successful transactions per minute
- **avgLatencyMs**: Average transaction latency
- **p50LatencyMs**: Median latency (50th percentile)
- **p95LatencyMs**: 95th percentile latency
- **p99LatencyMs**: 99th percentile latency
- **maxLatencyMs**: Maximum observed latency
- **minLatencyMs**: Minimum observed latency

### Error Breakdown
- **errorTypes**: Map of exception types to occurrence counts

## CSV Output

Results are automatically exported to CSV:

**File naming**: `{ContractId}-{Threads}-{Timestamp}.csv`

**Example**: `UserUpdaterV1_0_0-32-20251110-143022.csv`

**CSV Columns**:
```
contractId,threads,assets,totalTransactions,successfulTransactions,
failedTransactions,throughputPerMinute,avgLatencyMs,p50LatencyMs,
p95LatencyMs,p99LatencyMs,maxLatencyMs,minLatencyMs,durationSeconds,
errorTypes,testTimestamp
```

## Example Workflows

### 1. Quick Performance Check
```bash
# Test with single thread count for 30 seconds
./run-benchmark.sh --threads "16" --duration 30 --assets "1,8,32"
```

### 2. Comprehensive Contention Analysis
```bash
# Test multiple thread counts to see scaling behavior
./run-benchmark.sh --threads "8,16,32,64" --assets "1,2,4,8,16,32,64,128,256"
```

### 3. High-Load Stress Test
```bash
# Test high thread counts (produces 3 CSV files, one per thread count)
./run-benchmark.sh --threads "64,128,256" --duration 300 --assets "64,128,256"
```

### 4. Low-Contention Baseline
```bash
# Test scaling from low to high threads with many assets
./run-benchmark.sh --threads "8,16,32,64" --assets "256,512,1024"
```

### 5. Thread Scaling Analysis
```bash
# Test how throughput scales with increasing threads
./run-benchmark.sh --threads "1,2,4,8,16,32,64,128" --assets "64" --duration 30
```

## Understanding Results

### High Contention (assets << threads)
```
threads=32, assets=1
- High failure rate (70-90%)
- Lower throughput
- More variable latency
```

### Medium Contention (assets ≈ threads)
```
threads=32, assets=32
- Moderate failure rate (10-30%)
- Good throughput
- Stable latency
```

### Low Contention (assets >> threads)
```
threads=32, assets=64+
- Low failure rate (0-5%)
- Maximum throughput
- Consistent latency
```

## Troubleshooting

### Application Not Running
```
ERROR: Failed to connect to server at http://localhost:8080
```
**Solution**: Start the application first:
```bash
./gradlew bootRun
```

### Contract Not Found
```
Failed to execute contract: DL-COMMON-404001: The specified contract is not found.
```
**Solution**: Register the contract first:
```bash
curl -X POST 'http://localhost:8080/api/contracts/register-compiled' \
  -d 'versionedClassName=UserUpdaterV1_0_0&packageName=com.example.contracts'
```

### High Failure Rate
If you see >90% failure rate even with high asset counts:
1. Check contract is properly registered
2. Verify baseArgument template is valid JSON
3. Ensure ScalarDL server is running and accessible

## Advanced Configuration

### Custom Base Argument Template

The `baseArgument` field supports `{assetId}` placeholder:

```json
{
  "baseArgument": "{\"userName\":\"user_{assetId}\",\"orderId\":\"order_{assetId}\",\"status\":\"active\"}"
}
```

This generates:
- Asset 0: `{"userName":"user_0","orderId":"order_0","status":"active"}`
- Asset 1: `{"userName":"user_1","orderId":"order_1","status":"active"}`
- etc.

### Analyzing CSV Results

Load the CSV in your preferred tool:

**Python/Pandas**:
```python
import pandas as pd
df = pd.read_csv('benchmark-results/UserUpdaterV1_0_0-32-20251110-143022.csv')

# Plot throughput vs assets
df.plot(x='assets', y='throughputPerMinute', kind='line')

# Compare p95 latency
df.plot(x='assets', y='p95LatencyMs', kind='bar')
```

**Excel/Google Sheets**: Import CSV and create charts

## Best Practices

1. **Start small**: Begin with short duration (30s) and few assets
2. **Warm-up**: First run may be slower due to JVM warm-up
3. **Ramp-up**: Use 10+ second ramp-up for stable results
4. **Multiple runs**: Run 3+ times and average results
5. **Monitor system**: Check CPU/memory during high-load tests
6. **Baseline**: Always test with assets >> threads for max throughput baseline

## Next Steps

1. Review the CSV results in `./benchmark-results/`
2. Identify optimal thread/asset ratio for your use case
3. Test your own contracts by changing `--contract` parameter
4. Analyze error patterns in `errorTypes` field
5. Compare different contract versions

---

For questions or issues, check the application logs or review the source code in:
- `src/main/java/com/example/demo_dl/service/BenchmarkService.java`
- `src/main/java/com/example/demo_dl/controller/BenchmarkController.java`
