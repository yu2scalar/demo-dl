package com.example.demo_dl.controller;

import com.example.demo_dl.dto.ApiResponse;
import com.example.demo_dl.dto.FunctionRegisterRequest;
import com.example.demo_dl.service.ScalarDLService;
import com.scalar.dl.client.exception.ClientException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
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
    @Operation(summary = "Register a function", description = "Register a ScalarDL function by uploading the compiled .class file")
    @PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<String> registerFunction(
            @Parameter(description = "Function .class file", required = true,
                    content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE))
            @RequestPart("file") MultipartFile file,
            @Parameter(description = "Function registration details", required = true,
                    schema = @Schema(implementation = FunctionRegisterRequest.class))
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

    /**
     * Register function from compiled class file
     * POST /api/functions/register-compiled
     *
     * @param versionedClassName The versioned class name (e.g., "UserUpdaterFunctionV1_0_0")
     * @param packageName The package name (e.g., "com.example.functions")
     */
    @Operation(summary = "Register a compiled function", description = "Register a ScalarDL function from the compiled/ directory")
    @PostMapping("/register-compiled")
    public ApiResponse<String> registerCompiledFunction(
            @Parameter(description = "Versioned class name", required = true, example = "UserUpdaterFunctionV1_0_0")
            @org.springframework.web.bind.annotation.RequestParam String versionedClassName,
            @Parameter(description = "Package name", required = true, example = "com.example.functions")
            @org.springframework.web.bind.annotation.RequestParam String packageName) {
        try {
            // Build path to compiled class file
            String classFilePath = System.getProperty("user.dir") + "/compiled/functions/"
                + packageName.replace(".", "/") + "/" + versionedClassName + ".class";

            // Read class file
            byte[] functionBytes = java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(classFilePath));

            // Register with ScalarDL
            String functionBinaryName = packageName + "." + versionedClassName;
            scalarDLService.registerFunction(
                    versionedClassName,  // functionId
                    functionBinaryName,  // functionBinaryName
                    functionBytes
            );

            return ApiResponse.success(
                "Function registered successfully: " + versionedClassName,
                functionBinaryName
            );
        } catch (ClientException | IOException e) {
            log.error("Failed to register compiled function", e);
            return ApiResponse.error("Failed to register function: " + e.getMessage());
        }
    }
}
