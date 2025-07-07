package com.durgesh.service;

import com.durgesh.entity.ApiInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PostmanCollectionService {

    private final ObjectMapper objectMapper;

    public PostmanCollectionService() {
        this.objectMapper = new ObjectMapper();
    }

    public List<ApiInfo> extractApiNames(String filePath) throws IOException {
        JsonNode root;

        // Check if it's a URL or file path
        if (filePath.startsWith("http://") || filePath.startsWith("https://")) {
            root = objectMapper.readTree(new URL(filePath));
        } else {
            File file = new File(filePath);
            if (!file.exists()) {
                throw new FileNotFoundException("File not found: " + filePath);
            }
            root = objectMapper.readTree(file);
        }

        List<ApiInfo> apis = new ArrayList<>();
        String collectionName = root.path("info").path("name").asText("Unknown Collection");

        // Process items (can be nested)
        JsonNode items = root.path("item");
        if (items.isArray()) {
            processItems(items, apis, "");
        }

        return apis;
    }

    private void processItems(JsonNode items, List<ApiInfo> apis, String parentFolder) {
        for (JsonNode item : items) {
            String itemName = item.path("name").asText("Unnamed Item");
            String currentFolder = parentFolder.isEmpty() ? itemName : parentFolder + "/" + itemName;

            // Check if this item has a request (it's an API endpoint)
            if (item.has("request")) {
                JsonNode request = item.path("request");
                String method = request.path("method").asText("GET");
                String url = extractUrl(request.path("url"));

                // Extract request body
                String requestBody = extractRequestBody(request);

                // Extract headers
                Map<String, String> headers = extractHeaders(request);

                // Extract query parameters
                Map<String, String> queryParams = extractQueryParams(request.path("url"));

                // Extract base URL and endpoint
                String baseUrl = extractBaseUrl(url);
                String endpoint = extractEndpoint(url);

                // Set priority based on method or other logic
                Integer priority = determinePriority(method, url);

                ApiInfo apiInfo = new ApiInfo(itemName, method, url, parentFolder,
                        requestBody, headers, queryParams,
                        baseUrl, endpoint, priority);
                apis.add(apiInfo);
            } else if (item.has("item")) {
                // This is a folder, process its items recursively
                processItems(item.path("item"), apis, currentFolder);
            }
        }
    }

    private String extractBaseUrl(String fullUrl) {
        if (fullUrl == null || fullUrl.isEmpty()) {
            return "https://api.onvopay.com"; // Default base URL
        }

        try {
            URL url = new URL(fullUrl);
            return url.getProtocol() + "://" + url.getHost() +
                    (url.getPort() != -1 ? ":" + url.getPort() : "");
        } catch (Exception e) {
            // If URL parsing fails, return default
            return "https://api.onvopay.com";
        }
    }

    private String extractEndpoint(String fullUrl) {
        if (fullUrl == null || fullUrl.isEmpty()) {
            return "/v1/customers"; // Default endpoint
        }

        try {
            URL url = new URL(fullUrl);
            String path = url.getPath();
            return path.isEmpty() ? "/v1/customers" : path;
        } catch (Exception e) {
            // If URL parsing fails, return default
            return "/v1/customers";
        }
    }

    private Integer determinePriority(String method, String url) {
        // Default priority
        Integer priority = 3;

        // Custom logic for determining priority
        if (method != null) {
            switch (method.toUpperCase()) {
                case "GET":
                    priority = 1;
                    break;
                case "POST":
                    priority = 2;
                    break;
                case "PUT":
                case "PATCH":
                    priority = 3;
                    break;
                case "DELETE":
                    priority = 4;
                    break;
                default:
                    priority = 3;
            }
        }

        // You can add more logic based on URL patterns
        if (url != null && url.contains("customers")) {
            priority = 3; // As specified in your requirement
        }

        return priority;
    }

    private String extractRequestBody(JsonNode request) {
        JsonNode body = request.path("body");
        if (body.isMissingNode()) {
            return null;
        }

        String mode = body.path("mode").asText("");

        switch (mode) {
            case "raw":
                return body.path("raw").asText();
            case "urlencoded":
                return extractFormData(body.path("urlencoded"));
            case "formdata":
                return extractFormData(body.path("formdata"));
            case "file":
                return "File upload: " + body.path("file").path("src").asText();
            case "graphql":
                JsonNode graphql = body.path("graphql");
                return "GraphQL Query: " + graphql.path("query").asText() +
                        (graphql.has("variables") ? "\nVariables: " + graphql.path("variables").asText() : "");
            default:
                return body.toString();
        }
    }

    private String extractFormData(JsonNode formData) {
        if (!formData.isArray()) {
            return null;
        }

        StringBuilder formBuilder = new StringBuilder();
        for (JsonNode field : formData) {
            if (formBuilder.length() > 0) {
                formBuilder.append("&");
            }
            String key = field.path("key").asText();
            String value = field.path("value").asText();
            String type = field.path("type").asText("text");

            if ("file".equals(type)) {
                formBuilder.append(key).append("=").append("[FILE: ").append(value).append("]");
            } else {
                formBuilder.append(key).append("=").append(value);
            }
        }
        return formBuilder.toString();
    }

    private Map<String, String> extractHeaders(JsonNode request) {
        Map<String, String> headers = new HashMap<>();
        JsonNode headerNode = request.path("header");

        if (headerNode.isArray()) {
            for (JsonNode header : headerNode) {
                String key = header.path("key").asText();
                String value = header.path("value").asText();
                boolean disabled = header.path("disabled").asBoolean(false);

                if (!disabled && !key.isEmpty()) {
                    headers.put(key, value);
                }
            }
        }
        return headers;
    }

    private Map<String, String> extractQueryParams(JsonNode urlNode) {
        Map<String, String> queryParams = new HashMap<>();

        if (urlNode.isObject()) {
            JsonNode query = urlNode.path("query");
            if (query.isArray()) {
                for (JsonNode param : query) {
                    String key = param.path("key").asText();
                    String value = param.path("value").asText();
                    boolean disabled = param.path("disabled").asBoolean(false);

                    if (!disabled && !key.isEmpty()) {
                        queryParams.put(key, value);
                    }
                }
            }
        }
        return queryParams;
    }

    private String extractUrl(JsonNode urlNode) {
        if (urlNode.isTextual()) {
            return urlNode.asText();
        } else if (urlNode.isObject()) {
            StringBuilder urlBuilder = new StringBuilder();

            // Protocol
            String protocol = urlNode.path("protocol").asText("https");
            urlBuilder.append(protocol).append("://");

            // Host
            JsonNode host = urlNode.path("host");
            if (host.isArray()) {
                for (int i = 0; i < host.size(); i++) {
                    if (i > 0) urlBuilder.append(".");
                    urlBuilder.append(host.get(i).asText());
                }
            } else {
                urlBuilder.append(host.asText());
            }

            // Path
            JsonNode path = urlNode.path("path");
            if (path.isArray()) {
                for (JsonNode pathSegment : path) {
                    urlBuilder.append("/").append(pathSegment.asText());
                }
            }

            // Query parameters (for URL construction, not extraction)
            JsonNode query = urlNode.path("query");
            if (query.isArray() && query.size() > 0) {
                urlBuilder.append("?");
                for (int i = 0; i < query.size(); i++) {
                    if (i > 0) urlBuilder.append("&");
                    JsonNode param = query.get(i);
                    boolean disabled = param.path("disabled").asBoolean(false);
                    if (!disabled) {
                        urlBuilder.append(param.path("key").asText())
                                .append("=")
                                .append(param.path("value").asText());
                    }
                }
            }

            return urlBuilder.toString();
        }

        return "Unknown URL";
    }
}