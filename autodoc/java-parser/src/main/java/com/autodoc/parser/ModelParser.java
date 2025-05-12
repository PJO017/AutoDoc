package com.autodoc.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.autodoc.model.FieldData;
import com.autodoc.model.ModelData;
import com.autodoc.model.TypeRefData;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.javadoc.Javadoc;

public class ModelParser {

    private String extractDescription(ClassOrInterfaceDeclaration cls) {
        // 1) Try @Schema or @ApiModel
        String schemaDescription = cls.getAnnotationByName("Schema")
                .flatMap(a -> a.asSingleMemberAnnotationExpr().getMemberValue().toLiteralExpr())
                .map(lit -> lit.asStringLiteralExpr().getValue())
                .orElse(null);

        if (schemaDescription != null) {
            return schemaDescription;
        }

        // 2) Fallback to JavaDoc
        schemaDescription = cls.getJavadoc()
                .map(Javadoc::getDescription)
                .map(desc -> desc.toText().trim())
                .orElse(null);

        if (schemaDescription != null) {
            return schemaDescription;
        }
        // 3) Fallback to class name
        return "";
    }

    public void parseModelClasses(CompilationUnit compilationUnit, ParsedProject parsedProject) {

        // Get all the classes in the current CompilationUnit
        List<ClassOrInterfaceDeclaration> clsList = compilationUnit
                .findAll(ClassOrInterfaceDeclaration.class);

        // Get all enums in the current CompilationUnit
        List<EnumDeclaration> enumList = compilationUnit
                .findAll(EnumDeclaration.class);

        for (ClassOrInterfaceDeclaration cls : clsList) {
            // Skip abstract or interface classes
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
            String description = extractDescription(cls);

            // Process each field (for now, just extracting field names and types)
            List<FieldData> fields = new ArrayList<>();
            for (FieldDeclaration fd : cls.getFields()) {
                for (VariableDeclarator var : fd.getVariables()) {
                    FieldData f = new FieldData();
                    f.setName(var.getNameAsString());
                    f.setRequired(fd.isAnnotationPresent("NotNull"));
                    f.setDescription(
                            fd.getJavadoc().map(j -> j.getDescription().toText()).orElse(""));
                    // capture raw type-ref
                    TypeRefData tr = new TypeRefData();
                    tr.setArgs(new ArrayList<>());
                    Type t = var.getType();
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
                    } else {
                        tr.setBase(t.asString());
                    }
                    f.setTypeRef(tr);

                    fields.add(f);
                }
            }

            // Create ModelData object
            ModelData modelData = new ModelData(name, description, fields);

            // Add the model to the parsed project
            parsedProject.addModel(modelData);
        }

        // Process enums
        for (EnumDeclaration enumDecl : enumList) {

            String name = enumDecl.getNameAsString();
            String description = enumDecl.getJavadoc()
                    .map(j -> j.getDescription().toText().trim())
                    .orElse("");

            // Each enum constant as a FieldData (or you can create a special EnumValueData)
            List<FieldData> values = new ArrayList<>();
            for (EnumConstantDeclaration constant : enumDecl.getEntries()) {
                FieldData f = new FieldData();
                f.setName(constant.getNameAsString());
                f.setDescription(constant.getJavadoc()
                        .map(j -> j.getDescription().toText().trim())
                        .orElse(""));
                // Optionally set type info if needed
                values.add(f);
            }

            ModelData modelData = new ModelData(name, description, values);
            parsedProject.addModel(modelData);
        }
    }

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
                || pkg.contains("config")
                || pkg.contains("controller")
                || pkg.contains("util")
                || pkg.contains("handler");
    }

    // Check if the class has model-related annotations
    private boolean hasModelAnnotation(ClassOrInterfaceDeclaration classDecl) {
        List<AnnotationExpr> annotations = classDecl.getAnnotations();

        // Check for annotations like @Entity, @Data, @JsonProperty, etc.
        return annotations.stream()
                .anyMatch(ann -> List
                        .of("Entity", "Data", "Table", "JsonProperty", "JsonInclude", "Schema", "ApiModel")
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
