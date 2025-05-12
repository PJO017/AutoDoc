package com.autodoc.model;

import java.util.ArrayList;
import java.util.List;

public class TypeRefData {
    String base;
    private List<TypeRefData> args = new ArrayList<>();

    public String getBase() {
        return base;
    }

    public void setBase(String base) {
        this.base = base;
    }

    public List<TypeRefData> getArgs() {
        return args;
    }

    public void setArgs(List<TypeRefData> args) {
        this.args = (args != null ? args : new ArrayList<>());
    }
}