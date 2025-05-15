package com.autodoc.parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.autodoc.model.FieldData;
import com.autodoc.model.ModelData;
import com.autodoc.model.TypeRefData;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.javadoc.Javadoc;
import com.github.javaparser.javadoc.JavadocBlockTag;

public class ModelParser {

    // Define a functional interface for annotation handlers
    @FunctionalInterface
    private interface AnnotationHandler {
        void handle(AnnotationExpr annotation, ModelData modelData);
    }

    private Map<String, AnnotationHandler> modelAnnotationHandlers = new HashMap<>();
    private Map<String, FieldAnnotationHandler> fieldAnnotationHandlers = new HashMap<>();

    @FunctionalInterface
    private interface FieldAnnotationHandler {
        void handle(AnnotationExpr annotation, FieldData fieldData);
    }

    public ModelParser() {
        // Initialize annotation handlers
        initializeAnnotationHandlers();
    }

    private void initializeAnnotationHandlers() {
        // Model annotation handlers
        modelAnnotationHandlers.put("Entity", this::handleEntityAnnotation);
        modelAnnotationHandlers.put("Table", this::handleTableAnnotation);
        modelAnnotationHandlers.put("ApiModel", this::handleApiModelAnnotation);
        modelAnnotationHandlers.put("Schema", this::handleSchemaAnnotation);
        modelAnnotationHandlers.put("Deprecated", this::handleDeprecatedAnnotation);

        // Field annotation handlers
        fieldAnnotationHandlers.put("NotNull", this::handleNotNullAnnotation);
        fieldAnnotationHandlers.put("NotBlank", this::handleNotBlankAnnotation);
        fieldAnnotationHandlers.put("NotEmpty", this::handleNotEmptyAnnotation);
        fieldAnnotationHandlers.put("Size", this::handleSizeAnnotation);
        fieldAnnotationHandlers.put("Min", this::handleMinAnnotation);
        fieldAnnotationHandlers.put("Max", this::handleMaxAnnotation);
        fieldAnnotationHandlers.put("Pattern", this::handlePatternAnnotation);
        fieldAnnotationHandlers.put("Email", this::handleEmailAnnotation);
        fieldAnnotationHandlers.put("Deprecated", this::handleFieldDeprecatedAnnotation);
        fieldAnnotationHandlers.put("Schema", this::handleFieldSchemaAnnotation);
        fieldAnnotationHandlers.put("ApiModelProperty", this::handleApiModelPropertyAnnotation);
    }

    // Annotation handlers for models
    private void handleEntityAnnotation(AnnotationExpr annotation, ModelData modelData) {
        // Handle @Entity annotation
        modelData.getExtensions().put("isEntity", true);
    }

    private void handleTableAnnotation(AnnotationExpr annotation, ModelData modelData) {
        // Extract table name if present
        if (annotation.isNormalAnnotationExpr()) {
            annotation.asNormalAnnotationExpr().getPairs().stream()
                    .filter(p -> p.getNameAsString().equals("name"))
                    .findFirst()
                    .ifPresent(p -> modelData.getExtensions().put("tableName",
                            p.getValue().toString().replace("\"", "")));
        }
    }

    private void handleApiModelAnnotation(AnnotationExpr annotation, ModelData modelData) {
        // Extract description and other properties
        if (annotation.isNormalAnnotationExpr()) {
            annotation.asNormalAnnotationExpr().getPairs().stream()
                    .filter(p -> p.getNameAsString().equals("description"))
                    .findFirst()
                    .ifPresent(p -> modelData.setDescription(
                            p.getValue().toString().replace("\"", "")));

            // Extract example if present
            annotation.asNormalAnnotationExpr().getPairs().stream()
                    .filter(p -> p.getNameAsString().equals("example"))
                    .findFirst()
                    .ifPresent(p -> modelData.setExample(
                            p.getValue().toString().replace("\"", "")));
        }
    }

