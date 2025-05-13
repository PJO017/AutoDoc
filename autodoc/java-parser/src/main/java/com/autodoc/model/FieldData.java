package com.autodoc.model;

import java.util.HashMap;
import java.util.Map;

public class FieldData {
    private String name;
    private TypeRefData typeRef;
    private boolean required;
    private String description;
    private Map<String, Object> validationRules = new HashMap<>();
    private String example;
    private boolean deprecated = false;
    private String deprecationNotes;

    public FieldData() {
    }

    public FieldData(String name,
            TypeRefData typeRef,
            boolean required,
            String description) {
        this.name = name;
        this.typeRef = typeRef;
        this.required = required;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TypeRefData getTypeRef() {
        return typeRef;
    }

    public void setTypeRef(TypeRefData typeRef) {
        this.typeRef = typeRef;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, Object> getValidationRules() {
        return validationRules;
    }

    public void setValidationRules(Map<String, Object> validationRules) {
        this.validationRules = validationRules;
    }

    public String getExample() {
        return example;
    }

    public void setExample(String example) {
        this.example = example;
    }

    public boolean isDeprecated() {
        return deprecated;
    }

    public void setDeprecated(boolean deprecated) {
        this.deprecated = deprecated;
    }

    public String getDeprecationNotes() {
        return deprecationNotes;
    }

    public void setDeprecationNotes(String deprecationNotes) {
        this.deprecationNotes = deprecationNotes;
    }

    @Override
    public String toString() {
        return "FieldData{" +
                "name='" + name + '\'' +
                ", typeRef=" + typeRef +
                '}';
    }
}
