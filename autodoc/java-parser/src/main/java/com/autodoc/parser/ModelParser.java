package com.autodoc.parser;

import com.autodoc.model.FieldData;
import com.autodoc.model.ModelData;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;

import java.util.List;

public class ModelParser {

    public void parseModelClasses(CompilationUnit compilationUnit, ParsedProject parsedProject) {
        // Get all the classes in the current CompilationUnit
        List<ClassOrInterfaceDeclaration> classDeclarations = compilationUnit
                .findAll(ClassOrInterfaceDeclaration.class);

        for (ClassOrInterfaceDeclaration classDecl : classDeclarations) {
            // Skip abstract or interface classes
            if (classDecl.isInterface() || classDecl.isAbstract()) {
                continue;
            }

            // Skip classes in non-relevant packages (e.g., services, configurations)
            if (isServiceClass(classDecl)) {
                continue;
            }

            // Check for model annotations
            boolean isModel = hasModelAnnotation(classDecl) || isInModelPackage(classDecl);

            if (!isModel) {
                continue; // Skip non-model classes
            }

            // Extract the package name
            String packageName = classDecl.getParentNode()
                    .map(parent -> parent.toString()) // Get the package path
                    .orElse("");

            // Create ModelData object
            ModelData modelData = new ModelData(classDecl.getNameAsString(), packageName);

            // Process each field (for now, just extracting field names and types)
            List<FieldDeclaration> fields = classDecl.getFields();
            for (FieldDeclaration field : fields) {
                field.getVariables().forEach(variable -> {
                    // Create FieldData objects for each field
                    FieldData fieldData = new FieldData(variable.getNameAsString(), variable.getTypeAsString());
                    modelData.addField(fieldData);
                });
            }

            // Add the model to the parsed project
            parsedProject.addModel(modelData);
        }
    }

    // Check if the class is in a non-model package
    private boolean isServiceClass(ClassOrInterfaceDeclaration classDecl) {
        String packageName = classDecl.getParentNode()
                .map(parent -> parent.toString()) // Get the package path
                .orElse("");

        // Filter based on package names
        return packageName.contains("service") || packageName.contains("repository") || packageName.contains("config");
    }

    // Check if the class has model-related annotations
    private boolean hasModelAnnotation(ClassOrInterfaceDeclaration classDecl) {
        List<AnnotationExpr> annotations = classDecl.getAnnotations();

        // Check for annotations like @Entity, @Data, @JsonProperty, etc.
        return annotations.stream()
                .anyMatch(ann -> List.of("Entity", "Data", "Table", "JsonProperty", "JsonInclude")
                        .contains(ann.getNameAsString()));
    }

    // Check if the class is in a model-related package (heuristic)
    private boolean isInModelPackage(ClassOrInterfaceDeclaration classDecl) {
        String packageName = classDecl.getParentNode()
                .map(parent -> parent.toString()) // Get the package path
                .orElse("");

        // Heuristic: Assume classes in these packages are models/DTOs
        return packageName.contains("model") || packageName.contains("dto");
    }
}
