package com.example.demo_dl.service;

import com.example.demo_dl.config.ScalarDLProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.scalar.dl.client.config.ClientConfig;
import com.scalar.dl.client.exception.ClientException;
import com.scalar.dl.client.service.ClientService;
import com.scalar.dl.client.service.ClientServiceFactory;
import com.scalar.dl.ledger.model.ContractExecutionResult;
import com.scalar.dl.ledger.model.LedgerValidationResult;
import com.scalar.dl.ledger.proof.AssetProof;
import com.scalar.dl.ledger.util.JacksonSerDe;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScalarDLService {

    private final ScalarDLProperties scalarDLProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JacksonSerDe serde = new JacksonSerDe(new ObjectMapper());

    /**
     * Create ClientConfig from properties
     */
    private ClientConfig createClientConfig() throws IOException {
        Properties props = new Properties();

        // Server configuration
        props.setProperty("scalar.dl.client.server.host", scalarDLProperties.getServerHost());
        props.setProperty("scalar.dl.client.server.port", String.valueOf(scalarDLProperties.getServerPort()));
        props.setProperty("scalar.dl.client.server.privileged_port",
                String.valueOf(scalarDLProperties.getServerPrivilegedPort()));

        // Certificate configuration
        props.setProperty("scalar.dl.client.cert_holder_id", scalarDLProperties.getCertHolderId());
        props.setProperty("scalar.dl.client.cert_version", String.valueOf(scalarDLProperties.getCertVersion()));

        if (scalarDLProperties.getCertPath() != null) {
            props.setProperty("scalar.dl.client.cert_path", scalarDLProperties.getCertPath());
        }
        if (scalarDLProperties.getCertPem() != null) {
            props.setProperty("scalar.dl.client.cert_pem", scalarDLProperties.getCertPem());
        }
        if (scalarDLProperties.getPrivateKeyPath() != null) {
            props.setProperty("scalar.dl.client.private_key_path", scalarDLProperties.getPrivateKeyPath());
        }
        if (scalarDLProperties.getPrivateKeyPem() != null) {
            props.setProperty("scalar.dl.client.private_key_pem", scalarDLProperties.getPrivateKeyPem());
        }

        // TLS configuration
        if (scalarDLProperties.getTlsEnabled()) {
            props.setProperty("scalar.dl.client.tls.enabled", "true");
            if (scalarDLProperties.getTlsCaRootCertPath() != null) {
                props.setProperty("scalar.dl.client.tls.ca_root_cert_path",
                        scalarDLProperties.getTlsCaRootCertPath());
            }
        }

        // Auditor configuration
        if (scalarDLProperties.getAuditor().getEnabled()) {
            props.setProperty("scalar.dl.client.auditor.enabled", "true");
            props.setProperty("scalar.dl.client.auditor.host", scalarDLProperties.getAuditor().getHost());
            props.setProperty("scalar.dl.client.auditor.port",
                    String.valueOf(scalarDLProperties.getAuditor().getPort()));
            props.setProperty("scalar.dl.client.auditor.privileged_port",
                    String.valueOf(scalarDLProperties.getAuditor().getPrivilegedPort()));

            if (scalarDLProperties.getAuditor().getTlsEnabled()) {
                props.setProperty("scalar.dl.client.auditor.tls.enabled", "true");
            }
        }

        return new ClientConfig(props);
    }

    /**
     * Register client certificate
     */
    public void registerCertificate() throws ClientException, IOException {
        ClientServiceFactory factory = new ClientServiceFactory();
        try {
            ClientService service = factory.create(createClientConfig());
            service.registerCertificate();
            log.info("Certificate registered successfully");
        } finally {
            factory.close();
        }
    }

    /**
     * Register contract with byte array
     */
    public void registerContract(String contractId, String contractBinaryName,
                                 byte[] contractBytes, String contractProperties)
            throws ClientException, IOException {

        // Save contract to temporary file
        Path tempFile = Files.createTempFile("contract-", ".class");
        try {
            Files.write(tempFile, contractBytes);
            registerContract(contractId, contractBinaryName,
                    tempFile.toString(), contractProperties);
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    /**
     * Register contract with file path
     */
    public void registerContract(String contractId, String contractBinaryName,
                                 String contractClassFile, String contractProperties)
            throws ClientException, IOException {

        ClientServiceFactory factory = new ClientServiceFactory();
        try {
            ClientService service = factory.create(createClientConfig());

            JsonNode jsonContractProperties = null;
            if (contractProperties != null && !contractProperties.isEmpty()) {
                jsonContractProperties = serde.deserialize(contractProperties);
            }

            service.registerContract(contractId, contractBinaryName,
                    contractClassFile, jsonContractProperties);
            log.info("Contract registered: {}", contractId);
        } finally {
            factory.close();
        }
    }

    /**
     * Register function with byte array
     */
    public void registerFunction(String functionId, String functionBinaryName,
                                byte[] functionBytes)
            throws ClientException, IOException {

        // Save function to temporary file
        Path tempFile = Files.createTempFile("function-", ".class");
        try {
            Files.write(tempFile, functionBytes);
            registerFunction(functionId, functionBinaryName, tempFile.toString());
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    /**
     * Register function with file path
     */
    public void registerFunction(String functionId, String functionBinaryName,
                                String functionClassFile)
            throws ClientException, IOException {

        ClientServiceFactory factory = new ClientServiceFactory();
        try {
            ClientService service = factory.create(createClientConfig());
            service.registerFunction(functionId, functionBinaryName, functionClassFile);
            log.info("Function registered: {}", functionId);
        } finally {
            factory.close();
        }
    }

    /**
     * Execute contract
     */
    public Map<String, Object> executeContract(String contractId, String contractArgument)
            throws ClientException, IOException {
        return executeContract(contractId, contractArgument, null, null);
    }

    /**
     * Execute contract with function
     */
    public Map<String, Object> executeContract(String contractId, String contractArgument,
                                               String functionId, String functionArgument)
            throws ClientException, IOException {

        ClientServiceFactory factory = new ClientServiceFactory();
        try {
            ClientService service = factory.create(createClientConfig());

            JsonNode jsonContractArgument = serde.deserialize(contractArgument);
            JsonNode jsonFunctionArgument = null;
            if (functionArgument != null && !functionArgument.isEmpty()) {
                jsonFunctionArgument = serde.deserialize(functionArgument);
            }

            ContractExecutionResult result = service.executeContract(
                    contractId, jsonContractArgument, functionId, jsonFunctionArgument);

            Map<String, Object> response = new HashMap<>();
            result.getContractResult().ifPresent(r ->
                response.put("contractResult", serde.deserialize(r)));
            result.getFunctionResult().ifPresent(r ->
                response.put("functionResult", serde.deserialize(r)));
            response.put("ledgerProofs", result.getLedgerProofs());
            response.put("auditorProofs", result.getAuditorProofs());

            log.info("Contract executed: {}", contractId);
            return response;
        } finally {
            factory.close();
        }
    }

    /**
     * Validate ledger
     */
    public Map<String, Object> validateLedger(String assetId, Integer startAge, Integer endAge)
            throws ClientException, IOException {

        ClientServiceFactory factory = new ClientServiceFactory();
        try {
            ClientService service = factory.create(createClientConfig());

            LedgerValidationResult result;
            if (startAge != null && endAge != null) {
                result = service.validateLedger(assetId, startAge, endAge);
            } else {
                result = service.validateLedger(assetId);
            }

            ObjectNode json = objectMapper.createObjectNode()
                    .put("statusCode", result.getCode().toString());
            json.set("ledgerProof", getProofJson(result.getLedgerProof().orElse(null)));
            json.set("auditorProof", getProofJson(result.getAuditorProof().orElse(null)));

            Map<String, Object> response = new HashMap<>();
            response.put("statusCode", result.getCode().toString());
            response.put("ledgerProof", result.getLedgerProof().orElse(null));
            response.put("auditorProof", result.getAuditorProof().orElse(null));

            log.info("Ledger validated for asset: {}", assetId);
            return response;
        } finally {
            factory.close();
        }
    }

    /**
     * Convert AssetProof to JSON
     */
    private JsonNode getProofJson(@Nullable AssetProof proof) {
        if (proof == null) {
            return null;
        }
        return objectMapper.createObjectNode()
                .put("id", proof.getId())
                .put("age", proof.getAge())
                .put("nonce", proof.getNonce())
                .put("hash", Base64.getEncoder().encodeToString(proof.getHash()))
                .put("signature", Base64.getEncoder().encodeToString(proof.getSignature()));
    }
}
