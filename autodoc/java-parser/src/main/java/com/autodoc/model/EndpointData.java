package com.autodoc.model;

import java.util.List;

public class EndpointData {
    private String path;
    private String method;
    private String summary;
    private String description;
    private List<String> tags;
    private List<ParameterData> parameters;
    TypeRefData requestBodyType;
    TypeRefData responseType;

    public EndpointData() {
    }

    public EndpointData(String path,
            String method,
            String summary,
            String description,
            List<String> tags,
            List<ParameterData> parameters,
            TypeRefData requestBodyType,
            TypeRefData responseType) {
        this.path = path;
        this.method = method;
        this.summary = summary;
        this.description = description;
        this.tags = tags;
        this.parameters = parameters;
        this.requestBodyType = requestBodyType;
        this.responseType = responseType;
    }

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
}