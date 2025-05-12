package com.autodoc.builder;

import com.autodoc.model.EndpointData;
import com.autodoc.model.ModelData;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class OpenApiBuilder {

    private final SchemaBuilder schemaBuilder;

    public OpenApiBuilder(SchemaBuilder schemaBuilder) {
        this.schemaBuilder = schemaBuilder;
    }

    public Map<String, Object> buildOpenApiSpec(List<EndpointData> endpoints, List<ModelData> models) {
        // Build component schemas
        schemaBuilder.buildSchemas(models);
        Map<String, Object> schemas = schemaBuilder.getComponents();

        Map<String, Object> spec = new LinkedHashMap<>();
        spec.put("openapi", "3.0.0");
        spec.put("info", Map.of(
                "title", "Generated API Documentation",
                "version", "1.0.0"));
        spec.put("servers", List.of(
                Map.of("url", "https://api.example.com/v1", "description", "Production server")));
        spec.put("paths", buildPaths(endpoints));
        spec.put("components", Map.of("schemas", schemas));
        return spec;
    }

    public Map<String, Object> buildPaths(List<EndpointData> endpoints) {
        Map<String, Object> paths = new LinkedHashMap<>();
        for (EndpointData endpoint : endpoints) {
            String path = endpoint.getEndpoint();
            String method = endpoint.getHttpMethod().toLowerCase();

            Map<String, Object> operation = new LinkedHashMap<>();
            operation.put("parameters", buildParameters(endpoint.getParameters()));

            Map<String, Object> requestBody = buildRequestBody(endpoint.getRequestBody(), method);
            if (requestBody != null) {
                operation.put("requestBody", requestBody);
            }

            operation.put("responses", buildResponses(endpoint.getResponseType()));

            paths.computeIfAbsent(path, k -> new LinkedHashMap<String, Object>());
            @SuppressWarnings("unchecked")
            Map<String, Object> methodsMap = (Map<String, Object>) paths.get(path);
            methodsMap.put(method, operation);
        }
        return paths;
    }

    private List<Map<String, Object>> buildParameters(List<String> parameters) {
        return parameters.stream().map(raw -> {
            String[] parts = raw.split(":\\s*");
            String kind = parts[0];
            String name = parts[1];
            String in = kind.equals("PathVariable") ? "path" : "query";
            return Map.of(
                    "name", name,
                    "in", in,
                    "required", in.equals("path"),
                    "schema", Map.of("type", "string"));
        }).toList();
    }

    private Map<String, Object> buildRequestBody(String requestBody, String httpMethod) {
        if (requestBody == null || "get".equalsIgnoreCase(httpMethod)) {
            return null;
        }
        return Map.of(
                "required", true,
                "content", Map.of(
                        "application/json", Map.of(
                                "schema", Map.of("type", "object"))));
    }

    /**
     * Builds the response schema, handling nested generics and wrappers.
     */
    private Map<String, Object> buildResponses(String responseType) {
        Object schema;

        if (responseType != null && responseType.contains("<") && responseType.endsWith(">")) {
            // Outer generic, e.g. List<T> or ApiResponse<T>
            String outer = responseType.substring(0, responseType.indexOf('<'));
            String inner = responseType.substring(
                    responseType.indexOf('<') + 1,
                    responseType.lastIndexOf('>'));

            if ("List".equals(outer) || "Set".equals(outer)) {
                // Collection: emit array schema
                schema = Map.of(
                        "type", "array",
                        "items", Map.of("$ref", "#/components/schemas/" + inner));
            } else {
                // Wrapper: embed generic inside base schema via allOf
                Object innerSchema;
                // Handle inner generics recursively
                if (inner.contains("<") && inner.endsWith(">")) {
                    String io = inner.substring(0, inner.indexOf('<'));
                    String ii = inner.substring(inner.indexOf('<') + 1, inner.lastIndexOf('>'));
                    if ("List".equals(io) || "Set".equals(io)) {
                        innerSchema = Map.of(
                                "type", "array",
                                "items", Map.of("$ref", "#/components/schemas/" + ii));
                    } else {
                        innerSchema = Map.of("$ref", "#/components/schemas/" + io);
                    }
                } else {
                    innerSchema = "?".equals(inner)
                            ? Map.of("type", "object")
                            : Map.of("$ref", "#/components/schemas/" + inner);
                }
                Map<String, Object> genericExtension = Map.of(
                        "properties", Map.of("data", innerSchema));
                schema = Map.of(
                        "allOf", List.of(
                                Map.of("$ref", "#/components/schemas/" + outer),
                                genericExtension));
            }
        } else if (responseType != null && !responseType.isBlank()) {
            // Simple type reference
            schema = Map.of("$ref", "#/components/schemas/" + responseType);
        } else {
            // Fallback to generic object
            schema = Map.of("type", "object");
        }

        return Map.of(
                "200", Map.of(
                        "description", "Successful Response",
                        "content", Map.of(
                                "application/json", Map.of("schema", schema))));
    }

}