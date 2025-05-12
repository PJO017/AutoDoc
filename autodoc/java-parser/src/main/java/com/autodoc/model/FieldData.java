package com.autodoc.model;

import com.github.javaparser.ast.expr.AnnotationExpr;

import java.util.ArrayList;
import java.util.List;

public class FieldData {
    private String name;
    private String type;
    private List<String> annotations;

    public FieldData(String name, String type) {
        this.name = name;
        this.type = type;
        this.annotations = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public List<String> getAnnotations() {
        return annotations;
    }

    public void setAnnotations(List<AnnotationExpr> annotationExprs) {
        for (AnnotationExpr expr : annotationExprs) {
            annotations.add(expr.getNameAsString());
        }
    }

    @Override
    public String toString() {
        return "FieldData{" +
                "name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", annotations=" + annotations +
                '}';
    }
}
