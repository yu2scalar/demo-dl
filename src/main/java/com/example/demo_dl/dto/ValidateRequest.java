package com.example.demo_dl.dto;

import lombok.Data;

@Data
public class ValidateRequest {
    private String assetId;
    private Integer startAge;
    private Integer endAge;
}
