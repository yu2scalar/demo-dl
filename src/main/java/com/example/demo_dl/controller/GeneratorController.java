package com.example.demo_dl.controller;

import com.example.demo_dl.dto.ApiResponse;
import com.example.demo_dl.dto.ContractDefinition;
import com.example.demo_dl.dto.FunctionDefinition;
import com.example.demo_dl.service.CodeManagementService;
import com.example.demo_dl.service.ContractGenerator;
import com.example.demo_dl.service.FunctionGenerator;
import com.example.demo_dl.service.JavaCompilerService;
import com.example.demo_dl.util.VersionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/generator")
@RequiredArgsConstructor
public class GeneratorController {

    private final ContractGenerator contractGenerator;
    private final FunctionGenerator functionGenerator;
    private final CodeManagementService codeManagementService;
    private final JavaCompilerService javaCompilerService;

    /**
     * Generate a new contract
     * POST /api/generator/contracts?compile=true
     */
    @PostMapping("/contracts")
    public ApiResponse<Map<String, String>> generateContract(
            @RequestBody ContractDefinition definition,
            @RequestParam(defaultValue = "false") boolean compile) {
        try {
            // Generate versioned name
            String versionedName = VersionUtil.getVersionedName(
                definition.getName(), definition.getVersion());

            // Generate contract code
            String generatedCode = contractGenerator.generateContract(definition);

            // Save definition and generated code
            codeManagementService.saveContract(definition, generatedCode);

            Map<String, String> result = new HashMap<>();
            result.put("baseName", definition.getName());
            result.put("version", definition.getVersion());
            result.put("versionedName", versionedName);
            result.put("type", definition.getContractType().toString());
            result.put("definitionPath", "definitions/contracts/" + versionedName + ".json");
            result.put("codePath", "generated/contracts/" + versionedName + ".java");

            // Optionally compile
            if (compile) {
                JavaCompilerService.CompilationResult compilationResult =
                    javaCompilerService.compileContract(versionedName);

                if (compilationResult.isSuccess()) {
                    result.put("compiled", "true");
                    result.put("classFilePath", compilationResult.getClassFilePath());
                    return ApiResponse.success(
                        "Contract generated and compiled successfully: " + versionedName,
                        result
                    );
                } else {
                    result.put("compiled", "false");
                    result.put("compilationError", compilationResult.getMessage());
                    return ApiResponse.success(
                        "Contract generated but compilation failed: " + versionedName,
                        result
                    );
                }
            }

            return ApiResponse.success(
                "Contract generated successfully: " + versionedName,
                result
            );
        } catch (IOException e) {
            log.error("Failed to generate contract", e);
            return ApiResponse.error("Failed to generate contract: " + e.getMessage());
        }
    }

    /**
     * Generate a new function
     * POST /api/generator/functions?compile=true
     */
    @PostMapping("/functions")
    public ApiResponse<Map<String, String>> generateFunction(
            @RequestBody FunctionDefinition definition,
            @RequestParam(defaultValue = "false") boolean compile) {
        try {
            // Generate versioned name
            String versionedName = VersionUtil.getVersionedName(
                definition.getName(), definition.getVersion());

            // Generate function code
            String generatedCode = functionGenerator.generateFunction(definition);

            // Save definition and generated code
            codeManagementService.saveFunction(definition, generatedCode);

            Map<String, String> result = new HashMap<>();
            result.put("baseName", definition.getName());
            result.put("version", definition.getVersion());
            result.put("versionedName", versionedName);
            result.put("definitionPath", "definitions/functions/" + versionedName + ".json");
            result.put("codePath", "generated/functions/" + versionedName + ".java");

            // Optionally compile
            if (compile) {
                JavaCompilerService.CompilationResult compilationResult =
                    javaCompilerService.compileFunction(versionedName);

                if (compilationResult.isSuccess()) {
                    result.put("compiled", "true");
                    result.put("classFilePath", compilationResult.getClassFilePath());
                    return ApiResponse.success(
                        "Function generated and compiled successfully: " + versionedName,
                        result
                    );
                } else {
                    result.put("compiled", "false");
                    result.put("compilationError", compilationResult.getMessage());
                    return ApiResponse.success(
                        "Function generated but compilation failed: " + versionedName,
                        result
                    );
                }
            }

            return ApiResponse.success(
                "Function generated successfully: " + versionedName,
                result
            );
        } catch (IOException e) {
            log.error("Failed to generate function", e);
            return ApiResponse.error("Failed to generate function: " + e.getMessage());
        }
    }

    /**
     * List all contract definitions
     * GET /api/generator/contracts
     */
    @GetMapping("/contracts")
    public ApiResponse<List<String>> listContracts() {
        try {
            List<String> contracts = codeManagementService.listContracts();
            return ApiResponse.success("Found " + contracts.size() + " contracts", contracts);
        } catch (Exception e) {
            log.error("Failed to list contracts", e);
            return ApiResponse.error("Failed to list contracts: " + e.getMessage());
        }
    }

    /**
     * List all function definitions
     * GET /api/generator/functions
     */
    @GetMapping("/functions")
    public ApiResponse<List<String>> listFunctions() {
        try {
            List<String> functions = codeManagementService.listFunctions();
            return ApiResponse.success("Found " + functions.size() + " functions", functions);
        } catch (Exception e) {
            log.error("Failed to list functions", e);
            return ApiResponse.error("Failed to list functions: " + e.getMessage());
        }
    }

