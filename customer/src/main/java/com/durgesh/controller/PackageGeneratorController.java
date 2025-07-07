package com.durgesh.controller;

import com.durgesh.service.DynamicPackageGeneratorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/generator")
@CrossOrigin(origins = "*")
public class PackageGeneratorController {

    @Autowired
    private DynamicPackageGeneratorService packageGeneratorService;

    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generatePackageAndClasses(@RequestBody String jsonData) {
        try {
            Map<String, Object> result = packageGeneratorService.generatePackageAndClasses(jsonData);

            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
            }

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Error generating package and classes: " + e.getMessage());
            error.put("error", e.getClass().getSimpleName());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getGeneratorInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("service", "Dynamic Package Generator");
        info.put("description", "Generates Java packages and classes dynamically based on JSON input");
        info.put("version", "1.0.0");

        Map<String, String> features = new HashMap<>();
        features.put("dynamic-packages", "Creates packages based on PaymentProcessorName");
        features.put("main-class", "Generates PaymentProcessorName + Type class");
        features.put("request-body-classes", "Creates RequestBody classes for each API");
        features.put("api-classes", "Generates individual API classes");
        features.put("json-annotations", "Includes Jackson annotations for JSON mapping");

        info.put("features", features);

        Map<String, String> endpoints = new HashMap<>();
        endpoints.put("POST /api/generator/generate", "Generate package and classes from JSON");
        endpoints.put("GET /api/generator/info", "Get generator information");
        endpoints.put("GET /api/generator/help", "Get usage help");

        info.put("endpoints", endpoints);

        return ResponseEntity.ok(info);
    }

    @GetMapping("/help")
    public ResponseEntity<Map<String, Object>> getHelp() {
        Map<String, Object> help = new HashMap<>();
        help.put("service", "Dynamic Package Generator");
        help.put("description", "This service generates Java packages and classes dynamically");

        Map<String, String> usage = new HashMap<>();
        usage.put("endpoint", "POST /api/generator/generate");
        usage.put("content-type", "application/json");
        usage.put("body", "JSON string containing type, PaymentProcessorName, and data array");

        help.put("usage", usage);

        Map<String, String> generated = new HashMap<>();
        generated.put("package", "Created with PaymentProcessorName (lowercase)");
        generated.put("main-class", "PaymentProcessorName + Type (e.g., OnvoPayPayIn)");
        generated.put("request-body-classes", "One for each unique API (e.g., CreateCustomerRequestBody)");
        generated.put("api-classes", "Individual API classes (e.g., CreateCustomerApi)");
        generated.put("location", "src/main/java/com/durgesh/generated/[package-name]/");

        help.put("generated", generated);

        Map<String, String> example = new HashMap<>();
        example.put("input", "JSON with PaymentProcessorName: 'OnvoPay', type: 'PayIn'");
        example.put("package", "com.durgesh.generated.onvopay");
        example.put("main-class", "OnvoPayPayIn.java");
        example.put("classes", "CreateCustomerRequestBody.java, CreatePaymentMethodRequestBody.java, etc.");

        help.put("example", example);

        return ResponseEntity.ok(help);
    }
}