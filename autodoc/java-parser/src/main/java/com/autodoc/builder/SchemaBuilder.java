package com.autodoc.builder;

import com.autodoc.model.ModelData;
import com.autodoc.model.FieldData;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SchemaBuilder {
    private final Map<String, Object> components = new LinkedHashMap<>();

    public void buildSchemas(List<ModelData> models) {
        for (ModelData model : models) {
            Map<String, Object> schema = new LinkedHashMap<>();
            schema.put("type", "object");
            Map<String, Object> props = new LinkedHashMap<>();
            for (FieldData field : model.getFields()) {
                props.put(field.getName(), Map.of("type", mapJavaType(field.getType())));
            }
            schema.put("properties", props);
            components.put(model.getName(), schema);
        }
    }

    public Map<String, Object> getComponents() {
        return Map.copyOf(components);
    }

    private String mapJavaType(String javaType) {
        switch (javaType) {
            case "int":
            case "Integer":
                return "integer";
            case "long":
            case "Long":
                return "integer";
            case "double":
            case "Double":
            case "float":
            case "Float":
                return "number";
            case "boolean":
            case "Boolean":
                return "boolean";
            default:
                return "string";
        }
    }
}
