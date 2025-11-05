# ScalarDL REST API Documentation

This Spring Boot application provides REST API endpoints for interacting with ScalarDL, based on the CmdFileHash.java sample from the hellodl project.

## Configuration

Configure ScalarDL connection settings in `src/main/resources/application.properties`:

```properties
# Server settings
scalardl.server-host=localhost
scalardl.server-port=50051

# Certificate settings
scalardl.cert-holder-id=client
scalardl.cert-path=./cert/client.pem
scalardl.private-key-path=./cert/client-key.pem

# Auditor settings (optional)
scalardl.auditor.enabled=true
scalardl.auditor.host=localhost
scalardl.auditor.port=40051
```

## API Endpoints

### 1. Certificate Management

#### Register Certificate
```bash
POST /api/certificates/register

# Example using curl:
curl -X POST http://localhost:8080/api/certificates/register
```

**Response:**
```json
{
  "success": true,
  "message": "Certificate registered successfully",
  "data": "OK"
}
```

---

### 2. Contract Operations

#### Register Contract
Upload and register a contract .class file.

```bash
POST /api/contracts/register
Content-Type: multipart/form-data

# Example using curl:
curl -X POST http://localhost:8080/api/contracts/register \
  -F "file=@FileHashUpdaterV0910.class" \
  -F 'request={"contractId":"FileHashUpdaterV0910","contractBinaryName":"net.readeng.scalar.dl.hellodl.FileHashUpdaterV0910","contractProperties":null};type=application/json'
```

**Request Parameters:**
- `file` (multipart): Contract .class file
- `request` (JSON):
  - `contractId`: Unique identifier for the contract
  - `contractBinaryName`: Fully qualified class name
  - `contractProperties`: Optional JSON properties

**Response:**
```json
{
  "success": true,
  "message": "Contract registered successfully: FileHashUpdaterV0910",
  "data": "OK"
}
```

#### Execute Contract
```bash
POST /api/contracts/execute
Content-Type: application/json

# Example using curl:
curl -X POST http://localhost:8080/api/contracts/execute \
  -H "Content-Type: application/json" \
  -d '{
    "contractId": "FileHashUpdaterV0910",
    "contractArgument": "{\"fileId\":\"test-file-001\",\"event\":\"Create\",\"hash\":\"abc123\"}"
  }'
```

**Request Body:**
```json
{
  "contractId": "FileHashUpdaterV0910",
  "contractArgument": "{\"fileId\":\"test-file-001\",\"event\":\"Create\",\"hash\":\"abc123\"}"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Contract executed successfully",
  "data": {
    "contractResult": {...},
    "ledgerProofs": [...],
    "auditorProofs": [...]
  }
}
```

#### Execute Contract with Function
```bash
POST /api/contracts/execute-with-function
Content-Type: application/json

# Example:
curl -X POST http://localhost:8080/api/contracts/execute-with-function \
  -H "Content-Type: application/json" \
  -d '{
    "contractId": "FileHashUpdaterV0910",
    "contractArgument": "{\"fileId\":\"test-file-001\",\"event\":\"Update\",\"hash\":\"def456\"}",
    "functionId": "ToScalarDBV0906",
    "functionArgument": "{}"
  }'
```

**Request Body:**
```json
{
  "contractId": "FileHashUpdaterV0910",
  "contractArgument": "{\"fileId\":\"test-file-001\",\"event\":\"Update\",\"hash\":\"def456\"}",
  "functionId": "ToScalarDBV0906",
  "functionArgument": "{}"
}
```

#### Validate Ledger
```bash
POST /api/contracts/validate
Content-Type: application/json

# Example:
curl -X POST http://localhost:8080/api/contracts/validate \
  -H "Content-Type: application/json" \
  -d '{
    "assetId": "test-file-001",
    "startAge": null,
    "endAge": null
  }'
```

**Request Body:**
```json
{
  "assetId": "test-file-001",
  "startAge": null,
  "endAge": null
}
```

**Response:**
```json
{
  "success": true,
  "message": "Ledger validated successfully",
  "data": {
    "statusCode": "OK",
    "ledgerProof": {...},
    "auditorProof": {...}
  }
}
```

---

### 3. Function Operations

#### Register Function
Upload and register a function .class file.

