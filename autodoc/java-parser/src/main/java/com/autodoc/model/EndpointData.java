package com.autodoc.model;

import java.util.ArrayList;
import java.util.List;

public class EndpointData {
    private String path;
    private String method;
    private String summary;
    private String description;
    private List<String> tags;
    private List<ParameterData> parameters;
    private TypeRefData requestBodyType;
    private TypeRefData responseType;
    private String controllerName;
    private String controllerPackage;
    private List<DependencyData> dependencies = new ArrayList<>();
    private boolean deprecated;

    public EndpointData() {
    }

    public EndpointData(String path,
            String method,
            String summary,
            String description,
            List<String> tags,
            List<ParameterData> parameters,
            TypeRefData requestBodyType,
            TypeRefData responseType,
            String controllerName,
            String controllerPackage,
            List<DependencyData> dependencies) {
        this.path = path;
        this.method = method;
        this.summary = summary;
        this.description = description;
        this.tags = tags;
        this.parameters = parameters;
        this.requestBodyType = requestBodyType;
        this.responseType = responseType;
        this.controllerName = controllerName;
        this.controllerPackage = controllerPackage;
        this.dependencies = dependencies;
    }

    // Original getters and setters
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public List<ParameterData> getParameters() {
        return parameters;
    }

    public void setParameters(List<ParameterData> parameters) {
        this.parameters = parameters;
    }

    public TypeRefData getRequestBodyType() {
        return requestBodyType;
    }

    public void setRequestBodyType(TypeRefData requestBodyType) {
        this.requestBodyType = requestBodyType;
    }

    public TypeRefData getResponseType() {
        return responseType;
    }

    public void setResponseType(TypeRefData responseType) {
        this.responseType = responseType;
    }
    
    // New getters and setters
    public String getControllerName() {
        return controllerName;
    }

    public void setControllerName(String controllerName) {
        this.controllerName = controllerName;
    }

    public String getControllerPackage() {
        return controllerPackage;
    }

    public void setControllerPackage(String controllerPackage) {
        this.controllerPackage = controllerPackage;
    }

    public List<DependencyData> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<DependencyData> dependencies) {
        this.dependencies = dependencies;
    }
    
    public boolean isDeprecated() {
        return deprecated;
    }

    public void setDeprecated(boolean deprecated) {
        this.deprecated = deprecated;
    }
}