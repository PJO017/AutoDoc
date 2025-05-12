package com.autodoc.parser;

import com.autodoc.model.EndpointData;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.utils.SourceRoot;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ControllerParser {

    public void parseControllerFile(Path filePath, ParsedProject parsedProject) throws IOException {
        SourceRoot sourceRoot = new SourceRoot(filePath.getParent());
        CompilationUnit cu = sourceRoot.parse("", filePath.getFileName().toString());
        cu.findAll(ClassOrInterfaceDeclaration.class).forEach(clazz -> {
            if (clazz.getAnnotations().stream()
                    .anyMatch(a -> a.getNameAsString().matches("RestController|Controller"))) {
                // Capture class-level base path from @RequestMapping
                String basePath = clazz.getAnnotations().stream()
                        .filter(a -> a.getNameAsString().equals("RequestMapping"))
                        .findFirst()
                        .map(this::extractEndpoint)
                        .orElse("");
                parseControllerMethods(clazz, basePath, parsedProject);
            }
        });
    }

    private void parseControllerMethods(ClassOrInterfaceDeclaration clazz, String basePath,
            ParsedProject parsedProject) {
        for (MethodDeclaration method : clazz.getMethods()) {
            for (AnnotationExpr annotation : method.getAnnotations()) {
                if (annotation.getNameAsString()
                        .matches("GetMapping|PostMapping|PutMapping|DeleteMapping|RequestMapping")) {
                    // Determine relative path (empty if none specified)
                    String relativePath = extractEndpoint(annotation);
                    // Build full path by combining class and method paths
                    String fullPath;
                    if (relativePath.isEmpty()) {
                        fullPath = basePath;
                    } else if (basePath.endsWith("/")) {
                        fullPath = basePath + (relativePath.startsWith("/") ? relativePath.substring(1) : relativePath);
                    } else {
                        fullPath = basePath + (relativePath.startsWith("/") ? relativePath : "/" + relativePath);
                    }

                    String httpMethod = extractHttpMethod(annotation);
                    List<String> params = parseMethodParameters(method);
                    String requestBody = parseRequestBody(method);
                    String responseType = method.getType().asString();

                    parsedProject.addEndpoint(
                            new EndpointData(fullPath, httpMethod, params, requestBody, responseType));
                }
            }
        }
    }

    private String extractEndpoint(AnnotationExpr annotation) {
        if (annotation.isMarkerAnnotationExpr()) {
            return "";
        } else if (annotation.isSingleMemberAnnotationExpr()) {
            return annotation.asSingleMemberAnnotationExpr()
                    .getMemberValue().asStringLiteralExpr().getValue();
        } else if (annotation.isNormalAnnotationExpr()) {
            return annotation.asNormalAnnotationExpr().getPairs().stream()
                    .filter(p -> p.getNameAsString().matches("value|path"))
                    .findFirst()
                    .map(p -> p.getValue().asStringLiteralExpr().getValue())
                    .orElse("");
        }
        return "";
    }

    private String extractHttpMethod(AnnotationExpr annotation) {
        switch (annotation.getNameAsString()) {
            case "GetMapping":
                return "GET";
            case "PostMapping":
                return "POST";
            case "PutMapping":
                return "PUT";
            case "DeleteMapping":
                return "DELETE";
            default:
                return "GET";
        }
    }

    private List<String> parseMethodParameters(MethodDeclaration method) {
        List<String> parameters = new ArrayList<>();
        method.getParameters().forEach(p -> p.getAnnotations().forEach(a -> {
            String kind = a.getNameAsString();
            if (kind.equals("PathVariable") || kind.equals("RequestParam")) {
                parameters.add(kind + ": " + p.getNameAsString());
            }
        }));
        return parameters;
    }

    private String parseRequestBody(MethodDeclaration method) {
        return method.getAnnotations().stream()
                .filter(a -> a.getNameAsString().equals("RequestBody"))
                .map(a -> method.getNameAsString())
                .findFirst()
                .orElse(null);
    }
}