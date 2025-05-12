package com.autodoc.model;

import java.util.List;

public class EndpointData {

    private String endpoint;
    private String httpMethod;
    private List<String> parameters;
    private String requestBody;
    private String responseType;

    // Constructor
    public EndpointData(String endpoint, String httpMethod, List<String> parameters, String requestBody, String responseType) {
        this.endpoint = endpoint;
        this.httpMethod = httpMethod;
        this.parameters = parameters;
        this.requestBody = requestBody;
        this.responseType = responseType;
    }

    // Getters and Setters
    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public List<String> getParameters() {
        return parameters;
    }

    public void setParameters(List<String> parameters) {
        this.parameters = parameters;
    }

    public String getRequestBody() {
        return requestBody;
    }

    public void setRequestBody(String requestBody) {
        this.requestBody = requestBody;
    }

    public String getResponseType() {
        return responseType;
    }

    public void setResponseType(String responseType) {
        this.responseType = responseType;
    }

    @Override
    public String toString() {
        return "EndpointData{" +
                "endpoint='" + endpoint + '\'' +
                ", httpMethod='" + httpMethod + '\'' +
                ", parameters=" + parameters +
                ", requestBody='" + requestBody + '\'' +
                ", responseType='" + responseType + '\'' +
                '}';
    }
}
