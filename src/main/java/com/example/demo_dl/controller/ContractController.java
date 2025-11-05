package com.example.demo_dl.controller;

import com.example.demo_dl.dto.ApiResponse;
import com.example.demo_dl.dto.ContractExecuteRequest;
import com.example.demo_dl.dto.ContractRegisterRequest;
import com.example.demo_dl.dto.ValidateRequest;
import com.example.demo_dl.service.ScalarDLService;
import com.scalar.dl.client.exception.ClientException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/contracts")
@RequiredArgsConstructor
public class ContractController {

    private final ScalarDLService scalarDLService;

    /**
     * Register contract with file upload
     * POST /api/contracts/register
     *
     * @param file contract .class file
     * @param request contract registration details
     */
    @PostMapping("/register")
    public ApiResponse<String> registerContract(
            @RequestPart("file") MultipartFile file,
            @RequestPart("request") ContractRegisterRequest request) {
        try {
            byte[] contractBytes = file.getBytes();
            scalarDLService.registerContract(
                    request.getContractId(),
                    request.getContractBinaryName(),
                    contractBytes,
                    request.getContractProperties()
            );
            return ApiResponse.success("Contract registered successfully: " + request.getContractId(), "OK");
        } catch (ClientException | IOException e) {
            log.error("Failed to register contract", e);
            return ApiResponse.error("Failed to register contract: " + e.getMessage());
        }
    }

    /**
     * Execute contract
     * POST /api/contracts/execute
     *
     * @param request contract execution request
     */
    @PostMapping("/execute")
    public ApiResponse<Map<String, Object>> executeContract(@RequestBody ContractExecuteRequest request) {
        try {
            Map<String, Object> result = scalarDLService.executeContract(
                    request.getContractId(),
                    request.getContractArgument()
            );
            return ApiResponse.success("Contract executed successfully", result);
        } catch (ClientException | IOException e) {
            log.error("Failed to execute contract", e);
            return ApiResponse.error("Failed to execute contract: " + e.getMessage());
        }
    }

    /**
     * Execute contract with function
     * POST /api/contracts/execute-with-function
     *
     * @param request contract execution request with function
     */
    @PostMapping("/execute-with-function")
    public ApiResponse<Map<String, Object>> executeContractWithFunction(@RequestBody ContractExecuteRequest request) {
        try {
            Map<String, Object> result = scalarDLService.executeContract(
                    request.getContractId(),
                    request.getContractArgument(),
                    request.getFunctionId(),
                    request.getFunctionArgument()
            );
            return ApiResponse.success("Contract executed with function successfully", result);
        } catch (ClientException | IOException e) {
            log.error("Failed to execute contract with function", e);
            return ApiResponse.error("Failed to execute contract with function: " + e.getMessage());
        }
    }

    /**
     * Validate ledger
     * POST /api/contracts/validate
     *
     * @param request validation request
     */
    @PostMapping("/validate")
    public ApiResponse<Map<String, Object>> validateLedger(@RequestBody ValidateRequest request) {
        try {
            Map<String, Object> result = scalarDLService.validateLedger(
                    request.getAssetId(),
                    request.getStartAge(),
                    request.getEndAge()
            );
            return ApiResponse.success("Ledger validated successfully", result);
        } catch (ClientException | IOException e) {
            log.error("Failed to validate ledger", e);
            return ApiResponse.error("Failed to validate ledger: " + e.getMessage());
        }
    }
}
