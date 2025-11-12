package com.example.demo_dl.service;

import com.example.demo_dl.dto.FieldDefinition;
import com.example.demo_dl.dto.FunctionDefinition;
import com.example.demo_dl.util.VersionUtil;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class FunctionGenerator {

    public String generateFunction(FunctionDefinition definition) {
        StringBuilder sb = new StringBuilder();

        // Generate versioned class name: PriceCalculator + V1_0_0 = PriceCalculatorV1_0_0
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
        sb.append("import com.scalar.dl.ledger.function.JacksonBasedFunction;\n");
        sb.append("import com.scalar.dl.ledger.exception.ContractContextException;\n");
        sb.append("import com.scalar.dl.ledger.database.Database;\n");
        sb.append("import com.scalar.db.api.Get;\n");
        sb.append("import com.scalar.db.api.Scan;\n");
        sb.append("import com.scalar.db.api.Put;\n");
        sb.append("import com.scalar.db.api.Delete;\n");
        sb.append("import com.scalar.db.api.Result;\n\n");

        // Class declaration with JavaDoc
        if (definition.getDescription() != null) {
            sb.append("/**\n");
            sb.append(" * ").append(definition.getDescription()).append("\n");
            sb.append(" * Base Name: ").append(definition.getName()).append("\n");
            sb.append(" * Version: ").append(definition.getVersion()).append("\n");
            sb.append(" */\n");
        }
        sb.append("public class ").append(versionedClassName);
        sb.append(" extends JacksonBasedFunction {\n\n");

        // invoke method
        sb.append("    @Override\n");
        sb.append("    public JsonNode invoke(\n");
        sb.append("            Database<Get, Scan, Put, Delete, Result> database,\n");
        sb.append("            JsonNode argument,\n");
        sb.append("            JsonNode properties,\n");
        sb.append("            JsonNode contractProperties) {\n\n");

        // Input field validation
        sb.append(generateInputValidation(definition));

        // Input field extraction
        sb.append(generateInputExtraction(definition));

        // Generate function logic
        sb.append(generateFunctionLogic(definition));

        sb.append("    }\n");
        sb.append("}\n");

        return sb.toString();
    }

    private String generateInputValidation(FunctionDefinition definition) {
        StringBuilder sb = new StringBuilder();

        if (definition.getInputFields() == null || definition.getInputFields().isEmpty()) {
            return "";
        }

        // Collect required input fields
        boolean hasRequired = definition.getInputFields().stream()
            .anyMatch(FieldDefinition::isRequired);

        if (!hasRequired) {
            return "";
        }

        sb.append("        if (");
        String conditions = definition.getInputFields().stream()
            .filter(FieldDefinition::isRequired)
            .map(f -> "!argument.has(\"" + f.getName() + "\")")
            .collect(Collectors.joining(" || "));
        sb.append(conditions);
        sb.append(") {\n");

        String requiredFields = definition.getInputFields().stream()
            .filter(FieldDefinition::isRequired)
            .map(FieldDefinition::getName)
            .collect(Collectors.joining(", "));
        sb.append("            throw new ContractContextException(\n");
        sb.append("                \"Required input fields: ").append(requiredFields).append("\");\n");
        sb.append("        }\n\n");

        return sb.toString();
    }

    private String generateInputExtraction(FunctionDefinition definition) {
        StringBuilder sb = new StringBuilder();

        if (definition.getInputFields() == null || definition.getInputFields().isEmpty()) {
            return "";
        }

        // Extract input fields
        for (FieldDefinition field : definition.getInputFields()) {
            String javaType = getJavaType(field.getType());
            String extractMethod = getExtractMethod(field.getType());

            sb.append("        ").append(javaType).append(" ").append(field.getName());
            sb.append(" = argument.get(\"").append(field.getName()).append("\")");
            sb.append(".").append(extractMethod).append("();\n");
        }
        sb.append("\n");

        return sb.toString();
    }

    private String generateFunctionLogic(FunctionDefinition definition) {
        StringBuilder sb = new StringBuilder();

        sb.append("        // TODO: Implement custom logic here\n");
        sb.append("        // You can use the 'database' parameter to read/write external database\n");
        sb.append("        // Example: database.put(Put.newBuilder()...)\n");
        sb.append("        // Example: Optional<Result> result = database.get(Get.newBuilder()...)\n");
        if (definition.getLogic() != null && !definition.getLogic().isEmpty()) {
            sb.append("        // Logic: ").append(definition.getLogic()).append("\n");
        }
        sb.append("\n");

        // Build output object
        sb.append("        return getObjectMapper().createObjectNode()\n");

        if (definition.getOutputFields() != null && !definition.getOutputFields().isEmpty()) {
            for (int i = 0; i < definition.getOutputFields().size(); i++) {
                FieldDefinition field = definition.getOutputFields().get(i);
                sb.append("            .put(\"").append(field.getName()).append("\", ");

                // For now, just echo input or use placeholder
                if (definition.getInputFields() != null &&
                    definition.getInputFields().stream().anyMatch(f -> f.getName().equals(field.getName()))) {
                    sb.append(field.getName());
                } else {
                    sb.append(getDefaultValue(field.getType()));
                }
                sb.append(")");
                if (i < definition.getOutputFields().size() - 1) {
                    sb.append("\n");
                }
            }
        } else {
            sb.append("            .put(\"status\", \"success\")");
        }
        sb.append(";\n");

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

    private String getDefaultValue(FieldDefinition.FieldType type) {
        switch (type) {
            case STRING: return "\"\"";
            case INT: return "0";
            case LONG: return "0L";
            case BOOLEAN: return "false";
            case DOUBLE: return "0.0";
            default: return "\"\"";
        }
    }
}
