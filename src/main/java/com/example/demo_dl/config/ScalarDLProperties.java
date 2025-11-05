package com.example.demo_dl.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "scalardl")
public class ScalarDLProperties {

    private String serverHost = "localhost";
    private Integer serverPort = 50051;
    private Integer serverPrivilegedPort = 50052;

    private String certHolderId = "client";
    private Integer certVersion = 1;
    private String certPath;
    private String certPem;
    private String privateKeyPath;
    private String privateKeyPem;

    private Boolean tlsEnabled = false;
    private String tlsCaRootCertPath;
    private String tlsCaRootCertPem;

    private String authorizationCredential;

    private Auditor auditor = new Auditor();

    @Data
    public static class Auditor {
        private Boolean enabled = false;
        private String host = "localhost";
        private Integer port = 40051;
        private Integer privilegedPort = 40052;
        private Boolean tlsEnabled = false;
        private String tlsCaRootCertPath;
        private String tlsCaRootCertPem;
        private String authorizationCredential;
        private String linearizableValidationContractId = "validate-ledger";
    }
}
