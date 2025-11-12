package com.example.demo_dl.dto;

import lombok.Data;
import java.util.List;

@Data
public class ContractDefinition {
    private String name;
    private ContractType contractType;
    private String version;
    private String packageName;
    private String assetIdField;
    private List<FieldDefinition> fields;
    private String description;

    public enum ContractType {
        READER,   // Read-only: uses ledger.get()
        UPDATER   // Read + Write: uses ledger.get() + ledger.put()
    }
}
