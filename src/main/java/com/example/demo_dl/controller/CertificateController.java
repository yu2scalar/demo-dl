package com.example.demo_dl.controller;

import com.example.demo_dl.dto.ApiResponse;
import com.example.demo_dl.service.ScalarDLService;
import com.scalar.dl.client.exception.ClientException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/api/certificates")
@RequiredArgsConstructor
public class CertificateController {

    private final ScalarDLService scalarDLService;

    /**
     * Register client certificate
     * POST /api/certificates/register
     */
    @PostMapping("/register")
    public ApiResponse<String> registerCertificate() {
        try {
            scalarDLService.registerCertificate();
            return ApiResponse.success("Certificate registered successfully", "OK");
        } catch (ClientException | IOException e) {
            log.error("Failed to register certificate", e);
            return ApiResponse.error("Failed to register certificate: " + e.getMessage());
        }
    }
}
