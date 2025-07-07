package com.durgesh.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;

public class PackageGeneratorUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Pattern VALID_JAVA_IDENTIFIER = Pattern.compile("^[a-zA-Z_$][a-zA-Z0-9_$]*$");

    /**
     * Validates if the provided JSON string is valid
     */
    public static boolean isValidJson(String jsonString) {
        try {
            objectMapper.readTree(jsonString);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Validates if the JSON contains required fields
     */
    public static Map<String, Object> validateRequiredFields(JsonNode rootNode) {
        Map<String, Object> validation = new HashMap<>();
        List<String> missingFields = new ArrayList<>();

        // Check required fields
        if (!rootNode.has("type") || rootNode.get("type").asText().isEmpty()) {
            missingFields.add("type");
        }

        if (!rootNode.has("PaymentProcessorName") || rootNode.get("PaymentProcessorName").asText().isEmpty()) {
            missingFields.add("PaymentProcessorName");
        }

        if (!rootNode.has("data") || !rootNode.get("data").isArray()) {
            missingFields.add("data");
        }

        validation.put("isValid", missingFields.isEmpty());
        validation.put("missingFields", missingFields);

        return validation;
    }

    /**
     * Sanitizes class name to be valid Java identifier
     */
    public static String sanitizeClassName(String name) {
        if (name == null || name.isEmpty()) {
            return "DefaultClass";
        }

        // Remove special characters and spaces
        String sanitized = name.replaceAll("[^a-zA-Z0-9_$]", "");

        // Ensure it starts with a letter or underscore
        if (!sanitized.isEmpty() && !Character.isLetter(sanitized.charAt(0)) && sanitized.charAt(0) != '_') {
            sanitized = "_" + sanitized;
        }

        // If empty after sanitization, provide default
        if (sanitized.isEmpty()) {
            return "DefaultClass";
        }

        // Capitalize first letter
        return sanitized.substring(0, 1).toUpperCase() + sanitized.substring(1);
    }

    /**
     * Sanitizes package name to be valid Java package identifier
     */
    public static String sanitizePackageName(String name) {
        if (name == null || name.isEmpty()) {
            return "defaultpackage";
        }

        return name.toLowerCase()
                .replaceAll("[^a-zA-Z0-9_]", "")
                .replaceAll("^[0-9]+", ""); // Remove leading numbers
    }

    /**
     * Determines Java type from JSON node
     */
    public static String determineJavaType(JsonNode node) {
        if (node == null || node.isNull()) {
            return "Object";
        }

        if (node.isTextual()) {
            return "String";
        } else if (node.isInt()) {
            return "Integer";
        } else if (node.isLong()) {
            return "Long";
        } else if (node.isDouble() || node.isFloat()) {
            return "Double";
        } else if (node.isBoolean()) {
            return "Boolean";
        } else if (node.isArray()) {
            return "List<Object>";
        } else if (node.isObject()) {
            return "Map<String, Object>";
        } else {
            return "Object";
        }
    }

    /**
     * Extracts unique request body structures from data array
     */
    public static Map<String, String> extractUniqueRequestBodies(JsonNode dataArray) {
        Map<String, String> uniqueRequestBodies = new HashMap<>();

        for (JsonNode dataItem : dataArray) {
            if (dataItem.has("api") && dataItem.get("api").has("requestBody")) {
                String apiName = dataItem.get("api").get("name").asText();
                String requestBody = dataItem.get("api").get("requestBody").asText();

                String className = sanitizeClassName(apiName) + "RequestBody";
                uniqueRequestBodies.put(className, requestBody);
            }
        }

        return uniqueRequestBodies;
    }

    /**
     * Creates directory structure if it doesn't exist
     */
    public static boolean createDirectoryStructure(String path) {
        try {
            Path dirPath = Paths.get(path);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Generates Java field declaration with annotation
     */
    public static String generateFieldDeclaration(String fieldName, String fieldType, boolean withAnnotation) {
        StringBuilder declaration = new StringBuilder();

        if (withAnnotation) {
            declaration.append("    @JsonProperty(\"").append(fieldName).append("\")\n");
        }
        declaration.append("    private ").append(fieldType).append(" ").append(fieldName).append(";\n");

        return declaration.toString();
    }

    /**
     * Generates getter method
     */
    public static String generateGetter(String fieldName, String fieldType) {
        return "    public " + fieldType + " get" + capitalize(fieldName) + "() {\n" +
                "        return " + fieldName + ";\n" +
                "    }\n";
    }

    /**
     * Generates setter method
     */
    public static String generateSetter(String fieldName, String fieldType) {
        return "    public void set" + capitalize(fieldName) + "(" + fieldType + " " + fieldName + ") {\n" +
                "        this." + fieldName + " = " + fieldName + ";\n" +
                "    }\n";
    }

    /**
     * Capitalizes first letter of a string
     */
    public static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    /**
     * Generates package declaration
     */
    public static String generatePackageDeclaration(String packageName) {
        return "package com.durgesh.generated." + packageName + ";\n\n";
    }

    /**
     * Generates common imports
     */
    public static String generateCommonImports() {
        return "import com.fasterxml.jackson.annotation.JsonProperty;\n" +
                "import java.util.List;\n" +
                "import java.util.Map;\n\n";
    }

    /**
     * Generates class header with documentation
     */
    public static String generateClassHeader(String className, String description) {
        return "/**\n" +
                " * " + description + "\n" +
                " * Auto-generated by DynamicPackageGeneratorService\n" +
                " * Generated on: " + new Date().toString() + "\n" +
                " */\n" +
                "public class " + className + " {\n\n";
    }

    /**
     * Extracts API information from data array
     */
    public static List<Map<String, Object>> extractApiInfo(JsonNode dataArray) {
        List<Map<String, Object>> apiList = new ArrayList<>();

        for (JsonNode dataItem : dataArray) {
            if (dataItem.has("api")) {
                Map<String, Object> apiInfo = new HashMap<>();
                JsonNode api = dataItem.get("api");

                apiInfo.put("name", api.get("name").asText());
                apiInfo.put("method", api.get("method").asText());
                apiInfo.put("url", api.get("url").asText());
                apiInfo.put("actionType", dataItem.get("actionType").asText());
                apiInfo.put("sequence", dataItem.get("sequence").asInt());
                apiInfo.put("priority", dataItem.get("priority").asInt());

                apiList.add(apiInfo);
            }
        }

        return apiList;
    }

    /**
     * Validates Java identifier
     */
    public static boolean isValidJavaIdentifier(String identifier) {
        return VALID_JAVA_IDENTIFIER.matcher(identifier).matches();
    }

    /**
     * Generates toString method for a class
     */
    public static String generateToString(String className, List<String> fieldNames) {
        StringBuilder toString = new StringBuilder();
        toString.append("    @Override\n");
        toString.append("    public String toString() {\n");
        toString.append("        return \"").append(className).append("{\" +\n");

        for (int i = 0; i < fieldNames.size(); i++) {
            String fieldName = fieldNames.get(i);
            toString.append("                \"").append(fieldName).append("=\" + ").append(fieldName);
            if (i < fieldNames.size() - 1) {
                toString.append(" + \", \" +\n");
            } else {
                toString.append(" +\n");
            }
        }

        toString.append("                \"}\";\n");
        toString.append("    }\n");

        return toString.toString();
    }

    /**
     * Generates equals method for a class
     */
    public static String generateEquals(String className, List<String> fieldNames) {
        StringBuilder equals = new StringBuilder();
        equals.append("    @Override\n");
        equals.append("    public boolean equals(Object obj) {\n");
        equals.append("        if (this == obj) return true;\n");
        equals.append("        if (obj == null || getClass() != obj.getClass()) return false;\n");
        equals.append("        ").append(className).append(" that = (").append(className).append(") obj;\n");
        equals.append("        return ");

        for (int i = 0; i < fieldNames.size(); i++) {
            String fieldName = fieldNames.get(i);
            equals.append("Objects.equals(").append(fieldName).append(", that.").append(fieldName).append(")");
            if (i < fieldNames.size() - 1) {
                equals.append(" && ");
            }
        }

        equals.append(";\n");
        equals.append("    }\n");

        return equals.toString();
    }

    /**
     * Generates hashCode method for a class
     */
    public static String generateHashCode(List<String> fieldNames) {
        StringBuilder hashCode = new StringBuilder();
        hashCode.append("    @Override\n");
        hashCode.append("    public int hashCode() {\n");
        hashCode.append("        return Objects.hash(");

        for (int i = 0; i < fieldNames.size(); i++) {
            hashCode.append(fieldNames.get(i));
            if (i < fieldNames.size() - 1) {
                hashCode.append(", ");
            }
        }

        hashCode.append(");\n");
        hashCode.append("    }\n");

        return hashCode.toString();
    }

    /**
     * Generates constructor with all fields
     */
    public static String generateConstructor(String className, List<String> fieldNames, List<String> fieldTypes) {
        StringBuilder constructor = new StringBuilder();
        constructor.append("    public ").append(className).append("(");

        // Parameters
        for (int i = 0; i < fieldNames.size(); i++) {
            constructor.append(fieldTypes.get(i)).append(" ").append(fieldNames.get(i));
            if (i < fieldNames.size() - 1) {
                constructor.append(", ");
            }
        }

        constructor.append(") {\n");

        // Assignments
        for (String fieldName : fieldNames) {
            constructor.append("        this.").append(fieldName).append(" = ").append(fieldName).append(";\n");
        }

        constructor.append("    }\n");

        return constructor.toString();
    }

    /**
     * Generates default constructor
     */
    public static String generateDefaultConstructor(String className) {
        return "    public " + className + "() {\n" +
                "        // Default constructor\n" +
                "    }\n";
    }

    /**
     * Extracts nested object fields from JSON
     */
    public static Map<String, String> extractNestedFields(JsonNode node, String prefix) {
        Map<String, String> fields = new HashMap<>();

        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fieldsIterator = node.fields();
            while (fieldsIterator.hasNext()) {
                Map.Entry<String, JsonNode> entry = fieldsIterator.next();
                String fieldName = prefix.isEmpty() ? entry.getKey() : prefix + capitalize(entry.getKey());
                JsonNode fieldValue = entry.getValue();

                if (fieldValue.isObject()) {
                    // Recursively extract nested fields
                    fields.putAll(extractNestedFields(fieldValue, fieldName));
                } else {
                    fields.put(fieldName, determineJavaType(fieldValue));
                }
            }
        }

        return fields;
    }

    /**
     * Generates validation annotations
     */
    public static String generateValidationAnnotations(String fieldName, String fieldType) {
        StringBuilder annotations = new StringBuilder();

        if ("String".equals(fieldType)) {
            annotations.append("    @NotBlank(message = \"").append(fieldName).append(" cannot be blank\")\n");
        } else if ("Integer".equals(fieldType) || "Long".equals(fieldType) || "Double".equals(fieldType)) {
            annotations.append("    @NotNull(message = \"").append(fieldName).append(" cannot be null\")\n");
        } else if (fieldType.startsWith("List")) {
            annotations.append("    @NotEmpty(message = \"").append(fieldName).append(" cannot be empty\")\n");
        }

        return annotations.toString();
    }

    /**
     * Generates builder pattern methods
     */
    public static String generateBuilderMethods(String className, List<String> fieldNames, List<String> fieldTypes) {
        StringBuilder builder = new StringBuilder();

        // Builder class
        builder.append("    public static class Builder {\n");

        // Builder fields
        for (int i = 0; i < fieldNames.size(); i++) {
            builder.append("        private ").append(fieldTypes.get(i)).append(" ").append(fieldNames.get(i)).append(";\n");
        }

        builder.append("\n");

        // Builder methods
        for (int i = 0; i < fieldNames.size(); i++) {
            String fieldName = fieldNames.get(i);
            String fieldType = fieldTypes.get(i);

            builder.append("        public Builder ").append(fieldName).append("(").append(fieldType).append(" ").append(fieldName).append(") {\n");
            builder.append("            this.").append(fieldName).append(" = ").append(fieldName).append(";\n");
            builder.append("            return this;\n");
            builder.append("        }\n\n");
        }

        // Build method
        builder.append("        public ").append(className).append(" build() {\n");
        builder.append("            return new ").append(className).append("(");

        for (int i = 0; i < fieldNames.size(); i++) {
            builder.append(fieldNames.get(i));
            if (i < fieldNames.size() - 1) {
                builder.append(", ");
            }
        }

        builder.append(");\n");
        builder.append("        }\n");
        builder.append("    }\n\n");

        // Static builder method
        builder.append("    public static Builder builder() {\n");
        builder.append("        return new Builder();\n");
        builder.append("    }\n");

        return builder.toString();
    }

    /**
     * Generates file header comment
     */
    public static String generateFileHeader(String className, String packageName) {
        return "/*\n" +
                " * Auto-generated class: " + className + "\n" +
                " * Package: com.durgesh.generated." + packageName + "\n" +
                " * Generated on: " + new Date().toString() + "\n" +
                " * \n" +
                " * This file was automatically generated by DynamicPackageGeneratorService\n" +
                " * Do not modify this file manually as it may be overwritten\n" +
                " */\n\n";
    }

    /**
     * Validates package structure
     */
    public static Map<String, Object> validatePackageStructure(String basePath, String packageName) {
        Map<String, Object> validation = new HashMap<>();

        try {
            Path packagePath = Paths.get(basePath, packageName);
            boolean exists = Files.exists(packagePath);
            boolean isDirectory = Files.isDirectory(packagePath);
            boolean isWritable = Files.isWritable(packagePath.getParent());

            validation.put("exists", exists);
            validation.put("isDirectory", isDirectory);
            validation.put("isWritable", isWritable);
            validation.put("path", packagePath.toString());
            validation.put("isValid", exists && isDirectory && isWritable);

        } catch (Exception e) {
            validation.put("isValid", false);
            validation.put("error", e.getMessage());
        }

        return validation;
    }
}