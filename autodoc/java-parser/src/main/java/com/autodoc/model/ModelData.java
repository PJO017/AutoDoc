package com.autodoc.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModelData {
    private String name;
    private String description;
    private List<FieldData> fields = new ArrayList<>();
    private boolean isInterface = false;
    private boolean isEnum = false;
    private List<String> extendsList = new ArrayList<>();
    private List<String> implementsList = new ArrayList<>();
    private String example;
    private boolean deprecated = false;
    private String deprecationNotes;
    private String since;
    private Map<String, Object> extensions = new HashMap<>();

    public ModelData(String name, String description, List<FieldData> fields) {
        this.name = name;
        this.description = description;
        this.fields = fields;
    }

    public boolean isInterface() {
        return isInterface;
    }

    public void setInterface(boolean isInterface) {
        this.isInterface = isInterface;
    }

    public boolean isEnum() {
        return isEnum;
    }

    public void setEnum(boolean isEnum) {
        this.isEnum = isEnum;
    }

    public List<String> getExtendsList() {
        return extendsList;
    }

    public void setExtendsList(List<String> extendsList) {
        this.extendsList = extendsList;
    }

    public List<String> getImplementsList() {
        return implementsList;
    }

    public void setImplementsList(List<String> implementsList) {
        this.implementsList = implementsList;
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

    public String getSince() {
        return since;
    }

    public void setSince(String since) {
        this.since = since;
    }

    public Map<String, Object> getExtensions() {
        return extensions;
    }

    public void setExtensions(Map<String, Object> extensions) {
        this.extensions = extensions;
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
