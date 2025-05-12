package com.autodoc;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import com.autodoc.parser.ControllerParser;
import com.autodoc.parser.ModelParser;
import com.autodoc.parser.ParsedProject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.utils.SourceRoot;

public class AutodocApplication {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java -jar autodoc.jar <source_directory>");
            System.exit(1);
        }

        try {
            Path sourcePath = new File(args[0]).toPath();
            ParsedProject parsedProject = new ParsedProject();

            // Parse all compilation units
            SourceRoot sourceRoot = new SourceRoot(sourcePath);
            List<CompilationUnit> units = sourceRoot.tryToParse().stream()
                    .filter(r -> r.isSuccessful() && r.getResult().isPresent())
                    .map(r -> r.getResult().get())
                    .toList();

            // Extract models
            ModelParser modelParser = new ModelParser();
            for (CompilationUnit cu : units) {
                modelParser.parseModelClasses(cu, parsedProject);
            }

            // Extract controllers/endpoints
            ControllerParser controllerParser = new ControllerParser();
            for (CompilationUnit cu : units) {
                controllerParser.parseControllerClasses(cu, parsedProject);
            }

            // Serialize IR as JSON
            ObjectMapper mapper = new ObjectMapper()
                    .enable(SerializationFeature.INDENT_OUTPUT);
            if (args.length >= 2) {
                mapper.writeValue(new File(args[1]), parsedProject);
            } else {
                mapper.writeValue(System.out, parsedProject);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
