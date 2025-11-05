package com.example.demo_dl.dto;

import lombok.Data;

@Data
public class ContractExecuteRequest {
    private String contractId;
    private String contractArgument;
    private String functionId;
    private String functionArgument;
}
