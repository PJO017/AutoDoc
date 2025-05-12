package com.autodoc.model;

import java.util.List;

public class ModelData {
    private String name;
    private String description;
    private List<FieldData> fields;

    public ModelData(String name, String description, List<FieldData> fields) {
        this.name = name;
        this.description = description;
        this.fields = fields;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public List<FieldData> getFields() {
        return fields;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setFields(List<FieldData> fields) {
        this.fields = fields;
    }

    public void addField(FieldData field) {
        this.fields.add(field);
    }

    @Override
    public String toString() {
        return "ModelData{" +
                "name='" + name + '\'' +
                ", fields=" + fields +
                '}';
    }
}
