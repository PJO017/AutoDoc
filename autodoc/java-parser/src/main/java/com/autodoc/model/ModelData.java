package com.autodoc.model;

import java.util.List;

import java.util.ArrayList;

public class ModelData {
    private String name;
    private String packageName;
    private List<FieldData> fields;

    public ModelData(String name, String packageName) {
        this.name = name;
        this.packageName = packageName;
        this.fields = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public String getPackageName() {
        return packageName;
    }

    public List<FieldData> getFields() {
        return fields;
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
                ", packageName='" + packageName + '\'' +
                ", fields=" + fields +
                '}';
    }
}
