package com.autodoc.parser;

import com.autodoc.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import spoon.Launcher;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtType;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class SpoonParser {
    
    private final Launcher spoon;
    
    public SpoonParser() {
        this.spoon = new Launcher();
        configureSpoon();
    }
    
    private void configureSpoon() {
        // Configure Spoon environment
        spoon.getEnvironment().setComplianceLevel(11); // Java 11 compliance
        spoon.getEnvironment().setCommentEnabled(true); // Process JavaDoc
        spoon.getEnvironment().setAutoImports(true);
    }
    
    public ParsedProject parse(String sourcePath) {
        // Build Spoon model
        spoon.addInputResource(sourcePath);
        spoon.buildModel();
        CtModel model = spoon.getModel();
        
        // Create parsed project
        ParsedProject parsedProject = new ParsedProject();
        
        // Process models
        SpoonModelProcessor modelProcessor = new SpoonModelProcessor();
        modelProcessor.processModels(model, parsedProject);
        
        // Process controllers
        SpoonControllerProcessor controllerProcessor = new SpoonControllerProcessor();
        controllerProcessor.processControllers(model, parsedProject);
        
        return parsedProject;
    }
    
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java -jar autodoc.jar <source_directory>");
            System.exit(1);
        }
        
        try {
            // Create parser
            SpoonParser parser = new SpoonParser();
            
            // Parse source directory
            ParsedProject parsedProject = parser.parse(args[0]);
            
            // Serialize IR as JSON (same as current implementation)
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
