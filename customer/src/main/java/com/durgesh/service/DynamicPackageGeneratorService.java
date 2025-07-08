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

            // Validate required fields with proper null checks
            JsonNode paymentProcessorNode = rootNode.get("PaymentProcessorName");
            JsonNode typeNode = rootNode.get("type");

            if (paymentProcessorNode == null || paymentProcessorNode.isNull() || paymentProcessorNode.asText().trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "Error: 'PaymentProcessorName' field is required and cannot be null or empty");
                result.put("error", "MissingRequiredField");
                return result;
            }

            if (typeNode == null || typeNode.isNull() || typeNode.asText().trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "Error: 'type' field is required and cannot be null or empty");
                result.put("error", "MissingRequiredField");
                return result;
            }

            // Extract core details for package and class naming
            String paymentProcessorName = paymentProcessorNode.asText().trim();
            String type = typeNode.asText().trim();

            // Validate that the names are valid for Java package/class names
            if (!isValidJavaIdentifier(paymentProcessorName)) {
                result.put("success", false);
                result.put("message", "Error: 'PaymentProcessorName' contains invalid characters for Java package naming: " + paymentProcessorName);
                result.put("error", "InvalidIdentifier");
                return result;
            }

            if (!isValidJavaIdentifier(type)) {
                result.put("success", false);
                result.put("message", "Error: 'type' contains invalid characters for Java class naming: " + type);
                result.put("error", "InvalidIdentifier");
                return result;
            }

            String packageName = paymentProcessorName.toLowerCase(); // e.g., "new", "stripeprocessor"
            String fullPackageName = BASE_PACKAGE + "." + packageName; // e.g., "com.durgesh.generated.new"

            // Define DTO sub-package name
            String dtoPackageName = fullPackageName + ".dto"; // e.g., "com.durgesh.generated.new.dto"

            // Construct main package directory path
            String packagePath = BASE_PATH + "/" + packageName; // e.g., "src/main/java/com/durgesh/generated/new"
            File packageDir = new File(packagePath);
            if (!packageDir.exists()) {
                boolean created = packageDir.mkdirs(); // Create main package directory
                if (!created) {
                    result.put("success", false);
                    result.put("message", "Error: Failed to create package directory: " + packagePath);
                    result.put("error", "DirectoryCreationFailed");
                    return result;
                }
            }

            // Construct DTO sub-package directory path
            String dtoPackagePath = packagePath + "/dto"; // e.g., "src/main/java/com/durgesh/generated/new/dto"
            File dtoPackageDir = new File(dtoPackagePath);
            if (!dtoPackageDir.exists()) {
                boolean created = dtoPackageDir.mkdirs(); // Create DTO sub-package directory
                if (!created) {
                    result.put("success", false);
                    result.put("message", "Error: Failed to create DTO package directory: " + dtoPackagePath);
                    result.put("error", "DirectoryCreationFailed");
                    return result;
                }
            }

            // Generate the main service class (e.g., NewV1.java)
            String mainClassName = paymentProcessorName + type;
            generateMainClass(packagePath, fullPackageName, dtoPackageName, mainClassName, rootNode);
            generatedFiles.add(mainClassName + ".java");

            // Process 'data' array to generate request/response DTOs
            JsonNode dataArray = rootNode.get("data");
            Set<String> generatedRequestBodies = new HashSet<>(); // Track generated DTOs to avoid duplicates

            if (dataArray != null && dataArray.isArray()) {
                for (JsonNode dataNode : dataArray) {
                    if (dataNode == null || dataNode.isNull()) {
                        continue; // Skip null data nodes
                    }

                    JsonNode apiNode = dataNode.get("api");
                    if (apiNode == null || apiNode.isNull()) {
                        continue; // Skip if api node is missing
                    }

                    JsonNode apiNameNode = apiNode.get("name");
                    if (apiNameNode == null || apiNameNode.isNull() || apiNameNode.asText().trim().isEmpty()) {
                        continue; // Skip if API name is missing
                    }

                    String apiName = apiNameNode.asText().trim();
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
            } else {
                // If data array is missing, still proceed but log a warning
                System.out.println("Warning: 'data' array is missing or not an array in the JSON input. Only main class will be generated.");
            }

            result.put("success", true);
            result.put("message", "Package and classes generated successfully");
            result.put("packageName", fullPackageName);
            result.put("dtoPackageName", dtoPackageName);
            result.put("packagePath", packagePath);
            result.put("dtoPackagePath", dtoPackagePath);
            result.put("mainClass", mainClassName);
            result.put("generatedFiles", generatedFiles);

        } catch (IOException e) {
            result.put("success", false);
            result.put("message", "Error parsing JSON input: " + e.getMessage());
            result.put("error", "JSONParsingError");
            e.printStackTrace();
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Error generating package: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
            e.printStackTrace(); // Log the stack trace for detailed debugging
        }

        return result;
    }

    /**
     * Validates if a string is a valid Java identifier for package/class naming.
     * Removes common special characters and checks basic validity.
     *
     * @param identifier The string to validate
     * @return true if the identifier is valid for Java naming
     */
    private boolean isValidJavaIdentifier(String identifier) {
        if (identifier == null || identifier.trim().isEmpty()) {
            return false;
        }

        // Remove common special characters and spaces, convert to valid format
        String cleaned = identifier.replaceAll("[^a-zA-Z0-9]", "");

        // Check if after cleaning we have something left
        if (cleaned.isEmpty()) {
            return false;
        }

        // Java identifiers cannot start with a digit
        if (Character.isDigit(cleaned.charAt(0))) {
            return false;
        }

        // Check if it's a valid Java identifier
        return Character.isJavaIdentifierStart(cleaned.charAt(0)) &&
                cleaned.chars().skip(1).allMatch(Character::isJavaIdentifierPart);
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
    private void generateMainClass(String packagePath, String fullPackageName, String dtoPackageName,
                                   String className, JsonNode rootNode) throws IOException {
        StringBuilder classContent = new StringBuilder();

        // Extract payment processor details for interface implementation
        String paymentProcessorName = rootNode.get("PaymentProcessorName").asText().trim();
        String type = rootNode.get("type").asText().trim();

        // Package declaration and imports
        classContent.append("package ").append(fullPackageName).append(";\n\n");
        classContent.append("import org.springframework.beans.factory.annotation.Autowired;\n");
        classContent.append("import org.springframework.stereotype.Service;\n");
        classContent.append("import org.springframework.web.client.RestTemplate;\n");
        classContent.append("import org.springframework.http.HttpHeaders;\n");
        classContent.append("import org.springframework.http.MediaType;\n");
        classContent.append("import org.springframework.http.HttpEntity;\n");
        classContent.append("import org.springframework.http.ResponseEntity;\n");
        classContent.append("import org.springframework.http.HttpMethod;\n");
        classContent.append("import com.fasterxml.jackson.databind.ObjectMapper;\n");
        classContent.append("import com.fasterxml.jackson.core.JsonProcessingException;\n");
        classContent.append("import com.durgesh.service.PaymentProcessor;\n");
        classContent.append("import ").append(dtoPackageName).append(".*;\n");
        classContent.append("import java.util.*;\n\n");

        // Class declaration with PaymentProcessor interface implementation
        classContent.append("@Service\n");
        classContent.append("public class ").append(className).append(" implements PaymentProcessor {\n\n");

        // Instance variables
        classContent.append("    @Autowired\n");
        classContent.append("    private RestTemplate restTemplate;\n\n");
        classContent.append("    private final ObjectMapper objectMapper = new ObjectMapper();\n");
        classContent.append("    private final String PROCESSOR_NAME = \"").append(paymentProcessorName).append("\";\n");
        classContent.append("    private final String PROCESSOR_TYPE = \"").append(type).append("\";\n");

        // Add AUTH_TOKEN field if 'auth' array is present in JSON
        JsonNode authArray = rootNode.get("auth");
        String authTokenValue = null;
        if (authArray != null && authArray.isArray() && authArray.size() > 0) {
            authTokenValue = authArray.get(0).asText();
            classContent.append("    private final String AUTH_TOKEN = \"").append(authTokenValue).append("\";\n");
        }
        classContent.append("\n");

        // ========== PAYMENTPROCESSOR INTERFACE METHOD OVERRIDES ==========

        // 1. Override createPayload method
        classContent.append("    @Override\n");
        classContent.append("    public String createPayload() {\n");
        classContent.append("        // TODO: Implement payload creation logic\n");
        classContent.append("        return \"\";\n");
        classContent.append("    }\n\n");

        // 2. Override createRequest method
        classContent.append("    @Override\n");
        classContent.append("    public void createRequest() {\n");
        classContent.append("        // TODO: Implement request creation logic\n");
        classContent.append("    }\n\n");

        // 3. Override checkStatus method
        classContent.append("    @Override\n");
        classContent.append("    public void checkStatus() {\n");
        classContent.append("        // TODO: Implement status check logic\n");
        classContent.append("    }\n\n");

        // 4. Override executeWebhook method
        classContent.append("    @Override\n");
        classContent.append("    public ResponseEntity<?> executeWebhook() {\n");
        classContent.append("        // TODO: Implement webhook execution logic\n");
        classContent.append("        return null;\n");
        classContent.append("    }\n\n");

        // 5. Override getTimeoutMsg method
        classContent.append("    @Override\n");
        classContent.append("    public void getTimeoutMsg() {\n");
        classContent.append("        // TODO: Implement timeout message logic\n");
        classContent.append("    }\n\n");

        // 6. Override createResponseEntity method
        classContent.append("    @Override\n");
        classContent.append("    public ResponseEntity<?> createResponseEntity() {\n");
        classContent.append("        // TODO: Implement response entity creation logic\n");
        classContent.append("        return null;\n");
        classContent.append("    }\n\n");

        // 7. Override getOrderId method
        classContent.append("    @Override\n");
        classContent.append("    public String getOrderId() {\n");
        classContent.append("        // TODO: Implement order ID retrieval logic\n");
        classContent.append("        return \"\";\n");
        classContent.append("    }\n\n");

        // 8. Override postRedirectAfterCustVerification method
        classContent.append("    @Override\n");
        classContent.append("    public ResponseEntity<?> postRedirectAfterCustVerification() {\n");
        classContent.append("        // TODO: Implement post-redirect after customer verification logic\n");
        classContent.append("        return null;\n");
        classContent.append("    }\n\n");

        // 9. Override threeDSVerify method
        classContent.append("    @Override\n");
        classContent.append("    public void threeDSVerify() {\n");
        classContent.append("        // TODO: Implement 3DS verification logic\n");
        classContent.append("    }\n\n");



        // Close class
        classContent.append("}\n");

        // Write the generated class to file
        writeToFile(packagePath + "/" + className + ".java", classContent.toString());
    } /**
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