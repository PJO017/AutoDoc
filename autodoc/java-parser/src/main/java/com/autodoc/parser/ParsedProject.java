package com.autodoc.parser;

import java.util.ArrayList;
import java.util.List;

import com.autodoc.model.EndpointData;
import com.autodoc.model.ModelData;

public class ParsedProject {

    private List<EndpointData> endpoints;
    private List<ModelData> models;  // Updated from List<String>
    private List<String> components;

    public ParsedProject() {
        this.endpoints = new ArrayList<>();
        this.models = new ArrayList<>();
        this.components = new ArrayList<>();
    }

    public void addEndpoint(EndpointData endpoint) {
        this.endpoints.add(endpoint);
    }

    public void addModel(ModelData model) {
        this.models.add(model);  // Updated from String
    }

    public void addComponent(String component) {
        this.components.add(component);
    }

    public List<EndpointData> getEndpoints() {
        return endpoints;
    }

    public List<ModelData> getModels() {
        return models;
    }

    public List<String> getComponents() {
        return components;
    }

    @Override
    public String toString() {
        return "ParsedProject{" +
                "endpoints=" + endpoints +
                ", models=" + models +
                ", components=" + components +
                '}';
    }
}
