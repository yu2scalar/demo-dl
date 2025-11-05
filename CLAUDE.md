# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Spring Boot 3.5.7 web application using Java 17 and Gradle build system.

**Package naming**: The original package name 'com.example.demo-dl' is invalid, so this project uses 'com.example.demo_dl' instead.

## Build System

This project uses Gradle with the Gradle Wrapper (`gradlew`).

### Common Commands

```bash
# Build the project
./gradlew build

# Run the application
./gradlew bootRun

# Run all tests
./gradlew test

# Run a single test class
./gradlew test --tests com.example.demo_dl.DemoDlApplicationTests

# Run a specific test method
./gradlew test --tests com.example.demo_dl.ClassName.methodName

# Clean build artifacts
./gradlew clean

# Build without running tests
./gradlew build -x test
```

## Architecture

### Technology Stack
- **Spring Boot**: 3.5.7
- **Java**: 17 (toolchain configured)
- **Web Framework**: Spring Web (REST/MVC)
- **Lombok**: Enabled for reducing boilerplate code
- **Testing**: JUnit 5 (Jupiter) with Spring Boot Test

### Project Structure
```
src/main/java/com/example/demo_dl/    - Main application code
src/main/resources/                    - Configuration files (application.properties)
src/test/java/com/example/demo_dl/    - Test classes
```

### Key Configuration
- Main application class: `DemoDlApplication.java`
- Application name: `demo-dl` (configured in application.properties)
- Tests use `@SpringBootTest` annotation and JUnit Platform

## Development Notes

When adding new REST controllers or services, place them in the `com.example.demo_dl` package or sub-packages to ensure they are component-scanned by Spring Boot's `@SpringBootApplication` annotation.