    /**
     * Get contract definition
     * GET /api/generator/contracts/{name}
     */
    @GetMapping("/contracts/{name}")
    public ApiResponse<ContractDefinition> getContract(@PathVariable String name) {
        try {
            ContractDefinition definition = codeManagementService.loadContractDefinition(name);
            return ApiResponse.success("Contract definition retrieved", definition);
        } catch (IOException e) {
            log.error("Failed to load contract definition", e);
            return ApiResponse.error("Contract not found: " + name);
        }
    }

    /**
     * Get function definition
     * GET /api/generator/functions/{name}
     */
    @GetMapping("/functions/{name}")
    public ApiResponse<FunctionDefinition> getFunction(@PathVariable String name) {
        try {
            FunctionDefinition definition = codeManagementService.loadFunctionDefinition(name);
            return ApiResponse.success("Function definition retrieved", definition);
        } catch (IOException e) {
            log.error("Failed to load function definition", e);
            return ApiResponse.error("Function not found: " + name);
        }
    }

    /**
     * Get generated contract code
     * GET /api/generator/contracts/{name}/code
     */
    @GetMapping("/contracts/{name}/code")
    public ApiResponse<String> getContractCode(@PathVariable String name) {
        try {
            String code = codeManagementService.getGeneratedContractCode(name);
            return ApiResponse.success("Generated code retrieved", code);
        } catch (IOException e) {
            log.error("Failed to load contract code", e);
            return ApiResponse.error("Generated code not found: " + name);
        }
    }

    /**
     * Get generated function code
     * GET /api/generator/functions/{name}/code
     */
    @GetMapping("/functions/{name}/code")
    public ApiResponse<String> getFunctionCode(@PathVariable String name) {
        try {
            String code = codeManagementService.getGeneratedFunctionCode(name);
            return ApiResponse.success("Generated code retrieved", code);
        } catch (IOException e) {
            log.error("Failed to load function code", e);
            return ApiResponse.error("Generated code not found: " + name);
        }
    }

    /**
     * Get execute request template for a contract
     * GET /api/generator/contracts/{name}/template
     */
    @GetMapping("/contracts/{name}/template")
    public ApiResponse<Map<String, String>> getContractTemplate(@PathVariable String name) {
        try {
            Map<String, String> template = codeManagementService.generateContractExecuteTemplate(name);
            return ApiResponse.success("Execute template generated", template);
        } catch (IOException e) {
            log.error("Failed to generate contract template", e);
            return ApiResponse.error("Failed to generate template for contract: " + name);
        }
    }

    /**
     * Delete contract
     * DELETE /api/generator/contracts/{name}
     */
    @DeleteMapping("/contracts/{name}")
    public ApiResponse<String> deleteContract(@PathVariable String name) {
        try {
            codeManagementService.deleteContract(name);
            return ApiResponse.success("Contract deleted successfully", name);
        } catch (IOException e) {
            log.error("Failed to delete contract", e);
            return ApiResponse.error("Failed to delete contract: " + e.getMessage());
        }
    }

    /**
     * Delete function
     * DELETE /api/generator/functions/{name}
     */
    @DeleteMapping("/functions/{name}")
    public ApiResponse<String> deleteFunction(@PathVariable String name) {
        try {
            codeManagementService.deleteFunction(name);
            return ApiResponse.success("Function deleted successfully", name);
        } catch (IOException e) {
            log.error("Failed to delete function", e);
            return ApiResponse.error("Failed to delete function: " + e.getMessage());
        }
    }

    /**
     * Compile an existing contract
     * POST /api/generator/contracts/{name}/compile
     */
    @PostMapping("/contracts/{name}/compile")
    public ApiResponse<Map<String, String>> compileContract(@PathVariable String name) {
        try {
            JavaCompilerService.CompilationResult result = javaCompilerService.compileContract(name);

            Map<String, String> response = new HashMap<>();
            response.put("name", name);
            response.put("compiled", String.valueOf(result.isSuccess()));

            if (result.isSuccess()) {
                response.put("classFilePath", result.getClassFilePath());
                return ApiResponse.success("Contract compiled successfully: " + name, response);
            } else {
                response.put("error", result.getMessage());
                return ApiResponse.error("Compilation failed: " + result.getMessage());
            }
        } catch (IOException e) {
            log.error("Failed to compile contract", e);
            return ApiResponse.error("Failed to compile contract: " + e.getMessage());
        }
    }

    /**
     * Compile an existing function
     * POST /api/generator/functions/{name}/compile
     */
    @PostMapping("/functions/{name}/compile")
    public ApiResponse<Map<String, String>> compileFunction(@PathVariable String name) {
        try {
            JavaCompilerService.CompilationResult result = javaCompilerService.compileFunction(name);

            Map<String, String> response = new HashMap<>();
            response.put("name", name);
            response.put("compiled", String.valueOf(result.isSuccess()));

            if (result.isSuccess()) {
                response.put("classFilePath", result.getClassFilePath());
                return ApiResponse.success("Function compiled successfully: " + name, response);
            } else {
                response.put("error", result.getMessage());
                return ApiResponse.error("Compilation failed: " + result.getMessage());
            }
        } catch (IOException e) {
            log.error("Failed to compile function", e);
            return ApiResponse.error("Failed to compile function: " + e.getMessage());
        }
    }
}
