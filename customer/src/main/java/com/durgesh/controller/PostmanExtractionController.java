package com.durgesh.controller;
import com.durgesh.entity.ApiInfo;
import com.durgesh.service.PostmanCollectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class PostmanExtractionController {

    @Autowired
    private PostmanCollectionService postmanService;

    @GetMapping("/")
    public ResponseEntity<Map<String, String>> welcome() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "🚀 Postman API Extractor Service");
        response.put("version", "1.0.0");
        response.put("status", "running");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/help")
    public ResponseEntity<Map<String, Object>> help() {
        Map<String, Object> help = new HashMap<>();
        help.put("service", "Postman API Extractor");
        help.put("description", "Extract API information from Postman collections");

        Map<String, String> endpoints = new HashMap<>();
        endpoints.put("GET /api/", "Service information");
        endpoints.put("GET /api/help", "This help page");
        endpoints.put("POST /api/extract", "Extract APIs from Postman collection file");
        endpoints.put("GET /api/health", "Health check");

        help.put("endpoints", endpoints);
        help.put("usage", "Upload your Postman collection JSON file to extract API information");

        return ResponseEntity.ok(help);
    }

    @GetMapping("/extract")
    public ResponseEntity<Map<String, Object>> extractApis(/*@RequestParam("filePath") String filePath*/) {
        try {
            String collectionFilePath = "C:\\Users\\Hii\\OneDrive\\Desktop\\newtask\\X1_Collection.postman_collection.json";
            List<ApiInfo> apis = postmanService.extractApiNames(collectionFilePath);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "APIs extracted successfully");
            response.put("totalCount", apis.size());
            response.put("apis", apis);

            // Create formatted string for backward compatibility
            StringBuilder apiNames = new StringBuilder("API Names: ");
            for (int i = 0; i < apis.size(); i++) {
                if (i > 0) apiNames.append(" ");
                apiNames.append((i + 1)).append(". ").append(apis.get(i).getName());
            }
            response.put("formattedNames", apiNames.toString());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Error extracting APIs: " + e.getMessage());
            error.put("error", e.getClass().getSimpleName());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "Postman API Extractor");
        health.put("timestamp", java.time.Instant.now().toString());

        return ResponseEntity.ok(health);
    }
}