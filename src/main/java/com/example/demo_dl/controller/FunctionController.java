package com.example.demo_dl.controller;

import com.example.demo_dl.dto.ApiResponse;
import com.example.demo_dl.dto.FunctionRegisterRequest;
import com.example.demo_dl.service.ScalarDLService;
import com.scalar.dl.client.exception.ClientException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/api/functions")
@RequiredArgsConstructor
public class FunctionController {

    private final ScalarDLService scalarDLService;

    /**
     * Register function with file upload
     * POST /api/functions/register
     *
     * @param file function .class file
     * @param request function registration details
     */
    @PostMapping("/register")
    public ApiResponse<String> registerFunction(
            @RequestPart("file") MultipartFile file,
            @RequestPart("request") FunctionRegisterRequest request) {
        try {
            byte[] functionBytes = file.getBytes();
            scalarDLService.registerFunction(
                    request.getFunctionId(),
                    request.getFunctionBinaryName(),
                    functionBytes
            );
            return ApiResponse.success("Function registered successfully: " + request.getFunctionId(), "OK");
        } catch (ClientException | IOException e) {
            log.error("Failed to register function", e);
            return ApiResponse.error("Failed to register function: " + e.getMessage());
        }
    }
}