    private void handleSchemaAnnotation(AnnotationExpr annotation, ModelData modelData) {
        // Similar to ApiModel but with Schema annotation
        if (annotation.isNormalAnnotationExpr()) {
            annotation.asNormalAnnotationExpr().getPairs().stream()
                    .filter(p -> p.getNameAsString().equals("description"))
                    .findFirst()
                    .ifPresent(p -> modelData.setDescription(
                            p.getValue().toString().replace("\"", "")));

            // Extract example if present
            annotation.asNormalAnnotationExpr().getPairs().stream()
                    .filter(p -> p.getNameAsString().equals("example"))
                    .findFirst()
                    .ifPresent(p -> modelData.setExample(
                            p.getValue().toString().replace("\"", "")));
        }
    }

    private void handleDeprecatedAnnotation(AnnotationExpr annotation, ModelData modelData) {
        modelData.setDeprecated(true);
        // If it has reason or since, extract them
        if (annotation.isNormalAnnotationExpr()) {
            annotation.asNormalAnnotationExpr().getPairs().stream()
                    .filter(p -> p.getNameAsString().equals("since"))
                    .findFirst()
                    .ifPresent(p -> modelData.setSince(
                            p.getValue().toString().replace("\"", "")));
        }
    }

    // Annotation handlers for fields
    private void handleNotNullAnnotation(AnnotationExpr annotation, FieldData fieldData) {
        fieldData.setRequired(true);
        fieldData.getValidationRules().put("required", true);
    }

    private void handleNotBlankAnnotation(AnnotationExpr annotation, FieldData fieldData) {
        fieldData.setRequired(true);
        fieldData.getValidationRules().put("required", true);
        fieldData.getValidationRules().put("notBlank", true);
    }

    private void handleNotEmptyAnnotation(AnnotationExpr annotation, FieldData fieldData) {
        fieldData.setRequired(true);
        fieldData.getValidationRules().put("required", true);
        fieldData.getValidationRules().put("notEmpty", true);
    }

    private void handleSizeAnnotation(AnnotationExpr annotation, FieldData fieldData) {
        if (annotation.isNormalAnnotationExpr()) {
            annotation.asNormalAnnotationExpr().getPairs().stream()
                    .filter(p -> p.getNameAsString().equals("min"))
                    .findFirst()
                    .ifPresent(p -> fieldData.getValidationRules().put("minLength",
                            Integer.parseInt(p.getValue().toString())));

            annotation.asNormalAnnotationExpr().getPairs().stream()
                    .filter(p -> p.getNameAsString().equals("max"))
                    .findFirst()
                    .ifPresent(p -> fieldData.getValidationRules().put("maxLength",
                            Integer.parseInt(p.getValue().toString())));
        }
    }

    private void handleMinAnnotation(AnnotationExpr annotation, FieldData fieldData) {
        if (annotation.isSingleMemberAnnotationExpr()) {
            fieldData.getValidationRules().put("minimum",
                    Integer.parseInt(annotation.asSingleMemberAnnotationExpr().getMemberValue().toString()));
        }
    }

    private void handleMaxAnnotation(AnnotationExpr annotation, FieldData fieldData) {
        if (annotation.isSingleMemberAnnotationExpr()) {
            fieldData.getValidationRules().put("maximum",
                    Integer.parseInt(annotation.asSingleMemberAnnotationExpr().getMemberValue().toString()));
        }
    }

    private void handlePatternAnnotation(AnnotationExpr annotation, FieldData fieldData) {
        if (annotation.isNormalAnnotationExpr()) {
            annotation.asNormalAnnotationExpr().getPairs().stream()
                    .filter(p -> p.getNameAsString().equals("regexp"))
                    .findFirst()
                    .ifPresent(p -> fieldData.getValidationRules().put("pattern",
                            p.getValue().toString().replace("\"", "")));
        }
    }

    private void handleEmailAnnotation(AnnotationExpr annotation, FieldData fieldData) {
        fieldData.getValidationRules().put("format", "email");
    }

    private void handleFieldDeprecatedAnnotation(AnnotationExpr annotation, FieldData fieldData) {
        fieldData.setDeprecated(true);
    }

