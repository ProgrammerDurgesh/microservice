package com.durgesh.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

@Service
public class DynamicPackageGeneratorService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String BASE_PACKAGE = "com.durgesh.generated";
    private static final String BASE_PATH = "src/main/java/com/durgesh/generated";

    // To track global nested classes across all request bodies
    private Set<String> globalNestedClasses = new HashSet<>();

    /**
     * Generates dynamic Java packages and classes based on the provided JSON data.
     * The structure follows: com.durgesh.generated.<paymentProcessorName.toLowerCase()>.dto
     *
     * @param jsonData The JSON string containing payment processor details and API definitions.
     * @return A map indicating the success status, message, and details of generated files.
     */
    public Map<String, Object> generatePackageAndClasses(String jsonData) {
        Map<String, Object> result = new HashMap<>();
        List<String> generatedFiles = new ArrayList<>();

        try {
            // Reset global tracking for nested classes for each generation run
            globalNestedClasses.clear();

            // Parse JSON input
            JsonNode rootNode = objectMapper.readTree(jsonData);

            // Extract core details for package and class naming
            String paymentProcessorName = rootNode.get("PaymentProcessorName").asText();
            String type = rootNode.get("type").asText();
            String packageName = paymentProcessorName.toLowerCase(); // e.g., "new", "stripeprocessor"
            String fullPackageName = BASE_PACKAGE + "." + packageName; // e.g., "com.durgesh.generated.new"

            // Define DTO sub-package name
            String dtoPackageName = fullPackageName + ".dto"; // e.g., "com.durgesh.generated.new.dto"

            // Construct main package directory path
            String packagePath = BASE_PATH + "/" + packageName; // e.g., "src/main/java/com/durgesh/generated/new"
            File packageDir = new File(packagePath);
            if (!packageDir.exists()) {
                packageDir.mkdirs(); // Create main package directory
            }

            // Construct DTO sub-package directory path
            String dtoPackagePath = packagePath + "/dto"; // e.g., "src/main/java/com/durgesh/generated/new/dto"
            File dtoPackageDir = new File(dtoPackagePath);
            if (!dtoPackageDir.exists()) {
                dtoPackageDir.mkdirs(); // Create DTO sub-package directory
            }

            // Generate the main service class (e.g., NewV1.java)
            String mainClassName = paymentProcessorName + type;
            generateMainClass(packagePath, fullPackageName, dtoPackageName, mainClassName, rootNode);
            generatedFiles.add(mainClassName + ".java");

            // Process 'data' array to generate request/response DTOs
            JsonNode dataArray = rootNode.get("data");
            Set<String> generatedRequestBodies = new HashSet<>(); // Track generated DTOs to avoid duplicates

            for (JsonNode dataNode : dataArray) {
                JsonNode apiNode = dataNode.get("api");
                String apiName = apiNode.get("name").asText();
                JsonNode requestBodyRawNode = apiNode.get("requestBody"); // Raw requestBody node

                JsonNode requestBodyForClassGeneration;
                if (requestBodyRawNode != null && requestBodyRawNode.isObject()) {
                    requestBodyForClassGeneration = requestBodyRawNode;
                } else if (requestBodyRawNode != null && requestBodyRawNode.isTextual()) {
                    try {
                        // Attempt to parse string as JSON, if it fails, treat as plain string
                        requestBodyForClassGeneration = objectMapper.readTree(requestBodyRawNode.asText());
                    } catch (IOException e) {
                        requestBodyForClassGeneration = null; // Not a valid JSON object string
                    }
                } else {
                    requestBodyForClassGeneration = null; // No valid request body for structured class
                }

                String requestBodyClassName = formatClassName(apiName);
                if (!generatedRequestBodies.contains(requestBodyClassName)) {
                    // Generate the request body DTO class
                    generateRequestBodyClass(dtoPackagePath, dtoPackageName, requestBodyClassName, requestBodyForClassGeneration);
                    generatedFiles.add("dto/" + requestBodyClassName + ".java");
                    generatedRequestBodies.add(requestBodyClassName);
                }
            }

            result.put("success", true);
            result.put("message", "Package and classes generated successfully");
            result.put("packageName", fullPackageName);
            result.put("dtoPackageName", dtoPackageName);
            result.put("packagePath", packagePath);
            result.put("dtoPackagePath", dtoPackagePath);
            result.put("mainClass", mainClassName);
            result.put("generatedFiles", generatedFiles);

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Error generating package: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
            e.printStackTrace(); // Log the stack trace for detailed debugging
        }

        return result;
    }

    /**
     * Generates the main service class for the payment processor.
     * This class will contain the business logic for making API calls.
     *
     * @param packagePath      The file path to the main package directory.
     * @param fullPackageName  The full Java package name for the main class.
     * @param dtoPackageName   The full Java package name for DTOs (for imports).
     * @param className        The name of the main service class.
     * @param rootNode         The root JSON node for extracting authentication and API details.
     * @throws IOException if an I/O error occurs writing the file.
     */
    private void generateMainClass(String packagePath, String fullPackageName, String dtoPackageName, String className, JsonNode rootNode) throws IOException {
        StringBuilder classContent = new StringBuilder();

        // Package declaration and imports
        classContent.append("package ").append(fullPackageName).append(";\n\n");
        classContent.append("import org.springframework.beans.factory.annotation.Autowired;\n");
        classContent.append("import org.springframework.stereotype.Service;\n");
        classContent.append("import org.springframework.web.client.RestTemplate;\n");
        classContent.append("import org.springframework.http.HttpHeaders;\n"); // Specific import for HttpHeaders
        classContent.append("import org.springframework.http.MediaType;\n");   // Specific import for MediaType
        classContent.append("import org.springframework.http.HttpEntity;\n");  // Specific import for HttpEntity
        classContent.append("import org.springframework.http.ResponseEntity;\n"); // Specific import for ResponseEntity
        classContent.append("import org.springframework.http.HttpMethod;\n");  // Specific import for HttpMethod
        classContent.append("import com.fasterxml.jackson.databind.ObjectMapper;\n");
        classContent.append("import com.fasterxml.jackson.core.JsonProcessingException;\n");
        classContent.append("import ").append(dtoPackageName).append(".*;\n"); // Import all DTOs
        classContent.append("import java.util.*;\n\n");

        classContent.append("@Service\n");
        classContent.append("public class ").append(className).append(" {\n\n");

        classContent.append("    @Autowired\n");
        classContent.append("    private RestTemplate restTemplate;\n\n");

        classContent.append("    private final ObjectMapper objectMapper = new ObjectMapper();\n\n");

        // Add AUTH_TOKEN field if 'auth' array is present in JSON
        JsonNode authArray = rootNode.get("auth");
        String authTokenValue = null;
        if (authArray != null && authArray.isArray() && authArray.size() > 0) {
            authTokenValue = authArray.get(0).asText();
            classContent.append("    private final String AUTH_TOKEN = \"").append(authTokenValue).append("\";\n\n");
        }

        // Generate the main 'processPayment' method
        classContent.append("    /**\n");
        classContent.append("     * Orchestrates payment processing by calling various APIs defined in the configuration.\n");
        classContent.append("     * @param paymentData A map containing dynamic data needed for API calls (e.g., customerId, amount).\n");
        classContent.append("     * @return A map with results from each API call.\n");
        classContent.append("     */\n");
        classContent.append("    public Map<String, Object> processPayment(Map<String, Object> paymentData) {\n");
        classContent.append("        Map<String, Object> result = new HashMap<>();\n");
        classContent.append("        try {\n");

        JsonNode dataArray = rootNode.get("data");
        if (dataArray != null && dataArray.isArray()) {
            for (JsonNode dataNode : dataArray) {
                JsonNode apiNode = dataNode.get("api");
                String apiName = apiNode.get("name").asText();
                String method = apiNode.get("method").asText();
                String endpoint = apiNode.get("endpoint").asText();
                String baseUrl = apiNode.get("baseUrl").asText();
                String fullUrl = baseUrl + endpoint;

                JsonNode requestBodyRawNode = apiNode.get("requestBody");
                String requestBodyClassName = formatClassName(apiName);
                String camelCaseApiName = toCamelCase(apiName);

                if (requestBodyRawNode != null && requestBodyRawNode.isObject()) {
                    // Scenario: Request body is a structured JSON object
                    classContent.append("            // --- API Call: ").append(apiName).append(" ---\n");
                    classContent.append("            ").append(dtoPackageName).append(".").append(requestBodyClassName).append(" ").append(camelCaseApiName).append("Request = new ").append(dtoPackageName).append(".").append(requestBodyClassName).append("();\n");
                    classContent.append("            // TODO: Populate ").append(camelCaseApiName).append("Request with data from 'paymentData' or previous step results.\n");
                    classContent.append("            // Example: ").append(camelCaseApiName).append("Request.setSomeField(paymentData.get(\"someKey\").toString());\n");
                    classContent.append("            String ").append(camelCaseApiName).append("Json = objectMapper.writeValueAsString(").append(camelCaseApiName).append("Request);\n");
                    classContent.append("            HttpEntity<String> ").append(camelCaseApiName).append("Entity = new HttpEntity<>(").append(camelCaseApiName).append("Json, createHeaders());\n");
                    classContent.append("            ResponseEntity<String> ").append(camelCaseApiName).append("Response = restTemplate.exchange(\n");
                    classContent.append("                \"").append(fullUrl).append("\",\n");
                    classContent.append("                HttpMethod.").append(method.toUpperCase()).append(",\n");
                    classContent.append("                ").append(camelCaseApiName).append("Entity,\n");
                    classContent.append("                String.class\n");
                    classContent.append("            );\n");
                    classContent.append("            result.put(\"").append(camelCaseApiName).append("Status\", ").append(camelCaseApiName).append("Response.getStatusCode().value());\n");
                    classContent.append("            result.put(\"").append(camelCaseApiName).append("Body\", ").append(camelCaseApiName).append("Response.getBody());\n");
                    classContent.append("\n");

                } else if (requestBodyRawNode != null && requestBodyRawNode.isTextual()) {
                    // Scenario: Request body is a literal string (could be JSON string or plain text)
                    String literalRequestBody = requestBodyRawNode.asText().replace("\"", "\\\""); // Escape quotes for string literal
                    classContent.append("            // --- API Call: ").append(apiName).append(" (Literal String Request Body) ---\n");
                    classContent.append("            String ").append(camelCaseApiName).append("LiteralRequestBody = \"").append(literalRequestBody).append("\";\n");
                    classContent.append("            HttpEntity<String> ").append(camelCaseApiName).append("Entity = new HttpEntity<>(").append(camelCaseApiName).append("LiteralRequestBody, createHeaders());\n");
                    classContent.append("            ResponseEntity<String> ").append(camelCaseApiName).append("Response = restTemplate.exchange(\n");
                    classContent.append("                \"").append(fullUrl).append("\",\n");
                    classContent.append("                HttpMethod.").append(method.toUpperCase()).append(",\n");
                    classContent.append("                ").append(camelCaseApiName).append("Entity,\n");
                    classContent.append("                String.class\n");
                    classContent.append("            );\n");
                    classContent.append("            result.put(\"").append(camelCaseApiName).append("Status\", ").append(camelCaseApiName).append("Response.getStatusCode().value());\n");
                    classContent.append("            result.put(\"").append(camelCaseApiName).append("Body\", ").append(camelCaseApiName).append("Response.getBody());\n");
                    classContent.append("\n");

                    if (literalRequestBody.contains("{{") && literalRequestBody.contains("}}")) {
                        classContent.append("            // Note: The request body for this step (`").append(apiName).append("`) contains placeholders like `{{CUSTOMER_ID_FROM_PREVIOUS_STEP}}`.\n");
                        classContent.append("            // You will need to implement logic here to replace these placeholders with actual values\n");
                        classContent.append("            // obtained from previous API responses or `paymentData` before making the call.\n\n");
                    }

                } else {
                    // Scenario: No specific request body defined or it's not an object/string
                    classContent.append("            // --- API Call: ").append(apiName).append(" (No Request Body) ---\n");
                    classContent.append("            HttpEntity<String> ").append(camelCaseApiName).append("Entity = new HttpEntity<>(createHeaders());\n");
                    classContent.append("            ResponseEntity<String> ").append(camelCaseApiName).append("Response = restTemplate.exchange(\n");
                    classContent.append("                \"").append(fullUrl).append("\",\n");
                    classContent.append("                HttpMethod.").append(method.toUpperCase()).append(",\n");
                    classContent.append("                ").append(camelCaseApiName).append("Entity,\n");
                    classContent.append("                String.class\n");
                    classContent.append("            );\n");
                    classContent.append("            result.put(\"").append(camelCaseApiName).append("Status\", ").append(camelCaseApiName).append("Response.getStatusCode().value());\n");
                    classContent.append("            result.put(\"").append(camelCaseApiName).append("Body\", ").append(camelCaseApiName).append("Response.getBody());\n");
                    classContent.append("\n");
                }
            }
        }

        classContent.append("            result.put(\"success\", true);\n");
        classContent.append("            result.put(\"message\", \"Payment processing logic executed. Check individual API results for details.\");\n");
        classContent.append("        } catch (Exception e) {\n");
        classContent.append("            result.put(\"success\", false);\n");
        classContent.append("            result.put(\"message\", \"Payment processing failed: \" + e.getMessage());\n");
        classContent.append("            e.printStackTrace();\n");
        classContent.append("        }\n");
        classContent.append("        return result;\n");
        classContent.append("    }\n\n");

        // Moved createHeaders() method inside the generated main class
        classContent.append("    /**\n");
        classContent.append("     * Creates and configures HttpHeaders for API requests.\n");
        classContent.append("     * Includes Content-Type as application/json and Authorization header if AUTH_TOKEN is present.\n");
        classContent.append("     * @return Configured HttpHeaders instance.\n");
        classContent.append("     */\n");
        classContent.append("    private HttpHeaders createHeaders() {\n");
        classContent.append("        HttpHeaders headers = new HttpHeaders();\n"); // Initialize HttpHeaders
        classContent.append("        headers.setContentType(MediaType.APPLICATION_JSON);\n");
        if (authTokenValue != null) { // Only add Authorization header if AUTH_TOKEN was actually found in JSON
            classContent.append("        headers.set(\"Authorization\", \"Bearer \" + AUTH_TOKEN);\n");
        }
        classContent.append("        return headers;\n");
        classContent.append("    }\n");

        classContent.append("}\n");

        writeToFile(packagePath + "/" + className + ".java", classContent.toString());
    }

    /**
     * Generates a request body DTO class. This can be a structured POJO based on a JSON object,
     * or a generic class if the request body is a plain string or null.
     *
     * @param dtoPackagePath   The file path where the DTO class should be written.
     * @param dtoPackageName   The full Java package name for the DTO class.
     * @param className        The name of the DTO class.
     * @param requestBodyNode  The JsonNode representing the request body. Can be null or not an object.
     * @throws IOException if an I/O error occurs writing the file.
     */
    private void generateRequestBodyClass(String dtoPackagePath, String dtoPackageName, String className, JsonNode requestBodyNode) throws IOException {
        try {
            if (requestBodyNode != null && requestBodyNode.isObject()) {
                // For structured JSON objects, generate nested classes as separate files
                Set<String> currentFileNestedClasses = new HashSet<>();
                generateNestedClassesAsSeparateFiles(dtoPackagePath, dtoPackageName, requestBodyNode, currentFileNestedClasses);

                // Generate the main DTO class content
                StringBuilder classContent = new StringBuilder();
                classContent.append("package ").append(dtoPackageName).append(";\n\n");
                classContent.append("/*\n");
                classContent.append(" * To use this DTO, you might need Jackson annotations:\n");
                classContent.append(" * import com.fasterxml.jackson.annotation.JsonProperty; // for snake_case fields if needed\n");
                classContent.append(" * Example usage with ObjectMapper:\n");
                classContent.append(" * ObjectMapper om = new ObjectMapper();\n");
                classContent.append(" * ").append(className).append(" requestBody = om.readValue(jsonString, ").append(className).append(".class);\n");
                classContent.append(" */\n\n");
                classContent.append("public class ").append(className).append(" {\n");
                generateClassFields(classContent, requestBodyNode);
                generateGettersAndSetters(classContent, requestBodyNode);
                generateToString(classContent, requestBodyNode, className);
                classContent.append("}\n");
                writeToFile(dtoPackagePath + "/" + className + ".java", classContent.toString());
            } else {
                // If requestBody is not a structured JSON object (e.g., a simple string, null, or unparsable string)
                StringBuilder classContent = new StringBuilder();
                classContent.append("package ").append(dtoPackageName).append(";\n\n");
                classContent.append("/**\n");
                classContent.append(" * This class represents a request body that was a simple string,\n");
                classContent.append(" * or could not be parsed as a structured JSON object from the input.\n");
                classContent.append(" * You may need to manually modify this DTO if a more complex structure is required.\n");
                classContent.append(" */\n");
                classContent.append("public class ").append(className).append(" {\n");
                classContent.append("    public String rawRequestBody;\n\n"); // Holds the raw string content

                classContent.append("    public String getRawRequestBody() {\n");
                classContent.append("        return rawRequestBody;\n");
                classContent.append("    }\n\n");
                classContent.append("    public void setRawRequestBody(String rawRequestBody) {\n");
                classContent.append("        this.rawRequestBody = rawRequestBody;\n");
                classContent.append("    }\n\n");
                classContent.append("    @Override\n");
                classContent.append("    public String toString() {\n");
                classContent.append("        return \"").append(className).append("{rawRequestBody='\" + (rawRequestBody != null ? rawRequestBody : \"null\") + \"'}\";\n");
                classContent.append("    }\n");
                classContent.append("}\n");
                writeToFile(dtoPackagePath + "/" + className + ".java", classContent.toString());
            }
        } catch (Exception e) {
            // Fallback for any unexpected errors during class generation
            System.err.println("Error generating request body class for " + className + ": " + e.getMessage());
            e.printStackTrace();

            StringBuilder classContent = new StringBuilder();
            classContent.append("package ").append(dtoPackageName).append(";\n\n");
            classContent.append("/**\n");
            classContent.append(" * This class was generated as a fallback due to an error during parsing.\n");
            classContent.append(" * Please check the error logs for details and manually review/correct this class.\n");
            classContent.append(" */\n");
            classContent.append("public class ").append(className).append(" {\n");
            classContent.append("    public String generationError = \"Error during class generation: ").append(e.getMessage().replace("\"", "\\\"")).append("\";\n\n");
            classContent.append("    public String getGenerationError() {\n");
            classContent.append("        return generationError;\n");
            classContent.append("    }\n\n");
            classContent.append("    public void setGenerationError(String generationError) {\n");
            classContent.append("        this.generationError = generation;\n"); // Corrected typo here
            classContent.append("    }\n\n");
            classContent.append("    @Override\n");
            classContent.append("    public String toString() {\n");
            classContent.append("        return \"").append(className).append("{generationError='\" + generationError + \"'}\";\n");
            classContent.append("    }\n");
            classContent.append("}\n");

            writeToFile(dtoPackagePath + "/" + className + ".java", classContent.toString());
        }
    }

    /**
     * Recursively generates separate Java files for nested JSON objects within a DTO.
     * Ensures each nested object becomes its own public class in the same DTO package.
     *
     * @param dtoPackagePath   The file path where nested DTO classes should be written.
     * @param dtoPackageName   The full Java package name for the nested DTO classes.
     * @param jsonNode         The current JSON node being processed for nested objects.
     * @param currentFileNestedClasses A set to track classes generated during the current file's recursion to avoid infinite loops.
     * @throws IOException if an I/O error occurs writing the file.
     */
    private void generateNestedClassesAsSeparateFiles(String dtoPackagePath, String dtoPackageName, JsonNode jsonNode, Set<String> currentFileNestedClasses) throws IOException {
        Iterator<Map.Entry<String, JsonNode>> fields = jsonNode.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            String fieldName = entry.getKey();
            JsonNode fieldValue = entry.getValue();

            if (fieldValue.isObject()) {
                String nestedClassName = capitalize(fieldName);

                // Prevent regenerating the same class multiple times across different DTOs
                // or infinite recursion within the same DTO if there are circular references (though rare in DTOs).
                if (!globalNestedClasses.contains(nestedClassName)) {
                    globalNestedClasses.add(nestedClassName); // Add to global set

                    StringBuilder nestedClassContent = new StringBuilder();
                    nestedClassContent.append("package ").append(dtoPackageName).append(";\n\n");
                    nestedClassContent.append("public class ").append(nestedClassName).append(" {\n");
                    generateClassFields(nestedClassContent, fieldValue);
                    generateGettersAndSetters(nestedClassContent, fieldValue);
                    generateToString(nestedClassContent, fieldValue, nestedClassName);
                    nestedClassContent.append("}\n");

                    writeToFile(dtoPackagePath + "/" + nestedClassName + ".java", nestedClassContent.toString());

                    // Recursively process this newly generated nested class for its own nested objects
                    generateNestedClassesAsSeparateFiles(dtoPackagePath, dtoPackageName, fieldValue, currentFileNestedClasses);
                }
            }
            // Add handling for arrays of objects if needed, which would require generating a DTO for the array element type
            else if (fieldValue.isArray() && fieldValue.size() > 0 && fieldValue.get(0).isObject()) {
                String arrayItemClassName = capitalize(fieldName + "Item"); // e.g., "items" -> "ItemsItem"
                if (!globalNestedClasses.contains(arrayItemClassName)) {
                    globalNestedClasses.add(arrayItemClassName);

                    StringBuilder arrayItemClassContent = new StringBuilder();
                    arrayItemClassContent.append("package ").append(dtoPackageName).append(";\n\n");
                    arrayItemClassContent.append("public class ").append(arrayItemClassName).append(" {\n");
                    generateClassFields(arrayItemClassContent, fieldValue.get(0)); // Use the first item to infer structure
                    generateGettersAndSetters(arrayItemClassContent, fieldValue.get(0));
                    generateToString(arrayItemClassContent, fieldValue.get(0), arrayItemClassName);
                    arrayItemClassContent.append("}\n");
                    writeToFile(dtoPackagePath + "/" + arrayItemClassName + ".java", arrayItemClassContent.toString());

                    // Recursively check for nested objects within the array item
                    generateNestedClassesAsSeparateFiles(dtoPackagePath, dtoPackageName, fieldValue.get(0), currentFileNestedClasses);
                }
            }
        }
    }

    /**
     * Generates Java field declarations for a given JSON node.
     *
     * @param classContent The StringBuilder to append field declarations to.
     * @param jsonNode     The JSON node containing fields to convert.
     */
    private void generateClassFields(StringBuilder classContent, JsonNode jsonNode) {
        jsonNode.fields().forEachRemaining(entry -> {
            String fieldName = entry.getKey();
            JsonNode fieldValue = entry.getValue();
            String javaFieldName = toCamelCase(fieldName);

            // Determine Java type based on JSON field type
            if (fieldValue.isObject()) {
                classContent.append("    public ").append(capitalize(fieldName)).append(" ").append(javaFieldName).append(";\n");
            } else if (fieldValue.isArray()) {
                // If array of objects, use the generated class name for array items
                if (fieldValue.size() > 0 && fieldValue.get(0).isObject()) {
                    classContent.append("    public java.util.List<").append(capitalize(fieldName + "Item")).append("> ").append(javaFieldName).append(";\n");
                } else {
                    // Default to List<Object> for arrays of primitives or mixed types
                    classContent.append("    public java.util.List<Object> ").append(javaFieldName).append(";\n");
                }
            } else if (fieldValue.isTextual()) {
                classContent.append("    public String ").append(javaFieldName).append(";\n");
            } else if (fieldValue.isNumber()) {
                if (fieldValue.isInt()) {
                    classContent.append("    public int ").append(javaFieldName).append(";\n");
                } else if (fieldValue.isLong()) {
                    classContent.append("    public long ").append(javaFieldName).append(";\n");
                } else if (fieldValue.isDouble()) {
                    classContent.append("    public double ").append(javaFieldName).append(";\n");
                } else {
                    classContent.append("    public Number ").append(javaFieldName).append(";\n"); // Catch-all for other numeric types
                }
            } else if (fieldValue.isBoolean()) {
                classContent.append("    public boolean ").append(javaFieldName).append(";\n");
            } else if (fieldValue.isNull()) {
                classContent.append("    public Object ").append(javaFieldName).append(";\n"); // Null fields are generic
            } else {
                classContent.append("    public Object ").append(javaFieldName).append(";\n"); // Catch-all for unknown types
            }
        });
    }

    /**
     * Generates standard getter and setter methods for fields in a DTO class.
     *
     * @param classContent The StringBuilder to append getter/setter methods to.
     * @param jsonNode     The JSON node from which fields are derived.
     */
    private void generateGettersAndSetters(StringBuilder classContent, JsonNode jsonNode) {
        classContent.append("\n");

        jsonNode.fields().forEachRemaining(entry -> {
            String fieldName = entry.getKey();
            JsonNode fieldValue = entry.getValue();
            String javaFieldName = toCamelCase(fieldName);
            String capitalizedName = capitalize(javaFieldName); // For method names (e.g., getFieldName)
            String fieldType;

            // Determine field type for method signatures
            if (fieldValue.isObject()) {
                fieldType = capitalize(fieldName);
            } else if (fieldValue.isArray()) {
                if (fieldValue.size() > 0 && fieldValue.get(0).isObject()) {
                    fieldType = "java.util.List<" + capitalize(fieldName + "Item") + ">";
                } else {
                    fieldType = "java.util.List<Object>";
                }
            } else if (fieldValue.isTextual()) {
                fieldType = "String";
            } else if (fieldValue.isNumber()) {
                if (fieldValue.isInt()) {
                    fieldType = "int";
                } else if (fieldValue.isLong()) {
                    fieldType = "long";
                } else if (fieldValue.isDouble()) {
                    fieldType = "double";
                } else {
                    fieldType = "Number";
                }
            } else if (fieldValue.isBoolean()) {
                fieldType = "boolean";
            } else {
                fieldType = "Object";
            }

            // Getter method
            classContent.append("    public ").append(fieldType).append(" get").append(capitalizedName).append("() {\n");
            classContent.append("        return ").append(javaFieldName).append(";\n");
            classContent.append("    }\n\n");

            // Setter method
            classContent.append("    public void set").append(capitalizedName).append("(").append(fieldType).append(" ").append(javaFieldName).append(") {\n");
            classContent.append("        this.").append(javaFieldName).append(" = ").append(javaFieldName).append(";\n");
            classContent.append("    }\n\n");
        });
    }

    /**
     * Generates an overridden toString() method for a DTO class.
     *
     * @param classContent The StringBuilder to append the toString() method to.
     * @param jsonNode     The JSON node containing fields to include in the string representation.
     * @param className    The name of the class for the toString() header.
     */
    private void generateToString(StringBuilder classContent, JsonNode jsonNode, String className) {
        classContent.append("    @Override\n");
        classContent.append("    public String toString() {\n");
        classContent.append("        return \"").append(className).append("{\" +\n");

        List<String> fields = new ArrayList<>();
        jsonNode.fields().forEachRemaining(entry -> {
            String javaFieldName = toCamelCase(entry.getKey());
            fields.add(javaFieldName);
        });

        for (int i = 0; i < fields.size(); i++) {
            String field = fields.get(i);
            if (i == 0) {
                classContent.append("                \"").append(field).append("=\" + ").append(field);
            } else {
                classContent.append(" +\n                \", ").append(field).append("=\" + ").append(field);
            }
        }

        classContent.append(" +\n                '}';\n");
        classContent.append("    }\n");
    }

    /**
     * Formats an API name into a valid PascalCase Java class name.
     * Removes special characters and handles leading digits by prepending "Api".
     *
     * @param name The original API name (e.g., "create customer", "get-status_v2").
     * @return A formatted class name (e.g., "CreateCustomer", "GetStatusV2", "Api123").
     */
    private String formatClassName(String name) {
        // Remove special characters and convert to PascalCase
        String cleaned = name.replaceAll("[^a-zA-Z0-9\\s]", "");
        String[] words = cleaned.split("\\s+");
        StringBuilder result = new StringBuilder();

        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(capitalize(word));
            }
        }
        // If the resulting class name is empty or starts with a digit, prepend "Api" to make it valid
        if (result.length() == 0 || (result.length() > 0 && Character.isDigit(result.charAt(0)))) {
            result.insert(0, "Api");
        }

        return result.toString();
    }

    /**
     * Converts a string to lower camelCase. Handles snake_case and PascalCase input.
     *
     * @param str The input string (e.g., "Customer_ID", "CustomerName", "customerName").
     * @return The string in lower camelCase (e.g., "customerId", "customerName").
     */
    private String toCamelCase(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }

        // Convert snake_case to camelCase
        if (str.contains("_")) {
            String[] words = str.split("_");
            StringBuilder result = new StringBuilder(words[0].toLowerCase());
            for (int i = 1; i < words.length; i++) {
                result.append(capitalize(words[i]));
            }
            return result.toString();
        }

        // Convert PascalCase to lowerCamelCase
        if (str.length() > 0 && Character.isUpperCase(str.charAt(0))) {
            return str.substring(0, 1).toLowerCase() + str.substring(1);
        }

        return str; // Already in lower camelCase or single word
    }

    /**
     * Capitalizes the first letter of a string and converts the rest to lowercase.
     *
     * @param str The input string.
     * @return The capitalized string.
     */
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        // Ensure the rest of the string is lowercase after the first char is capitalized
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    /**
     * Writes the given content to a file at the specified path.
     *
     * @param filePath The full path including the file name and extension.
     * @param content  The string content to write to the file.
     * @throws IOException if an I/O error occurs.
     */
    private void writeToFile(String filePath, String content) throws IOException {
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write(content);
        }
    }
}