package com.autodoc.model;

public class ParameterData {
    private String name;
    private String in;
    private boolean required;
    private String description;
    TypeRefData type;

    public ParameterData() {
    }

    public ParameterData(String name,
            String in,
            boolean required,
            String description,
            TypeRefData type) {
        this.name = name;
        this.in = in;
        this.required = required;
        this.description = description;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIn() {
        return in;
    }

    public void setIn(String in) {
        this.in = in;
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

    public TypeRefData getType() {
        return type;
    }

    public void setType(TypeRefData type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "ParameterData{" +
                "name='" + name + '\'' +
                ", in='" + in + '\'' +
                ", required=" + required +
                ", description='" + description + '\'' +
                ", type=" + type +
                '}';
    }

}
