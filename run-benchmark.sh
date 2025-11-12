#!/bin/bash

################################################################################
# ScalarDL Benchmark Runner Script
#
# Usage:
#   ./run-benchmark.sh [OPTIONS]
#
# Options:
#   --contract CONTRACT_ID     Contract to benchmark (default: UserUpdaterV1_0_0)
#   --threads NUMS             Thread counts to test (default: "32")
#   --assets NUMS              Asset pool sizes (default: "1,2,4,8,16,32,64")
#   --duration SEC             Test duration in seconds (default: 60)
#   --rampup SEC               Ramp-up period in seconds (default: 10)
#   --output DIR               Output directory (default: ./benchmark-results)
#   --help                     Show this help message
#
# Examples:
#   # Run with defaults
#   ./run-benchmark.sh
#
#   # Custom configuration
#   ./run-benchmark.sh --threads "16,32,64" --duration 120 --assets "1,4,16,64"
#
#   # Test specific contract with multiple thread counts
#   ./run-benchmark.sh --contract MyContractV1_0_0 --threads "16,32,64"
#
################################################################################

# Default values
CONTRACT_ID="UserUpdaterV1_0_0"
THREADS="32"
ASSETS="1,2,4,8,16,32,64"
DURATION=60
RAMPUP=10
OUTPUT_DIR="./benchmark-results"
BASE_URL="http://localhost:8080"

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --contract)
            CONTRACT_ID="$2"
            shift 2
            ;;
        --threads)
            THREADS="$2"
            shift 2
            ;;
        --assets)
            ASSETS="$2"
            shift 2
            ;;
        --duration)
            DURATION="$2"
            shift 2
            ;;
        --rampup)
            RAMPUP="$2"
            shift 2
            ;;
        --output)
            OUTPUT_DIR="$2"
            shift 2
            ;;
        --help)
            echo "Usage: $0 [OPTIONS]"
            echo ""
            echo "Options:"
            echo "  --contract CONTRACT_ID     Contract to benchmark (default: UserUpdaterV1_0_0)"
            echo "  --threads NUMS             Thread counts to test (default: \"32\")"
            echo "  --assets NUMS              Asset pool sizes (default: \"1,2,4,8,16,32,64\")"
            echo "  --duration SEC             Test duration in seconds (default: 60)"
            echo "  --rampup SEC               Ramp-up period in seconds (default: 10)"
            echo "  --output DIR               Output directory (default: ./benchmark-results)"
            echo "  --help                     Show this help message"
            echo ""
            echo "Examples:"
            echo "  ./run-benchmark.sh"
            echo "  ./run-benchmark.sh --threads \"16,32,64\" --duration 120"
            echo "  ./run-benchmark.sh --contract MyContractV1_0_0 --threads \"32,64\" --assets \"1,4,16\""
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            echo "Use --help for usage information"
            exit 1
            ;;
    esac
done

# Convert comma-separated values to JSON arrays
# Example: "1,2,4" -> [1,2,4]
THREADS_ARRAY="[${THREADS}]"
ASSETS_ARRAY="[${ASSETS}]"

# Prepare base argument based on contract
# You can customize this based on your contract requirements
BASE_ARGUMENT='{\"userName\":\"user_{assetId}\",\"userAddress\":\"123 Main St\",\"email\":\"test@example.com\",\"phoneNo\":\"+1-555-0123\"}'

# Create output directory if it doesn't exist
mkdir -p "$OUTPUT_DIR"

# Display configuration
echo "=================================="
echo "ScalarDL Benchmark Configuration"
echo "=================================="
echo "Contract ID:     $CONTRACT_ID"
echo "Threads:         $THREADS"
echo "Assets:          $ASSETS"
echo "Duration:        ${DURATION}s"
echo "Ramp-up:         ${RAMPUP}s"
echo "Output Dir:      $OUTPUT_DIR"
echo "=================================="
echo ""

# Build JSON request
REQUEST_JSON=$(cat <<EOF
{
  "contractId": "$CONTRACT_ID",
  "baseArgument": "$BASE_ARGUMENT",
  "threads": $THREADS_ARRAY,
  "assets": $ASSETS_ARRAY,
  "durationSeconds": $DURATION,
  "rampUpSeconds": $RAMPUP,
  "exportCsv": true,
  "csvOutputDirectory": "$OUTPUT_DIR"
}
EOF
)

# Display request
echo "Request JSON:"
echo "$REQUEST_JSON"
echo ""

# Execute benchmark
echo "Starting benchmark..."
echo "Endpoint: ${BASE_URL}/api/benchmark/run"
echo ""

RESPONSE=$(curl -s -X POST "${BASE_URL}/api/benchmark/run" \
    -H "Content-Type: application/json" \
    -H "Accept: application/json" \
    -d "$REQUEST_JSON")

# Check if curl succeeded
if [ $? -ne 0 ]; then
    echo "ERROR: Failed to connect to server at ${BASE_URL}"
    echo "Please ensure the application is running."
    exit 1
fi

# Display response
echo "Response:"
echo "$RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$RESPONSE"
echo ""

# Extract CSV filename if available
CSV_FILE=$(echo "$RESPONSE" | grep -o '"csvFileName":"[^"]*"' | head -1 | sed 's/"csvFileName":"\(.*\)"/\1/')

if [ -n "$CSV_FILE" ]; then
    echo "Results exported to: ${OUTPUT_DIR}/${CSV_FILE}"
    echo ""
    echo "Summary:"
    echo "--------"

    # Display summary from first and last test
    FIRST_TEST=$(echo "$RESPONSE" | grep -o '"throughputPerMinute":[0-9.]*' | head -1 | sed 's/"throughputPerMinute"://')
    LAST_TEST=$(echo "$RESPONSE" | grep -o '"throughputPerMinute":[0-9.]*' | tail -1 | sed 's/"throughputPerMinute"://')

    if [ -n "$FIRST_TEST" ] && [ -n "$LAST_TEST" ]; then
        echo "First test (assets=${ASSETS%%,*}): ${FIRST_TEST} tx/min"
        LAST_ASSET="${ASSETS##*,}"
        echo "Last test (assets=${LAST_ASSET}): ${LAST_TEST} tx/min"
    fi
else
    echo "Warning: No CSV file generated"
fi

echo ""
echo "Benchmark completed!"
