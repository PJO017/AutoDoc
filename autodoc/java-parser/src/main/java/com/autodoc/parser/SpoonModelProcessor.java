package com.autodoc.parser;

import com.autodoc.model.*;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.*;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.filter.TypeFilter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SpoonModelProcessor {

    public void processModels(CtModel model, ParsedProject parsedProject) {
        // Find all classes in the model
        List<CtClass<?>> classes = model.getElements(new TypeFilter<>(CtClass.class));

        for (CtClass<?> cls : classes) {
            // Skip anonymous and inner classes
            if (cls.isAnonymous() || cls.isLocalType())
                continue;

            // Skip non-model classes
            if (!isModelClass(cls))
                continue;

            // Process the model
            ModelData modelData = extractModelData(cls);
            parsedProject.addModel(modelData);
        }

        // Process enums as models
        List<CtEnum<?>> enums = model.getElements(new TypeFilter<>(CtEnum.class));
        for (CtEnum<?> enumCls : enums) {
            ModelData modelData = extractEnumData(enumCls);
            parsedProject.addModel(modelData);
        }
    }

    private boolean isModelClass(CtClass<?> cls) {
        // Skip abstract or interface classes
        if (cls.isInterface() || cls.isAbstract()) {
            return false;
        }

        // Skip classes in non-relevant packages
        if (isServiceClass(cls)) {
            return false;
        }

        // Check for model annotations
        if (hasModelAnnotation(cls)) {
            return true;
        }

        // Check if in model package
        if (isInModelPackage(cls)) {
            return true;
        }

        // Check if record or enum
        if (cls.isEnum() || cls.isEnum()) {
            return true;
        }

        return false;
    }

    private boolean isServiceClass(CtClass<?> cls) {
        String pkg = cls.getPackage().getQualifiedName().toLowerCase();

        return pkg.contains("service")
                || pkg.contains("repository")
                || pkg.contains("repo")
                || pkg.contains("config")
                || pkg.contains("controller")
                || pkg.contains("util")
                || pkg.contains("handler");
    }

    private boolean hasModelAnnotation(CtClass<?> cls) {
        List<String> modelAnnotations = Arrays.asList(
                "Entity", "Data", "Table", "JsonProperty", "JsonInclude",
                "Schema", "ApiModel", "Document", "Embeddable", "Value");

        return cls.getAnnotations().stream()
                .anyMatch(ann -> modelAnnotations.contains(ann.getAnnotationType().getSimpleName()));
    }

    private boolean isInModelPackage(CtClass<?> cls) {
        String packageName = cls.getPackage().getQualifiedName().toLowerCase();

        return packageName.contains("model") ||
                packageName.contains("dto") ||
                packageName.contains("entity") ||
                packageName.contains("domain");
    }

    private ModelData extractModelData(CtClass<?> cls) {
        // Create model data
        ModelData modelData = new ModelData(cls.getSimpleName(), "", new ArrayList<>());

        // Process model annotations
        processModelAnnotations(cls, modelData);

        // Process inheritance
        processInheritance(cls, modelData);

        // Process JavaDoc
        if (cls.getDocComment() != null) {
            modelData.setDescription(cls.getDocComment());
        }

        // Process fields
        for (CtField<?> field : cls.getFields()) {
            // Skip static and final fields
            if (field.isStatic() || field.isFinal()) {
                continue;
            }

            FieldData fieldData = extractFieldData(field);
            modelData.addField(fieldData);
        }

        return modelData;
    }

    private ModelData extractEnumData(CtEnum<?> enumCls) {
        // Create model data for enum
        ModelData modelData = new ModelData(enumCls.getSimpleName(), "", new ArrayList<>());
        modelData.setEnum(true);

        // Process JavaDoc
        if (enumCls.getDocComment() != null) {
            modelData.setDescription(enumCls.getDocComment());
        }

        // Process enum values
        List<FieldData> fields = new ArrayList<>();
        for (CtEnumValue<?> value : enumCls.getEnumValues()) {
            FieldData fieldData = new FieldData();
            fieldData.setName(value.getSimpleName());

            // Process JavaDoc for the enum value
            if (value.getDocComment() != null) {
                fieldData.setDescription(value.getDocComment());
            }

            fields.add(fieldData);
        }

        modelData.setFields(fields);
        return modelData;
    }

    private void processModelAnnotations(CtClass<?> cls, ModelData modelData) {
        for (CtAnnotation<?> annotation : cls.getAnnotations()) {
            String annoName = annotation.getAnnotationType().getSimpleName();

            switch (annoName) {
                case "Entity":
                    // Handle @Entity annotation
                    modelData.getExtensions().put("isEntity", true);
                    break;
                case "Table":
                    // Extract table name if present
                    annotation.getValues().forEach((key, value) -> {
                        if (key.equals("name")) {
                            modelData.getExtensions().put("tableName", value.toString());
                        }
                    });
                    break;
                case "ApiModel":
                case "Schema":
                    // Extract description from annotation
                    annotation.getValues().forEach((key, value) -> {
                        if (key.equals("description")) {
                            modelData.setDescription(value.toString().replace("\"", ""));
                        } else if (key.equals("example")) {
                            modelData.setExample(value.toString().replace("\"", ""));
                        }
                    });
                    break;
                case "Deprecated":
                    modelData.setDeprecated(true);
                    // Extract since value if present
                    annotation.getValues().forEach((key, value) -> {
                        if (key.equals("since")) {
                            modelData.setSince(value.toString().replace("\"", ""));
                        }
                    });
                    break;
            }
        }
    }

    private void processInheritance(CtClass<?> cls, ModelData modelData) {
        // Process extends relationship
        if (cls.getSuperclass() != null) {
            modelData.getExtendsList().add(cls.getSuperclass().getQualifiedName());
        }

        // Process implements relationships
        for (CtTypeReference<?> iface : cls.getSuperInterfaces()) {
            modelData.getImplementsList().add(iface.getQualifiedName());
        }
    }

    private FieldData extractFieldData(CtField<?> field) {
        FieldData fieldData = new FieldData();
        fieldData.setName(field.getSimpleName());

        // Process field JavaDoc
        if (field.getDocComment() != null) {
            fieldData.setDescription(field.getDocComment());
        }

        // Process field annotations for validation
        boolean required = false;
        for (CtAnnotation<?> annotation : field.getAnnotations()) {
            String annoName = annotation.getAnnotationType().getSimpleName();

            switch (annoName) {
                case "NotNull":
                case "NotBlank":
                case "NotEmpty":
                    required = true;
                    fieldData.getValidationRules().put("required", true);
                    break;
                case "Size":
                    annotation.getValues().forEach((key, value) -> {
                        if (key.equals("min")) {
                            // Extract the integer value instead of the Spoon element
                            try {
                                int minValue = Integer.parseInt(value.toString().replaceAll("[^0-9]", ""));
                                fieldData.getValidationRules().put("minLength", minValue);
                            } catch (NumberFormatException e) {
                                // Handle parsing error
                            }
                        } else if (key.equals("max")) {
                            try {
                                int maxValue = Integer.parseInt(value.toString().replaceAll("[^0-9]", ""));
                                fieldData.getValidationRules().put("maxLength", maxValue);
                            } catch (NumberFormatException e) {
                                // Handle parsing error
                            }
                        }
                    });
                    break;
                case "Min":
                    annotation.getValues().forEach((key, value) -> {
                        if (key.equals("value")) {
                            try {
                                double minValue = Double.parseDouble(value.toString().replaceAll("[^0-9.]", ""));
                                fieldData.getValidationRules().put("minimum", minValue);
                            } catch (NumberFormatException e) {
                                // Handle parsing error
                            }
                        }
                    });
                    break;
                case "Max":
                    annotation.getValues().forEach((key, value) -> {
                        if (key.equals("value")) {
                            try {
                                double maxValue = Double.parseDouble(value.toString().replaceAll("[^0-9.]", ""));
                                fieldData.getValidationRules().put("maximum", maxValue);
                            } catch (NumberFormatException e) {
                                // Handle parsing error
                            }
                        }
                    });
                    break;
                case "Pattern":
                    annotation.getValues().forEach((key, value) -> {
                        if (key.equals("regexp")) {
                            // Extract string and clean it
                            String pattern = value.toString();
                            if (pattern.startsWith("\"") && pattern.endsWith("\"")) {
                                pattern = pattern.substring(1, pattern.length() - 1);
                            }
                            fieldData.getValidationRules().put("pattern", pattern);
                        }
                    });
                    break;
                case "Email":
                    fieldData.getValidationRules().put("format", "email");
                    break;
                case "Deprecated":
                    fieldData.setDeprecated(true);
                    break;
                case "Schema":
                case "ApiModelProperty":
                    annotation.getValues().forEach((key, value) -> {
                        if (key.equals("description") || key.equals("value")) {
                            String description = value.toString();
                            if (description.startsWith("\"") && description.endsWith("\"")) {
                                description = description.substring(1, description.length() - 1);
                            }
                            fieldData.setDescription(description);
                        } else if (key.equals("example")) {
                            String example = value.toString();
                            if (example.startsWith("\"") && example.endsWith("\"")) {
                                example = example.substring(1, example.length() - 1);
                            }
                            fieldData.setExample(example);
                        } else if (key.equals("required")) {
                            boolean isRequired = Boolean.parseBoolean(value.toString());
                            fieldData.setRequired(isRequired);
                            if (isRequired) {
                                fieldData.getValidationRules().put("required", true);
                            }
                        }
                    });
                    break;
            }
        }

        fieldData.setRequired(required);

        // Extract type information
        TypeRefData typeRef = typeRefFrom(field.getType());
        fieldData.setTypeRef(typeRef);

        return fieldData;
    }

    private TypeRefData typeRefFrom(CtTypeReference<?> type) {
        TypeRefData tr = new TypeRefData();

        tr.setBase(type.getSimpleName());

        // Process generic type arguments
        if (!type.getActualTypeArguments().isEmpty()) {
            List<TypeRefData> args = new ArrayList<>();
            for (CtTypeReference<?> argType : type.getActualTypeArguments()) {
                if (!argType.getSimpleName().equals("?")) { // Skip wildcards
                    args.add(typeRefFrom(argType));
                }
            }
            tr.setArgs(args);
        } else {
            tr.setArgs(new ArrayList<>());
        }

        return tr;
    }
}