    private void handleFieldSchemaAnnotation(AnnotationExpr annotation, FieldData fieldData) {
        if (annotation.isNormalAnnotationExpr()) {
            annotation.asNormalAnnotationExpr().getPairs().stream()
                    .filter(p -> p.getNameAsString().equals("description"))
                    .findFirst()
                    .ifPresent(p -> fieldData.setDescription(
                            p.getValue().toString().replace("\"", "")));

            // Extract example if present
            annotation.asNormalAnnotationExpr().getPairs().stream()
                    .filter(p -> p.getNameAsString().equals("example"))
                    .findFirst()
                    .ifPresent(p -> fieldData.setExample(
                            p.getValue().toString().replace("\"", "")));
        }
    }

    private void handleApiModelPropertyAnnotation(AnnotationExpr annotation, FieldData fieldData) {
        if (annotation.isNormalAnnotationExpr()) {
            // Extract description
            annotation.asNormalAnnotationExpr().getPairs().stream()
                    .filter(p -> p.getNameAsString().equals("value") ||
                            p.getNameAsString().equals("notes"))
                    .findFirst()
                    .ifPresent(p -> fieldData.setDescription(
                            p.getValue().toString().replace("\"", "")));

            // Extract required
            annotation.asNormalAnnotationExpr().getPairs().stream()
                    .filter(p -> p.getNameAsString().equals("required"))
                    .findFirst()
                    .ifPresent(p -> {
                        boolean isRequired = Boolean.parseBoolean(
                                p.getValue().toString().replace("\"", ""));
                        fieldData.setRequired(isRequired);
                        if (isRequired) {
                            fieldData.getValidationRules().put("required", true);
                        }
                    });

            // Extract example
            annotation.asNormalAnnotationExpr().getPairs().stream()
                    .filter(p -> p.getNameAsString().equals("example"))
                    .findFirst()
                    .ifPresent(p -> fieldData.setExample(
                            p.getValue().toString().replace("\"", "")));
        }
    }

    // Process inheritance relationships
    private void processInheritance(ClassOrInterfaceDeclaration cls, ModelData modelData) {
        // Process extends relationships
        cls.getExtendedTypes().forEach(extType -> {
            modelData.getExtendsList().add(extType.getNameAsString());
        });

        // Process implements relationships
        cls.getImplementedTypes().forEach(implType -> {
            modelData.getImplementsList().add(implType.getNameAsString());
        });
    }

    // Enhanced JavaDoc processing
    private void processJavaDoc(Optional<Javadoc> javadoc, ModelData modelData) {
        javadoc.ifPresent(doc -> {
            // Process description
            modelData.setDescription(doc.getDescription().toText().trim());

            // Process tags
            for (JavadocBlockTag tag : doc.getBlockTags()) {
                switch (tag.getTagName()) {
                    case "example":
                        modelData.setExample(tag.getContent().toText().trim());
                        break;
                    case "deprecated":
                        modelData.setDeprecated(true);
                        modelData.setDeprecationNotes(tag.getContent().toText().trim());
                        break;
                    case "since":
                        modelData.setSince(tag.getContent().toText().trim());
                        break;
                    // Add more tag handlers as needed
                }
            }
        });
    }

    // Process field JavaDoc
    private void processFieldJavaDoc(Optional<Javadoc> javadoc, FieldData fieldData) {
        javadoc.ifPresent(doc -> {
            // Process description
            fieldData.setDescription(doc.getDescription().toText().trim());

            // Process tags
            for (JavadocBlockTag tag : doc.getBlockTags()) {
                switch (tag.getTagName()) {
                    case "example":
                        fieldData.setExample(tag.getContent().toText().trim());
                        break;
                    case "deprecated":
                        fieldData.setDeprecated(true);
                        fieldData.setDeprecationNotes(tag.getContent().toText().trim());
                        break;
                    // Add more tag handlers as needed
                }
            }
        });
    }

