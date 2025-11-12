package com.example.demo_dl.service;

import com.example.demo_dl.dto.ContractDefinition;
import com.example.demo_dl.dto.FieldDefinition;
import com.example.demo_dl.util.VersionUtil;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class ContractGenerator {

    public String generateContract(ContractDefinition definition) {
        StringBuilder sb = new StringBuilder();

        // Generate versioned class name: UserUpdater + V1_0_0 = UserUpdaterV1_0_0
        String versionedClassName = VersionUtil.getVersionedName(
            definition.getName(),
            definition.getVersion()
        );

        // Package declaration
        String packageName = definition.getPackageName() != null ?
            definition.getPackageName() : "net.readeng.scalar.dl.generated";
        sb.append("package ").append(packageName).append(";\n\n");

        // Imports
        sb.append("import com.fasterxml.jackson.databind.JsonNode;\n");
        sb.append("import com.scalar.dl.ledger.contract.JacksonBasedContract;\n");
        sb.append("import com.scalar.dl.ledger.exception.ContractContextException;\n");
        sb.append("import com.scalar.dl.ledger.statemachine.Asset;\n");
        sb.append("import com.scalar.dl.ledger.statemachine.Ledger;\n");
        sb.append("import java.util.Optional;\n\n");

        // Class declaration with JavaDoc
        if (definition.getDescription() != null) {
            sb.append("/**\n");
            sb.append(" * ").append(definition.getDescription()).append("\n");
            sb.append(" * Base Name: ").append(definition.getName()).append("\n");
            sb.append(" * Version: ").append(definition.getVersion()).append("\n");
            sb.append(" */\n");
        }
        sb.append("public class ").append(versionedClassName);
        sb.append(" extends JacksonBasedContract {\n\n");

        // invoke method
        sb.append("    @Override\n");
        sb.append("    public JsonNode invoke(\n");
        sb.append("        Ledger<JsonNode> ledger, JsonNode argument, JsonNode properties) {\n\n");

        // Field validation
        sb.append(generateFieldValidation(definition));

        // Field extraction
        sb.append(generateFieldExtraction(definition));

        // Generate contract logic based on type
        if (definition.getContractType() == ContractDefinition.ContractType.READER) {
            sb.append(generateReaderLogic(definition));
        } else {
            sb.append(generateUpdaterLogic(definition));
        }

        sb.append("    }\n");
        sb.append("}\n");

        return sb.toString();
    }

    private String generateFieldValidation(ContractDefinition definition) {
        StringBuilder sb = new StringBuilder();

        // Collect required fields
        String requiredFields = definition.getFields().stream()
            .filter(FieldDefinition::isRequired)
            .map(f -> "\"" + f.getName() + "\"")
            .collect(Collectors.joining(", "));

        // Add assetIdField to validation
        sb.append("        if (!argument.has(\"").append(definition.getAssetIdField()).append("\")");
        for (FieldDefinition field : definition.getFields()) {
            if (field.isRequired()) {
                sb.append(" || !argument.has(\"").append(field.getName()).append("\")");
            }
        }
        sb.append(") {\n");
        sb.append("            throw new ContractContextException(\n");
        sb.append("                \"Required fields: ").append(definition.getAssetIdField());
        if (!requiredFields.isEmpty()) {
            sb.append(", ").append(requiredFields.replace("\"", ""));
        }
        sb.append("\");\n");
        sb.append("        }\n\n");

        return sb.toString();
    }

    private String generateFieldExtraction(ContractDefinition definition) {
        StringBuilder sb = new StringBuilder();

        // Extract assetId
        sb.append("        String ").append(definition.getAssetIdField());
        sb.append(" = argument.get(\"").append(definition.getAssetIdField()).append("\").asText();\n");

        // Extract other fields
        for (FieldDefinition field : definition.getFields()) {
            String javaType = getJavaType(field.getType());
            String extractMethod = getExtractMethod(field.getType());

            sb.append("        ").append(javaType).append(" ").append(field.getName());
            sb.append(" = argument.get(\"").append(field.getName()).append("\")");
            sb.append(".").append(extractMethod).append("();\n");
        }
        sb.append("\n");

        return sb.toString();
    }

    private String generateReaderLogic(ContractDefinition definition) {
        StringBuilder sb = new StringBuilder();

        sb.append("        Optional<Asset<JsonNode>> asset = ledger.get(");
        sb.append(definition.getAssetIdField()).append(");\n\n");

        sb.append("        return asset\n");
        sb.append("            .map(value ->\n");
        sb.append("                (JsonNode) getObjectMapper()\n");
        sb.append("                    .createObjectNode()\n");
        sb.append("                    .put(\"id\", value.id())\n");
        sb.append("                    .put(\"age\", value.age())\n");
        sb.append("                    .set(\"data\", value.data()))\n");
        sb.append("            .orElse(null);\n");

        return sb.toString();
    }

    private String generateUpdaterLogic(ContractDefinition definition) {
        StringBuilder sb = new StringBuilder();

        // Get existing asset (optional)
        sb.append("        Optional<Asset<JsonNode>> existing = ledger.get(");
        sb.append(definition.getAssetIdField()).append(");\n\n");

        // Create data object
        sb.append("        ledger.put(").append(definition.getAssetIdField());
        sb.append(", getObjectMapper().createObjectNode()\n");
        for (int i = 0; i < definition.getFields().size(); i++) {
            FieldDefinition field = definition.getFields().get(i);
            sb.append("            .put(\"").append(field.getName()).append("\", ");
            sb.append(field.getName()).append(")");
            if (i < definition.getFields().size() - 1) {
                sb.append("\n");
            }
        }
        sb.append(");\n\n");

        // Return confirmation
        sb.append("        return getObjectMapper().createObjectNode()\n");
        sb.append("            .put(\"status\", \"success\")\n");
        sb.append("            .put(\"assetId\", ").append(definition.getAssetIdField()).append(");\n");

        return sb.toString();
    }

    private String getJavaType(FieldDefinition.FieldType type) {
        switch (type) {
            case STRING: return "String";
            case INT: return "int";
            case LONG: return "long";
            case BOOLEAN: return "boolean";
            case DOUBLE: return "double";
            default: return "String";
        }
    }

    private String getExtractMethod(FieldDefinition.FieldType type) {
        switch (type) {
            case STRING: return "asText";
            case INT: return "asInt";
            case LONG: return "asLong";
            case BOOLEAN: return "asBoolean";
            case DOUBLE: return "asDouble";
            default: return "asText";
        }
    }
}
