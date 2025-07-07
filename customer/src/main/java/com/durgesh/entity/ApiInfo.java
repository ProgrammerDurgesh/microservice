package com.durgesh.entity;

import java.util.Map;

public class ApiInfo {
    private String name;
    private String method;
    private String url;
    private String folder;
    private String requestBody;
    private Map<String, String> headers;
    private Map<String, String> queryParams;

    // New fields
    private String baseUrl;
    private String endpoint;
    private Integer priority;

    // Default constructor
    public ApiInfo() {}

    // Updated constructor
    public ApiInfo(String name, String method, String url, String folder,
                   String requestBody, Map<String, String> headers,
                   Map<String, String> queryParams, String baseUrl,
                   String endpoint, Integer priority) {
        this.name = name;
        this.method = method;
        this.url = url;
        this.folder = folder;
        this.requestBody = requestBody;
        this.headers = headers;
        this.queryParams = queryParams;
        this.baseUrl = baseUrl;
        this.endpoint = endpoint;
        this.priority = priority;
    }

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getFolder() { return folder; }
    public void setFolder(String folder) { this.folder = folder; }

    public String getRequestBody() { return requestBody; }
    public void setRequestBody(String requestBody) { this.requestBody = requestBody; }

    public Map<String, String> getHeaders() { return headers; }
    public void setHeaders(Map<String, String> headers) { this.headers = headers; }

    public Map<String, String> getQueryParams() { return queryParams; }
    public void setQueryParams(Map<String, String> queryParams) { this.queryParams = queryParams; }

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }

    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }

    @Override
    public String toString() {
        return "ApiInfo{" +
                "name='" + name + '\'' +
                ", method='" + method + '\'' +
                ", url='" + url + '\'' +
                ", folder='" + folder + '\'' +
                ", baseUrl='" + baseUrl + '\'' +
                ", endpoint='" + endpoint + '\'' +
                ", priority=" + priority +
                '}';
    }
}