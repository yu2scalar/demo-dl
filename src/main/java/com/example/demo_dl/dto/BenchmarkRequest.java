package com.example.demo_dl.dto;

import lombok.Data;

@Data
public class BenchmarkRequest {
    private String contractId;
    private String baseArgument;
    private Integer requestCount = 1000;
    private Integer validationRatio = 10; // 0: none, 1: always, N: once per N requests
}