    public void parseModelClasses(CompilationUnit compilationUnit, ParsedProject parsedProject) {
        // Get all the classes in the current CompilationUnit
        List<ClassOrInterfaceDeclaration> clsList = compilationUnit
                .findAll(ClassOrInterfaceDeclaration.class);

        // Get all enums in the current CompilationUnit
        List<EnumDeclaration> enumList = compilationUnit
                .findAll(EnumDeclaration.class);

        for (ClassOrInterfaceDeclaration cls : clsList) {
            // Skip abstract or interface classes (for now)
            if (cls.isInterface() || cls.isAbstract()) {
                continue;
            }

            // Skip classes in non-relevant packages (e.g., services, configurations)
            if (isServiceClass(cls)) {
                continue;
            }

            // Check for model annotations
            boolean isModel = hasModelAnnotation(cls) || isInModelPackage(cls) || cls.isEnumDeclaration()
                    || cls.isRecordDeclaration();

            if (!isModel) {
                continue; // Skip non-model classes
            }

            String name = cls.getNameAsString();

            // Create ModelData object
            ModelData modelData = new ModelData(name, "", new ArrayList<>());

            // Process model annotations
            for (AnnotationExpr annotation : cls.getAnnotations()) {
                String annoName = annotation.getNameAsString();
                if (modelAnnotationHandlers.containsKey(annoName)) {
                    modelAnnotationHandlers.get(annoName).handle(annotation, modelData);
                }
            }

            // Process inheritance
            processInheritance(cls, modelData);

            // Process JavaDoc
            processJavaDoc(cls.getJavadoc(), modelData);

            // Process each field
            for (FieldDeclaration fd : cls.getFields()) {
                for (VariableDeclarator var : fd.getVariables()) {
                    FieldData fieldData = new FieldData();
                    fieldData.setName(var.getNameAsString());

                    // Process field annotations
                    for (AnnotationExpr annotation : fd.getAnnotations()) {
                        String annoName = annotation.getNameAsString();
                        if (fieldAnnotationHandlers.containsKey(annoName)) {
                            fieldAnnotationHandlers.get(annoName).handle(annotation, fieldData);
                        }
                    }

                    // Process field JavaDoc
                    processFieldJavaDoc(fd.getJavadoc(), fieldData);

                    // Capture type-ref
                    TypeRefData tr = typeRefFrom(var.getType());
                    fieldData.setTypeRef(tr);

                    modelData.getFields().add(fieldData);
                }
            }

            // Add the model to the parsed project
            parsedProject.addModel(modelData);
        }

        // Process enums
        for (EnumDeclaration enumDecl : enumList) {
            String name = enumDecl.getNameAsString();

            // Create ModelData for enum
            ModelData modelData = new ModelData(name, "", new ArrayList<>());
            modelData.setEnum(true);

            // Process JavaDoc
            enumDecl.getJavadoc().ifPresent(doc -> {
                modelData.setDescription(doc.getDescription().toText().trim());
            });

            // Each enum constant as a FieldData
            List<FieldData> values = new ArrayList<>();
            for (EnumConstantDeclaration constant : enumDecl.getEntries()) {
                FieldData f = new FieldData();
                f.setName(constant.getNameAsString());

                // Process constant JavaDoc
                constant.getJavadoc().ifPresent(doc -> {
                    f.setDescription(doc.getDescription().toText().trim());
                });

                values.add(f);
            }

            modelData.setFields(values);
            parsedProject.addModel(modelData);
        }
    }

