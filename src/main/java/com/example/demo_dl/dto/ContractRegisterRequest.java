package com.example.demo_dl.dto;

import lombok.Data;

@Data
public class ContractRegisterRequest {
    private String contractId;
    private String contractBinaryName;
    private String contractProperties;
}
