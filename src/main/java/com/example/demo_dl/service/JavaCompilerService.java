package com.example.demo_dl.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.tools.*;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
public class JavaCompilerService {

    private static final String BASE_DIR = System.getProperty("user.dir");
    private static final String GENERATED_DIR = BASE_DIR + "/generated";
    private static final String COMPILED_DIR = BASE_DIR + "/compiled";

    /**
     * Compile a contract Java file
     * @param versionedClassName Versioned class name (e.g., "UserUpdaterV1_0_0")
     */
    public CompilationResult compileContract(String versionedClassName) throws IOException {
        String sourceFile = GENERATED_DIR + "/contracts/" + versionedClassName + ".java";
        String outputDir = COMPILED_DIR + "/contracts";
        return compile(sourceFile, outputDir);
    }

    /**
     * Compile a function Java file
     * @param versionedClassName Versioned class name (e.g., "PriceCalculatorV1_0_0")
     */
    public CompilationResult compileFunction(String versionedClassName) throws IOException {
        String sourceFile = GENERATED_DIR + "/functions/" + versionedClassName + ".java";
        String outputDir = COMPILED_DIR + "/functions";
        return compile(sourceFile, outputDir);
    }

    /**
     * Generic compilation method
     */
    private CompilationResult compile(String sourceFilePath, String outputDir) throws IOException {
        File sourceFile = new File(sourceFilePath);
        if (!sourceFile.exists()) {
            return CompilationResult.failure("Source file not found: " + sourceFilePath);
        }

        // Create output directory
        Files.createDirectories(Paths.get(outputDir));

        // Get Java compiler
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            return CompilationResult.failure(
                "Java compiler not available. Make sure you're running with JDK (not JRE)");
        }

        // Setup diagnostic collector for error messages
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);

        // Get source file
        Iterable<? extends JavaFileObject> compilationUnits =
            fileManager.getJavaFileObjectsFromFiles(Arrays.asList(sourceFile));

        // Setup compilation options
        // Use --release 8 for true Java 8 bytecode compatibility (class version 52.0)
        // This ensures contracts work with ScalarDL Java 8 runtime
        List<String> options = Arrays.asList(
            "-d", outputDir,                    // Output directory
            "-cp", getClasspath(),              // Classpath with dependencies
            "--release", "8"                    // Target Java 8 (replaces -source/-target)
        );

        // Compile
        StringWriter output = new StringWriter();
        JavaCompiler.CompilationTask task = compiler.getTask(
            output,
            fileManager,
            diagnostics,
            options,
            null,
            compilationUnits
        );

        boolean success = task.call();
        fileManager.close();

        if (success) {
            log.info("Compilation successful: {}", sourceFile.getName());
            String className = extractClassName(sourceFilePath);
            String classFilePath = outputDir + "/" + className.replace(".", "/") + ".class";
            return CompilationResult.success(classFilePath);
        } else {
            StringBuilder errors = new StringBuilder();
            for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                errors.append(String.format("Line %d: %s%n",
                    diagnostic.getLineNumber(),
                    diagnostic.getMessage(null)));
            }
            log.error("Compilation failed: {}", errors);
            return CompilationResult.failure(errors.toString());
        }
    }

    /**
     * Get classpath with ScalarDL dependencies
     * Uses the current runtime classpath which already has all dependencies
     */
    private String getClasspath() {
        StringBuilder classpath = new StringBuilder();

        // Add output directory for compiled classes
        classpath.append(BASE_DIR).append("/build/classes/java/main");
        classpath.append(File.pathSeparator);

        // Add the current runtime classpath (includes all dependencies)
        String runtimeClasspath = System.getProperty("java.class.path");
        if (runtimeClasspath != null && !runtimeClasspath.isEmpty()) {
            classpath.append(runtimeClasspath);
        }

        log.debug("Compiler classpath: {}", classpath.toString());
        return classpath.toString();
    }

    /**
     * Extract fully qualified class name from Java source file
     */
    private String extractClassName(String sourceFilePath) throws IOException {
        String content = Files.readString(Paths.get(sourceFilePath));

        // Extract package name
        String packageName = "";
        if (content.contains("package ")) {
            int start = content.indexOf("package ") + 8;
            int end = content.indexOf(";", start);
            packageName = content.substring(start, end).trim();
        }

        // Extract class name from filename
        String fileName = new File(sourceFilePath).getName();
        String className = fileName.replace(".java", "");

        return packageName.isEmpty() ? className : packageName + "." + className;
    }

    /**
     * Compilation result
     */
    public static class CompilationResult {
        private final boolean success;
        private final String message;
        private final String classFilePath;

        private CompilationResult(boolean success, String message, String classFilePath) {
            this.success = success;
            this.message = message;
            this.classFilePath = classFilePath;
        }

        public static CompilationResult success(String classFilePath) {
            return new CompilationResult(true, "Compilation successful", classFilePath);
        }

        public static CompilationResult failure(String error) {
            return new CompilationResult(false, error, null);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public String getClassFilePath() {
            return classFilePath;
        }
    }
}