    // Add a new method to process interfaces
    public void processInterfaces(CompilationUnit compilationUnit, ParsedProject parsedProject) {
        List<ClassOrInterfaceDeclaration> interfaces = compilationUnit
                .findAll(ClassOrInterfaceDeclaration.class)
                .stream()
                .filter(ClassOrInterfaceDeclaration::isInterface)
                .collect(Collectors.toList());

        for (ClassOrInterfaceDeclaration intf : interfaces) {
            // Skip non-model interfaces
            if (isServiceClass(intf)) {
                continue;
            }

            ModelData modelData = new ModelData(intf.getNameAsString(), "", new ArrayList<>());
            modelData.setInterface(true);

            // Process JavaDoc
            processJavaDoc(intf.getJavadoc(), modelData);

            // Extract methods as potential properties
            for (MethodDeclaration method : intf.getMethods()) {
                // Consider getter methods (no params, returns something)
                if (method.getParameters().isEmpty() && !method.getType().isVoidType()) {
                    String name = method.getNameAsString();
                    if (name.startsWith("get") && name.length() > 3) {
                        String fieldName = name.substring(3, 4).toLowerCase() + name.substring(4);
                        FieldData fieldData = new FieldData();
                        fieldData.setName(fieldName);
                        fieldData.setTypeRef(typeRefFrom(method.getType()));

                        // Process method JavaDoc
                        method.getJavadoc().ifPresent(doc -> {
                            fieldData.setDescription(doc.getDescription().toText().trim());
                        });

                        modelData.getFields().add(fieldData);
                    }
                }
            }

            parsedProject.addModel(modelData);
        }
    }

    // Existing helper methods...
    private TypeRefData typeRefFrom(Type t) {
        TypeRefData tr = new TypeRefData();
        tr.setArgs(new ArrayList<>());
        if (t.isClassOrInterfaceType()) {
            ClassOrInterfaceType cit = t.asClassOrInterfaceType();
            tr.setBase(cit.getNameAsString());
            if (cit.getTypeArguments().isPresent()) {
                List<TypeRefData> args = new ArrayList<>();
                for (Type ta : cit.getTypeArguments().get()) {
                    TypeRefData child = typeRefFrom(ta);
                    if (!"?".equals(child.getBase())) {
                        args.add(child);
                    }
                }
                tr.setArgs(args);
            }
        } else if (t.isArrayType()) {
            // Handle array types specifically
            tr.setBase("Array");
            List<TypeRefData> args = new ArrayList<>();
            args.add(typeRefFrom(t.asArrayType().getComponentType()));
            tr.setArgs(args);
        } else {
            tr.setBase(t.asString());
        }
        return tr;
    }

    // Check if the class is in a non-model package
    private boolean isServiceClass(ClassOrInterfaceDeclaration classDecl) {
        // Climb up to the CompilationUnit
        Optional<CompilationUnit> cu = classDecl.findCompilationUnit();

        // Extract the package name, lower‐cased
        String pkg = cu
                .flatMap(CompilationUnit::getPackageDeclaration)
                .map(pd -> pd.getNameAsString().toLowerCase())
                .orElse("");

        // Now do your simple substring checks on only the package path
        return pkg.contains("service")
                || pkg.contains("repository")
                || pkg.contains("repo")
                || pkg.contains("config")
                || pkg.contains("controller")
                || pkg.contains("util")
                || pkg.contains("handler");
    }

    // Enhanced check for model annotations
    private boolean hasModelAnnotation(ClassOrInterfaceDeclaration classDecl) {
        List<String> modelAnnotations = Arrays.asList(
                "Entity", "Data", "Table", "JsonProperty", "JsonInclude",
                "Schema", "ApiModel", "Document", "Embeddable", "Value");

        return classDecl.getAnnotations().stream()
                .anyMatch(ann -> modelAnnotations.contains(ann.getNameAsString()));
    }

    // Check if the class is in a model-related package (heuristic)
    private boolean isInModelPackage(ClassOrInterfaceDeclaration classDecl) {
        String packageName = classDecl.findCompilationUnit()
                .flatMap(CompilationUnit::getPackageDeclaration)
                .map(pd -> pd.getNameAsString().toLowerCase())
                .orElse("");

        // Heuristic: Assume classes in these packages are models/DTOs
        return packageName.contains("model") ||
                packageName.contains("models") ||
                packageName.contains("dto") ||
                packageName.contains("dtos") ||
                packageName.contains("entity") ||
                packageName.contains("entities") ||
                packageName.contains("domain");
    }
}