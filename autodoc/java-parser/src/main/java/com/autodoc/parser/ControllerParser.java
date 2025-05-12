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

    // Method to parse a single file
    public void parseControllerFile(Path filePath, ParsedProject parsedProject) throws IOException {
        SourceRoot sourceRoot = new SourceRoot(filePath.getParent());
        CompilationUnit cu = sourceRoot.parse("", filePath.getFileName().toString());

        // Traverse classes and methods in the file
        cu.findAll(ClassOrInterfaceDeclaration.class).forEach(clazz -> {
            // Check if the class is a REST controller
            clazz.getAnnotations().forEach(annotation -> {
                if (annotation.getNameAsString().equals("RestController") || annotation.getNameAsString().equals("Controller")) {
                    parseControllerMethods(clazz, parsedProject);
                }
            });
        });
    }

    // Parse the methods in the controller class
    private void parseControllerMethods(ClassOrInterfaceDeclaration clazz, ParsedProject parsedProject) {
        clazz.getMethods().forEach(method -> {
            // Check for endpoint annotations
            method.getAnnotations().forEach(annotation -> {
                if (isRestEndpointAnnotation(annotation)) {
                    String endpoint = extractEndpoint(annotation);
                    String httpMethod = extractHttpMethod(annotation);

                    // Parse method parameters (request params, path variables, etc.)
                    List<String> parameters = parseMethodParameters(method);

                    // Parse request body and response type
                    String requestBody = parseRequestBody(method);
                    String responseType = method.getType().asString();  // Assumes return type is the response type

                    // Store this method in the parsed project
                    parsedProject.addEndpoint(new EndpointData(endpoint, httpMethod, parameters, requestBody, responseType));
                    System.out.println("Endpoint: " + endpoint + " Method: " + httpMethod);
                }
            });
        });
    }

    // Check if the annotation is a valid REST endpoint annotation
    private boolean isRestEndpointAnnotation(AnnotationExpr annotation) {
        return annotation.getNameAsString().matches("GetMapping|PostMapping|PutMapping|DeleteMapping|RequestMapping");
    }

    // Extract the endpoint URL from the annotation
    private String extractEndpoint(AnnotationExpr annotation) {
        // Extract path from the annotation if it's present
        if (annotation.isNormalAnnotationExpr()) {
            annotation.asNormalAnnotationExpr().getPairs().forEach(pair -> {
                if (pair.getNameAsString().equals("value")) {
                    System.out.println("Path: " + pair.getValue().toString());
                }
            });
        }
        return annotation.toString(); // Placeholder for path extraction
    }

    // Extract the HTTP method (GET, POST, etc.) from the annotation
    private String extractHttpMethod(AnnotationExpr annotation) {
        // Extract HTTP method based on the annotation
        if (annotation.getNameAsString().equals("GetMapping")) {
            return "GET";
        } else if (annotation.getNameAsString().equals("PostMapping")) {
            return "POST";
        } else if (annotation.getNameAsString().equals("PutMapping")) {
            return "PUT";
        } else if (annotation.getNameAsString().equals("DeleteMapping")) {
            return "DELETE";
        } else if (annotation.getNameAsString().equals("RequestMapping")) {
            return "REQUEST";
        }
        return "UNKNOWN";  // Default if no known mapping is found
    }

    // Parse method parameters (request parameters, path variables, etc.)
    private List<String> parseMethodParameters(MethodDeclaration method) {
        List<String> parameters = new ArrayList<>();
        method.getParameters().forEach(param -> {
            // Check for @PathVariable or @RequestParam or other annotations
            param.getAnnotations().forEach(annotation -> {
                if (annotation.getNameAsString().equals("PathVariable")) {
                    parameters.add("PathVariable: " + param.getName());
                } else if (annotation.getNameAsString().equals("RequestParam")) {
                    parameters.add("RequestParam: " + param.getName());
                }
            });
        });
        return parameters;
    }

    // Parse request body from method annotations
    private String parseRequestBody(MethodDeclaration method) {
        return method.getAnnotations().stream()
                .filter(annotation -> annotation.getNameAsString().equals("RequestBody"))
                .map(annotation -> "RequestBody: " + method.getNameAsString())
                .findFirst()
                .orElse("No RequestBody");
    }
}
