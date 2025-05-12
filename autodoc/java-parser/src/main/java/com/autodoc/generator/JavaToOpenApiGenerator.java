package com.autodoc.generator;

import com.autodoc.builder.OpenApiBuilder;
import com.autodoc.builder.SchemaBuilder;
import com.autodoc.parser.ControllerParser;
import com.autodoc.parser.ModelParser;
import com.autodoc.parser.ParsedProject;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.utils.SourceRoot;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class JavaToOpenApiGenerator {

    public Map<String, Object> generate(Path sourcePath) throws Exception {
        ParsedProject parsedProject = new ParsedProject();

        // Parse models
        SourceRoot sourceRoot = new SourceRoot(sourcePath);
        ModelParser modelParser = new ModelParser();
        List<com.github.javaparser.ast.CompilationUnit> units = sourceRoot.tryToParse().stream()
                .filter(result -> result.isSuccessful() && result.getResult().isPresent())
                .map(result -> result.getResult().get())
                .toList();
        for (CompilationUnit cu : units) {
            modelParser.parseModelClasses(cu, parsedProject);
        }

        // Parse controllers
        ControllerParser controllerParser = new ControllerParser();
        List<File> files = listJavaFiles(sourcePath.toFile());
        for (File file : files) {
            controllerParser.parseControllerFile(file.toPath(), parsedProject);
        }

        // Generate OpenAPI spec
        SchemaBuilder schemaBuilder = new SchemaBuilder();
        OpenApiBuilder openApiBuilder = new OpenApiBuilder(schemaBuilder);

         return openApiBuilder.buildOpenApiSpec(parsedProject.getEndpoints(), parsedProject.getModels());
    }

    private List<File> listJavaFiles(File dir) {
        File[] files = dir.listFiles();
        if (files == null) return List.of();

        return java.util.Arrays.stream(files)
                .flatMap(file -> file.isDirectory() ? listJavaFiles(file).stream() : java.util.stream.Stream.of(file))
                .filter(file -> file.getName().endsWith(".java"))
                .toList();
    }
}
