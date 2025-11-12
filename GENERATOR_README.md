# ScalarDL Contract & Function Generator

This system provides automated generation of ScalarDL contracts and functions through REST APIs. Define your contract/function structure via JSON, and the system generates ready-to-use Java code.

## Table of Contents

- [Overview](#overview)
- [Directory Structure](#directory-structure)
- [Generating Contracts](#generating-contracts)
- [Generating Functions](#generating-functions)
- [API Reference](#api-reference)
- [Examples](#examples)
- [Next Steps](#next-steps)

---

## Overview

### What is This?

A code generation system that creates ScalarDL contracts and functions based on JSON definitions. This eliminates boilerplate code and ensures consistent patterns across your contracts.

### Key Features

- **REST API-based generation** - Generate code via HTTP endpoints
- **Definition storage** - JSON definitions saved for versioning and reuse
- **Two contract types**:
  - **READER** - Read-only contracts using `ledger.get()`
  - **UPDATER** - Read+Write contracts using `ledger.get()` + `ledger.put()`
- **Function generation** - Stateless functions for computations
- **Type-safe field handling** - Supports STRING, INT, LONG, BOOLEAN, DOUBLE

---

## Directory Structure

```
demo-dl/
├── definitions/
│   ├── contracts/          # Contract definition JSON files
│   │   ├── OrderReader.json
│   │   └── OrderUpdater.json
│   └── functions/          # Function definition JSON files
│       └── PriceCalculator.json
├── generated/
│   ├── contracts/          # Generated contract Java files
│   │   ├── OrderReader.java
│   │   └── OrderUpdater.java
│   └── functions/          # Generated function Java files
│       └── PriceCalculator.java
├── compiled/               # Compiled .class files (auto-generated)
│   ├── contracts/
│   │   └── com/example/contracts/
│   │       ├── OrderReader.class
│   │       └── OrderUpdater.class
│   └── functions/
│       └── com/example/functions/
│           └── PriceCalculator.class
├── examples/               # Example definition files
│   ├── OrderReader.json
│   ├── OrderUpdater.json
│   ├── UserUpdater.json
│   └── PriceCalculator.json
```

---

## Contract Versioning

### Why Versioning Matters

**ScalarDL is immutable** - once a contract is registered, it cannot be updated. To deploy a new version, you must register it with a **different class name**.

This generator automatically handles versioning by appending the version to the class name.

### Naming Convention

**Format**: `{ContractName}V{Major}_{Minor}_{Patch}`

Examples:
- Version `1.0.0` → Class: `UserUpdaterV1_0_0`
- Version `1.2.3` → Class: `UserUpdaterV1_2_3`
- Version `10.10.10` → Class: `UserUpdaterV10_10_10`
- Version `2.15.7` → Class: `OrderProcessorV2_15_7`

### File Naming

**Definition files**: `{Name}_V{Version}.json`
**Java source files**: `{Name}V{Version}.java` (class name)
**Class files**: `{Name}V{Version}.class`

Example for UserUpdater v1.0.0:
```
definitions/contracts/UserUpdater_V1_0_0.json    # Definition
generated/contracts/UserUpdaterV1_0_0.java       # Source code
compiled/contracts/.../UserUpdaterV1_0_0.class   # Compiled
```

### Multiple Versions

You can have multiple versions of the same contract simultaneously:

```json
// Version 1.0.0
{
  "name": "UserUpdater",
  "version": "1.0.0",
  ...
}

// Version 2.0.0 - Different fields, same base name
{
  "name": "UserUpdater",
  "version": "2.0.0",
  ...
}
```

Both will coexist:
- `UserUpdaterV1_0_0` registered and running in production
- `UserUpdaterV2_0_0` registered for new features

### Best Practices

1. **Semantic Versioning**: Use `MAJOR.MINOR.PATCH`
   - MAJOR: Breaking changes
   - MINOR: New features (backward compatible)
   - PATCH: Bug fixes

2. **Version Progression**: Don't skip versions
   ✅ 1.0.0 → 1.1.0 → 1.2.0
   ❌ 1.0.0 → 3.0.0

3. **Documentation**: Update contract description with changelog

---

## Generating Contracts

### Contract Types

#### READER Contract
- **Purpose**: Read-only operations
- **Uses**: `ledger.get(assetId)`
- **Returns**: Asset data (id, age, data)
- **No state changes**

#### UPDATER Contract
- **Purpose**: Create/Update operations
- **Uses**: `ledger.get(assetId)` + `ledger.put(assetId, data)`
- **Returns**: Confirmation with assetId
- **Modifies ledger state**

### Definition Structure

```json
{
  "name": "OrderUpdater",
  "contractType": "UPDATER",
  "version": "1.0.0",
  "packageName": "com.example.contracts",
  "assetIdField": "orderId",
  "description": "Updates order information in the ledger",
  "fields": [
    {
      "name": "customerId",
      "type": "STRING",
      "required": true,
      "description": "Customer identifier"
    },
    {
      "name": "amount",
      "type": "INT",
      "required": true,
      "description": "Order amount"
    },
    {
      "name": "status",
      "type": "STRING",
      "required": true,
      "description": "Order status"
    }
  ]
}
```

### Field Types

| Type | Java Type | JsonNode Method | Example Value |
|------|-----------|----------------|---------------|
| STRING | String | asText() | "customer123" |
| INT | int | asInt() | 42 |
| LONG | long | asLong() | 123456789L |
| BOOLEAN | boolean | asBoolean() | true |
| DOUBLE | double | asDouble() | 99.99 |

### Generate via API

**Endpoint**: `POST /api/generator/contracts`

**Request**:
```json
{
  "name": "OrderUpdater",
  "contractType": "UPDATER",
  "version": "1.0.0",
  "packageName": "com.example.contracts",
  "assetIdField": "orderId",
  "fields": [
    {"name": "customerId", "type": "STRING", "required": true},
    {"name": "amount", "type": "INT", "required": true},
    {"name": "status", "type": "STRING", "required": true}
  ]
}
```

**Response**:
```json
{
  "success": true,
  "message": "Contract generated successfully: OrderUpdater",
  "data": {
    "name": "OrderUpdater",
    "type": "UPDATER",
    "definitionPath": "definitions/contracts/OrderUpdater.json",
    "codePath": "generated/contracts/OrderUpdater.java"
  }
}
```

### Generated Contract Example

The above definition generates:

```java
package com.example.contracts;

import com.fasterxml.jackson.databind.JsonNode;
import com.scalar.dl.ledger.contract.JacksonBasedContract;
import com.scalar.dl.ledger.exception.ContractContextException;
import com.scalar.dl.ledger.statemachine.Asset;
import com.scalar.dl.ledger.statemachine.Ledger;
import java.util.Optional;
import javax.annotation.Nullable;

/**
 * Updates order information in the ledger
 * Generated contract - Version: 1.0.0
 */
public class OrderUpdater extends JacksonBasedContract {

    @Nullable
    @Override
    public JsonNode invoke(
        Ledger<JsonNode> ledger, JsonNode argument, @Nullable JsonNode properties) {

        if (!argument.has("orderId") || !argument.has("customerId") || !argument.has("amount") || !argument.has("status")) {
            throw new ContractContextException(
                "Required fields: orderId, customerId, amount, status");
        }

        String orderId = argument.get("orderId").asText();
        String customerId = argument.get("customerId").asText();
        int amount = argument.get("amount").asInt();
        String status = argument.get("status").asText();

        Optional<Asset<JsonNode>> existing = ledger.get(orderId);

        ledger.put(orderId, getObjectMapper().createObjectNode()
            .put("customerId", customerId)
            .put("amount", amount)
            .put("status", status));

        return getObjectMapper().createObjectNode()
            .put("status", "success")
            .put("assetId", orderId);
    }
}
```

---

## Generating Functions

### What are ScalarDL Functions?

Functions are **stateless** components used for:
- Data transformation
- Calculations
- Validation logic
- Processing that doesn't require ledger access

### Definition Structure

```json
{
  "name": "PriceCalculator",
  "version": "1.0.0",
  "packageName": "com.example.functions",
  "description": "Calculates total price with tax",
  "inputFields": [
    {
      "name": "basePrice",
      "type": "DOUBLE",
      "required": true,
      "description": "Base price before tax"
    },
    {
      "name": "taxRate",
      "type": "DOUBLE",
      "required": true,
      "description": "Tax rate as decimal"
    }
  ],
  "outputFields": [
    {
      "name": "totalPrice",
      "type": "DOUBLE",
      "required": false,
      "description": "Total price including tax"
    }
  ],
  "logic": "Calculate totalPrice = basePrice * (1 + taxRate)"
}
```

### Generate via API

**Endpoint**: `POST /api/generator/functions`

**Request**:
```json
{
  "name": "PriceCalculator",
  "version": "1.0.0",
  "packageName": "com.example.functions",
  "inputFields": [
    {"name": "basePrice", "type": "DOUBLE", "required": true},
    {"name": "taxRate", "type": "DOUBLE", "required": true}
  ],
  "outputFields": [
    {"name": "totalPrice", "type": "DOUBLE"}
  ]
}
```

**Response**:
```json
{
  "success": true,
  "message": "Function generated successfully: PriceCalculator",
  "data": {
    "name": "PriceCalculator",
    "definitionPath": "definitions/functions/PriceCalculator.json",
    "codePath": "generated/functions/PriceCalculator.java"
  }
}
```

### Generated Function Template

```java
package com.example.functions;

import com.fasterxml.jackson.databind.JsonNode;
import com.scalar.dl.ledger.function.JacksonBasedFunction;
import com.scalar.dl.ledger.exception.ContractContextException;
import javax.annotation.Nullable;

/**
 * Calculates total price with tax
 * Generated function - Version: 1.0.0
 */
public class PriceCalculator extends JacksonBasedFunction {

    @Nullable
    @Override
    public JsonNode invoke(JsonNode argument, @Nullable JsonNode properties) {

        if (!argument.has("basePrice") || !argument.has("taxRate")) {
            throw new ContractContextException(
                "Required input fields: basePrice, taxRate");
        }

        double basePrice = argument.get("basePrice").asDouble();
        double taxRate = argument.get("taxRate").asDouble();

        // TODO: Implement custom logic here
        // Logic: Calculate totalPrice = basePrice * (1 + taxRate)

        return getObjectMapper().createObjectNode()
            .put("totalPrice", 0.0);
    }
}
```

**Note**: Functions generate template code with TODO comments. You'll need to implement the actual business logic.

---

## API Reference

### Contract Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/generator/contracts?compile=true` | Generate new contract (optionally compile) |
| POST | `/api/generator/contracts/{name}/compile` | Compile existing contract |
| GET | `/api/generator/contracts` | List all contracts |
| GET | `/api/generator/contracts/{name}` | Get contract definition |
| GET | `/api/generator/contracts/{name}/code` | Get generated code |
| DELETE | `/api/generator/contracts/{name}` | Delete contract |

### Function Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/generator/functions?compile=true` | Generate new function (optionally compile) |
| POST | `/api/generator/functions/{name}/compile` | Compile existing function |
| GET | `/api/generator/functions` | List all functions |
| GET | `/api/generator/functions/{name}` | Get function definition |
| GET | `/api/generator/functions/{name}/code` | Get generated code |
| DELETE | `/api/generator/functions/{name}` | Delete function |

---

## Examples

### Example 1: File Hash Storage (UPDATER)

**Definition**:
```json
{
  "name": "FileHashUpdater",
  "contractType": "UPDATER",
  "version": "1.0.0",
  "packageName": "com.example.contracts",
  "assetIdField": "fileId",
  "fields": [
    {"name": "hash", "type": "STRING", "required": true},
    {"name": "event", "type": "STRING", "required": true}
  ]
}
```

**Execution** (via `/api/contracts/execute`):
```json
{
  "contractId": "FileHashUpdater",
  "contractArgument": "{\"fileId\": \"abc-123\", \"hash\": \"sha256:abcd\", \"event\": \"upload\"}"
}
```

### Example 2: File Hash Retrieval (READER)

**Definition**:
```json
{
  "name": "FileHashReader",
  "contractType": "READER",
  "version": "1.0.0",
  "packageName": "com.example.contracts",
  "assetIdField": "fileId",
  "fields": []
}
```

**Execution**:
```json
{
  "contractId": "FileHashReader",
  "contractArgument": "{\"fileId\": \"abc-123\"}"
}
```

### Example 3: User Account Creation (UPDATER)

**Definition**:
```json
{
  "name": "UserAccountCreator",
  "contractType": "UPDATER",
  "version": "1.0.0",
  "packageName": "com.example.contracts",
  "assetIdField": "userId",
  "fields": [
    {"name": "username", "type": "STRING", "required": true},
    {"name": "email", "type": "STRING", "required": true},
    {"name": "createdAt", "type": "LONG", "required": true},
    {"name": "active", "type": "BOOLEAN", "required": true}
  ]
}
```

---

## Next Steps

After generating your contract/function:

### 1. Review Generated Code

```bash
# View generated contract
cat generated/contracts/OrderUpdater.java

# View definition
cat definitions/contracts/OrderUpdater.json
```

### 2. Customize Logic (Functions Only)

For functions, edit the generated `.java` file to implement your business logic:

```java
// Replace this:
return getObjectMapper().createObjectNode()
    .put("totalPrice", 0.0);

// With your logic:
double totalPrice = basePrice * (1 + taxRate);
return getObjectMapper().createObjectNode()
    .put("totalPrice", totalPrice);
```

### 3. Compile the Contract/Function

#### Option A: Auto-Compile via API (Recommended)

**During Generation:**
```bash
# Generate and compile in one step
curl -X POST "http://localhost:8080/api/generator/contracts?compile=true" \
  -H "Content-Type: application/json" \
  -d @examples/UserUpdater.json
```

**Response:**
```json
{
  "success": true,
  "message": "Contract generated and compiled successfully: UserUpdater",
  "data": {
    "name": "UserUpdater",
    "type": "UPDATER",
    "definitionPath": "definitions/contracts/UserUpdater.json",
    "codePath": "generated/contracts/UserUpdater.java",
    "compiled": "true",
    "classFilePath": "/home/user/demo-dl/compiled/contracts/com/example/contracts/UserUpdater.class"
  }
}
```

**After Generation:**
```bash
# Compile existing contract
curl -X POST http://localhost:8080/api/generator/contracts/UserUpdater/compile

# Compile existing function
curl -X POST http://localhost:8080/api/generator/functions/PriceCalculator/compile
```

#### Option B: Manual Compilation

If auto-compilation fails or you prefer manual control:

```bash
# Copy generated file to your ScalarDL contract project
cp generated/contracts/OrderUpdater.java /path/to/scalardl-project/src/main/java/com/example/contracts/

# Compile using Maven/Gradle
cd /path/to/scalardl-project
mvn clean compile
```

### 4. Register with ScalarDL

Use the `/api/contracts/register` endpoint with the compiled `.class` file.

**IMPORTANT**: Use the **versioned class name** from the generation response!

**Using auto-compiled file (v1.0.0):**
```bash
curl -X POST http://localhost:8080/api/contracts/register \
  -F "file=@compiled/contracts/com/example/contracts/UserUpdaterV1_0_0.class" \
  -F 'request={"contractId":"UserUpdaterV1_0_0","contractBinaryName":"com.example.contracts.UserUpdaterV1_0_0","contractProperties":{}}'
```

**Or via Swagger UI:**
1. Go to `POST /api/contracts/register`
2. Upload the `.class` file: `compiled/contracts/com/example/contracts/UserUpdaterV1_0_0.class`
3. Provide the JSON request:
   ```json
   {
     "contractId": "UserUpdaterV1_0_0",
     "contractBinaryName": "com.example.contracts.UserUpdaterV1_0_0",
     "contractProperties": {}
   }
   ```

**Note**: The `contractId` and `contractBinaryName` must match the versioned class name (`UserUpdaterV1_0_0`)

### 5. Execute the Contract

```bash
curl -X POST http://localhost:8080/api/contracts/execute \
  -H "Content-Type: application/json" \
  -d '{
    "contractId": "OrderUpdater",
    "contractArgument": "{\"orderId\":\"order-001\",\"customerId\":\"cust-123\",\"amount\":500,\"status\":\"pending\"}"
  }'
```

---

## Tips & Best Practices

### Naming Conventions

- **Contracts**: Use descriptive names ending with operation type
  - `FileHashReader`, `FileHashUpdater`
  - `OrderCreator`, `OrderReader`
  - `UserAccountUpdater`

- **Functions**: Use action-oriented names
  - `PriceCalculator`
  - `HashValidator`
  - `DataTransformer`

### Version Management

- Include version in contract name for breaking changes: `OrderUpdaterV2`
- Or use the `version` field and keep name stable: `"version": "2.0.0"`

### Field Design

- **Keep it simple**: Start with essential fields only
- **Use descriptive names**: `customerId` not `cid`
- **Mark required fields**: Set `required: true` for mandatory data
- **Add descriptions**: Helps document your contracts

### Testing

1. Generate contract with test data
2. Review generated code for correctness
3. Compile and fix any issues
4. Test with ScalarDL locally before production

---

## Troubleshooting

### "Failed to generate contract"

- Check that all required fields are provided
- Ensure field names are valid Java identifiers
- Verify packageName uses valid package syntax

### "Definition not found"

- Ensure the definition was successfully saved
- Check the `definitions/contracts/` or `definitions/functions/` directory
- Verify the name matches exactly (case-sensitive)

### Generated Code Won't Compile

- Ensure ScalarDL dependencies are on classpath
- Check package name matches directory structure
- Verify all imports are available

---

## Support

For issues or questions:
- Check the main project README: `CLAUDE.md`
- Review ScalarDL documentation: https://scalardl.scalar-labs.com/docs/latest/
- Examine sample contracts in `/path/to/hellodl/` project

---

**Generated by the ScalarDL Contract & Function Generator**
