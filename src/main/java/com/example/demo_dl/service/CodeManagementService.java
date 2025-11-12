package com.example.demo_dl.service;

import com.example.demo_dl.dto.ContractDefinition;
import com.example.demo_dl.dto.FunctionDefinition;
import com.example.demo_dl.util.VersionUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CodeManagementService {

    private final ObjectMapper objectMapper;
    private static final String BASE_DIR = System.getProperty("user.dir");
    private static final String DEFINITIONS_CONTRACTS_DIR = BASE_DIR + "/definitions/contracts";
    private static final String DEFINITIONS_FUNCTIONS_DIR = BASE_DIR + "/definitions/functions";
    private static final String GENERATED_CONTRACTS_DIR = BASE_DIR + "/generated/contracts";
    private static final String GENERATED_FUNCTIONS_DIR = BASE_DIR + "/generated/functions";

    /**
     * Save contract definition and generated code
     * Definition: UserUpdaterV1_0_0.json
     * Code: UserUpdaterV1_0_0.java
     */
    public void saveContract(ContractDefinition definition, String generatedCode) throws IOException {
        String versionedName = VersionUtil.getVersionedName(
            definition.getName(), definition.getVersion());

        // Save definition as JSON with versioned name
        String definitionPath = DEFINITIONS_CONTRACTS_DIR + "/" + versionedName + ".json";
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(definitionPath), definition);
        log.info("Saved contract definition: {}", definitionPath);

        // Save generated Java code with versioned name
        String codePath = GENERATED_CONTRACTS_DIR + "/" + versionedName + ".java";
        Files.writeString(Paths.get(codePath), generatedCode);
        log.info("Saved generated contract code: {}", codePath);
    }

    /**
     * Save function definition and generated code
     * Definition: PriceCalculatorV1_0_0.json
     * Code: PriceCalculatorV1_0_0.java
     */
    public void saveFunction(FunctionDefinition definition, String generatedCode) throws IOException {
        String versionedName = VersionUtil.getVersionedName(
            definition.getName(), definition.getVersion());

        // Save definition as JSON with versioned name
        String definitionPath = DEFINITIONS_FUNCTIONS_DIR + "/" + versionedName + ".json";
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(definitionPath), definition);
        log.info("Saved function definition: {}", definitionPath);

        // Save generated Java code with versioned name
        String codePath = GENERATED_FUNCTIONS_DIR + "/" + versionedName + ".java";
        Files.writeString(Paths.get(codePath), generatedCode);
        log.info("Saved generated function code: {}", codePath);
    }

    /**
     * Load contract definition by versioned name (e.g., "UserUpdaterV1_0_0")
     * Also supports legacy format "UserUpdater_V1_0_0" for backward compatibility
     */
    public ContractDefinition loadContractDefinition(String versionedName) throws IOException {
        String definitionPath = DEFINITIONS_CONTRACTS_DIR + "/" + versionedName + ".json";
        return objectMapper.readValue(new File(definitionPath), ContractDefinition.class);
    }

    /**
     * Load function definition by versioned name (e.g., "PriceCalculatorV1_0_0")
     * Also supports legacy format "PriceCalculator_V1_0_0" for backward compatibility
     */
    public FunctionDefinition loadFunctionDefinition(String versionedName) throws IOException {
        String definitionPath = DEFINITIONS_FUNCTIONS_DIR + "/" + versionedName + ".json";
        return objectMapper.readValue(new File(definitionPath), FunctionDefinition.class);
    }

    /**
     * List all contract definitions
     */
    public List<String> listContracts() {
        return listDefinitions(DEFINITIONS_CONTRACTS_DIR);
    }

    /**
     * List all function definitions
     */
    public List<String> listFunctions() {
        return listDefinitions(DEFINITIONS_FUNCTIONS_DIR);
    }

    /**
     * Get generated contract code by versioned name (e.g., "UserUpdaterV1_0_0")
     * Also supports legacy format "UserUpdater_V1_0_0" for backward compatibility
     */
    public String getGeneratedContractCode(String versionedName) throws IOException {
        // Normalize to new format if legacy format is provided
        String baseName = VersionUtil.extractBaseName(versionedName);
        String version = VersionUtil.extractVersion(versionedName);
        String normalizedName = VersionUtil.getVersionedName(baseName, version);

        String codePath = GENERATED_CONTRACTS_DIR + "/" + normalizedName + ".java";
        return Files.readString(Paths.get(codePath));
    }

    /**
     * Get generated function code by versioned name (e.g., "PriceCalculatorV1_0_0")
     * Also supports legacy format "PriceCalculator_V1_0_0" for backward compatibility
     */
    public String getGeneratedFunctionCode(String versionedName) throws IOException {
        // Normalize to new format if legacy format is provided
        String baseName = VersionUtil.extractBaseName(versionedName);
        String version = VersionUtil.extractVersion(versionedName);
        String normalizedName = VersionUtil.getVersionedName(baseName, version);

        String codePath = GENERATED_FUNCTIONS_DIR + "/" + normalizedName + ".java";
        return Files.readString(Paths.get(codePath));
    }

    /**
     * Delete contract definition and generated code by versioned name
     * Also supports legacy format for backward compatibility
     */
    public void deleteContract(String versionedName) throws IOException {
        String baseName = VersionUtil.extractBaseName(versionedName);
        String version = VersionUtil.extractVersion(versionedName);
        String normalizedName = VersionUtil.getVersionedName(baseName, version);

        Files.deleteIfExists(Paths.get(DEFINITIONS_CONTRACTS_DIR + "/" + normalizedName + ".json"));
        Files.deleteIfExists(Paths.get(GENERATED_CONTRACTS_DIR + "/" + normalizedName + ".java"));
        log.info("Deleted contract: {}", normalizedName);
    }

    /**
     * Delete function definition and generated code by versioned name
     * Also supports legacy format for backward compatibility
     */
    public void deleteFunction(String versionedName) throws IOException {
        String baseName = VersionUtil.extractBaseName(versionedName);
        String version = VersionUtil.extractVersion(versionedName);
        String normalizedName = VersionUtil.getVersionedName(baseName, version);

        Files.deleteIfExists(Paths.get(DEFINITIONS_FUNCTIONS_DIR + "/" + normalizedName + ".json"));
        Files.deleteIfExists(Paths.get(GENERATED_FUNCTIONS_DIR + "/" + normalizedName + ".java"));
        log.info("Deleted function: {}", normalizedName);
    }

    /**
     * Generate an execute request template for a contract
     * Returns a complete execute request with contractArgument as escaped JSON string
     */
    public Map<String, String> generateContractExecuteTemplate(String versionedName) throws IOException {
        ContractDefinition definition = loadContractDefinition(versionedName);

        // Generate the versioned name for contractId
        String contractId = VersionUtil.getVersionedName(
            definition.getName(), definition.getVersion());

        // Build the argument object
        com.fasterxml.jackson.databind.node.ObjectNode argumentObject = objectMapper.createObjectNode();

        // Add the asset ID field first
        String assetIdField = definition.getAssetIdField();
        argumentObject.put(assetIdField, getExampleValue(assetIdField, "STRING"));

        // Add all other fields
        definition.getFields().forEach(field -> {
            Object exampleValue = getExampleValueForType(field.getType(), field.getName());
            switch (field.getType()) {
                case STRING -> argumentObject.put(field.getName(), (String) exampleValue);
                case INT -> argumentObject.put(field.getName(), (Integer) exampleValue);
                case LONG -> argumentObject.put(field.getName(), (Long) exampleValue);
                case DOUBLE -> argumentObject.put(field.getName(), (Double) exampleValue);
                case BOOLEAN -> argumentObject.put(field.getName(), (Boolean) exampleValue);
            }
        });

        // Convert argument object to JSON string (this will be the value of contractArgument)
        String contractArgumentString = objectMapper.writeValueAsString(argumentObject);

        // Build the execute request template as a Map
        Map<String, String> executeTemplate = new HashMap<>();
        executeTemplate.put("contractId", contractId);
        executeTemplate.put("contractArgument", contractArgumentString);
        executeTemplate.put("functionId", "");
        executeTemplate.put("functionArgument", "");

        return executeTemplate;
    }

    /**
     * Get example value based on field type and name
     */
    private Object getExampleValueForType(com.example.demo_dl.dto.FieldDefinition.FieldType type, String fieldName) {
        String lowerName = fieldName.toLowerCase();

        return switch (type) {
            case STRING -> getExampleValue(lowerName, "STRING");
            case INT -> {
                if (lowerName.contains("age")) yield 30;
                if (lowerName.contains("count") || lowerName.contains("quantity")) yield 10;
                yield 100;
            }
            case LONG -> {
                if (lowerName.contains("timestamp") || lowerName.contains("time"))
                    yield System.currentTimeMillis();
                if (lowerName.contains("id")) yield 123456789L;
                yield 1000L;
            }
            case DOUBLE -> {
                if (lowerName.contains("price") || lowerName.contains("amount")) yield 99.99;
                if (lowerName.contains("rate") || lowerName.contains("percentage")) yield 0.15;
                yield 1.5;
            }
            case BOOLEAN -> {
                if (lowerName.contains("active") || lowerName.contains("enabled")) yield true;
                yield false;
            }
        };
    }

    /**
     * Get example string value based on field name
     */
    private String getExampleValue(String fieldName, String type) {
        String lowerName = fieldName.toLowerCase();

        if (lowerName.contains("email")) return "user@example.com";
        if (lowerName.contains("phone")) return "+1-555-0123";
        if (lowerName.contains("address")) return "123 Main St, Springfield";
        if (lowerName.contains("name") && !lowerName.contains("file")) return "John Doe";
        if (lowerName.contains("username") || lowerName.equals("user")) return "john_doe";
        if (lowerName.contains("id")) return "id_" + System.currentTimeMillis();
        if (lowerName.contains("url") || lowerName.contains("link")) return "https://example.com";
        if (lowerName.contains("code")) return "ABC123";
        if (lowerName.contains("description")) return "Sample description";
        if (lowerName.contains("status")) return "active";

        return "example_" + fieldName;
    }

    private List<String> listDefinitions(String directory) {
        List<String> definitions = new ArrayList<>();
        File dir = new File(directory);

        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
            if (files != null) {
                for (File file : files) {
                    String name = file.getName().replace(".json", "");
                    definitions.add(name);
                }
            }
        }

        return definitions;
    }
}
