package com.autodoc;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class AutoDocParser {

    public static class MethodInfo {
        String name;
        List<String> annotations;
        List<String> parameters;
    }

    public static class ClassInfo {
        String name;
        List<String> annotations;
        List<MethodInfo> methods;
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage: java AutoDocParser <JavaSourceFile>");
            System.exit(1);
        }

        String filePath = args[0];
        File sourceFile = new File(filePath);

        JavaParser parser = new JavaParser();
        ParseResult<CompilationUnit> result = null;
        try {
            result = parser.parse(sourceFile);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        List<ClassInfo> classInfos = new ArrayList<>();

        if (result.getResult().isPresent()) {
            CompilationUnit cu = result.getResult().get();

            cu.findAll(ClassOrInterfaceDeclaration.class).forEach(clazz -> {
                ClassInfo classInfo = new ClassInfo();
                classInfo.name = clazz.getNameAsString();
                classInfo.annotations = new ArrayList<>();
                classInfo.methods = new ArrayList<>();

                for (AnnotationExpr annotation : clazz.getAnnotations()) {
                    classInfo.annotations.add(annotation.getNameAsString());
                }

                clazz.getMethods().forEach(method -> {
                    MethodInfo methodInfo = new MethodInfo();
                    methodInfo.name = method.getNameAsString();
                    methodInfo.annotations = new ArrayList<>();
                    methodInfo.parameters = new ArrayList<>();

                    for (AnnotationExpr annotation : method.getAnnotations()) {
                        methodInfo.annotations.add(annotation.getNameAsString());
                    }

                    method.getParameters().forEach(param -> {
                        methodInfo.parameters.add(param.getTypeAsString() + " " + param.getNameAsString());
                    });

                    classInfo.methods.add(methodInfo);
                });

                classInfos.add(classInfo);
            });
        }

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        System.out.println(gson.toJson(classInfos));
    }
}
