package com.autodoc.model;

import java.util.ArrayList;
import java.util.List;

public class ParsedProject {
    private final List<EndpointData> endpoints = new ArrayList<>();
    private final List<ModelData> models = new ArrayList<>();

    public void addEndpoint(EndpointData endpoint) {
        endpoints.add(endpoint);
    }

    public void addModel(ModelData model) {
        models.add(model);
    }

    public List<EndpointData> getEndpoints() {
        return List.copyOf(endpoints);
    }

    public List<ModelData> getModels() {
        return List.copyOf(models);
    }
}