```bash
POST /api/functions/register
Content-Type: multipart/form-data

# Example:
curl -X POST http://localhost:8080/api/functions/register \
  -F "file=@ToScalarDBV0906.class" \
  -F 'request={"functionId":"ToScalarDBV0906","functionBinaryName":"net.readeng.scalar.dl.hellodl.ToScalarDBV0906"};type=application/json'
```

**Request Parameters:**
- `file` (multipart): Function .class file
- `request` (JSON):
  - `functionId`: Unique identifier for the function
  - `functionBinaryName`: Fully qualified class name

**Response:**
```json
{
  "success": true,
  "message": "Function registered successfully: ToScalarDBV0906",
  "data": "OK"
}
```

---

### 4. Benchmark Operations

#### Run Benchmark
Execute performance testing with configurable parameters (based on CmdFileHash.java benchmark feature).

```bash
POST /api/benchmark/run
Content-Type: application/json

# Example:
curl -X POST http://localhost:8080/api/benchmark/run \
  -H "Content-Type: application/json" \
  -d '{
    "contractId": "FileHashUpdaterV0910",
    "baseArgument": "{\"fileId\":\"bench-file\",\"event\":\"Bench{iteration}\",\"hash\":\"hash{iteration}\"}",
    "requestCount": 1000,
    "validationRatio": 10
  }'
```

**Request Body:**
```json
{
  "contractId": "FileHashUpdaterV0910",
  "baseArgument": "{\"fileId\":\"bench-file\",\"event\":\"Bench{iteration}\",\"hash\":\"hash{iteration}\"}",
  "requestCount": 1000,
  "validationRatio": 10
}
```

**Parameters:**
- `contractId`: Contract to execute
- `baseArgument`: Template for contract arguments (use `{iteration}` as placeholder)
- `requestCount`: Number of requests to execute (default: 1000)
- `validationRatio`: Validation frequency
  - `0`: No validation
  - `1`: Validate after every request
  - `N`: Validate once per N requests (e.g., 10 = validate every 10th request)

**Response:**
```json
{
  "success": true,
  "message": "Benchmark completed successfully",
  "data": {
    "startTimeMs": 1699123456789,
    "endTimeMs": 1699123466789,
    "durationMs": 10000,
    "totalRequests": 1000,
    "transactionsPerSecond": 100,
    "validationCount": 100
  }
}
```

---

## Utilities

### HashUtil

Utility class for SHA-256 hash generation (from CmdFileHash sample):

```java
import com.example.demo_dl.util.HashUtil;

String hash = HashUtil.generateSha256Hash("myFileId" + "Create");
```

---

## Example Workflow

1. **Register Certificate**
```bash
curl -X POST http://localhost:8080/api/certificates/register
```

2. **Register Contract**
```bash
curl -X POST http://localhost:8080/api/contracts/register \
  -F "file=@FileHashUpdaterV0910.class" \
  -F 'request={"contractId":"FileHashUpdaterV0910","contractBinaryName":"net.readeng.scalar.dl.hellodl.FileHashUpdaterV0910"};type=application/json'
```

3. **Execute Contract**
```bash
curl -X POST http://localhost:8080/api/contracts/execute \
  -H "Content-Type: application/json" \
  -d '{
    "contractId": "FileHashUpdaterV0910",
    "contractArgument": "{\"fileId\":\"file001\",\"event\":\"Create\",\"hash\":\"abc123\"}"
  }'
```

4. **Validate Ledger**
```bash
curl -X POST http://localhost:8080/api/contracts/validate \
  -H "Content-Type: application/json" \
  -d '{"assetId": "file001"}'
```

5. **Run Benchmark**
```bash
curl -X POST http://localhost:8080/api/benchmark/run \
  -H "Content-Type: application/json" \
  -d '{
    "contractId": "FileHashUpdaterV0910",
    "baseArgument": "{\"fileId\":\"bench-file\",\"event\":\"Event{iteration}\",\"hash\":\"hash{iteration}\"}",
    "requestCount": 1000,
    "validationRatio": 10
  }'
```

---

## Running the Application

```bash
# Build the project
./gradlew build

# Run the application
./gradlew bootRun

# Application will start on http://localhost:8080
```

## Error Handling

All endpoints return a consistent response format:

**Success:**
```json
{
  "success": true,
  "message": "Operation completed successfully",
  "data": {...}
}
```

**Error:**
```json
{
  "success": false,
  "message": "Error description here",
  "data": null
}
```
