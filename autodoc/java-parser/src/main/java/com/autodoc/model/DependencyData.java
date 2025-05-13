package com.autodoc.model;

public class DependencyData {
    private String name;
    private String type;
    private String injectionType; // "field" or "constructor"

    public DependencyData() {
    }

    public DependencyData(String name, String type, String injectionType) {
        this.name = name;
        this.type = type;
        this.injectionType = injectionType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getInjectionType() {
        return injectionType;
    }

    public void setInjectionType(String injectionType) {
        this.injectionType = injectionType;
    }
}

