package com.autodoc;

import com.autodoc.model.*;
import com.autodoc.processor.ControllerProcessor;
import com.autodoc.processor.ModelProcessor;
import com.autodoc.processor.RelationshipExtractor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import spoon.Launcher;
import spoon.reflect.CtModel;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Parser {

    private final Launcher spoon;

    public Parser() {
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
        ModelProcessor modelProcessor = new ModelProcessor();
        modelProcessor.processModels(model, parsedProject);

        // Process controllers
        ControllerProcessor controllerProcessor = new ControllerProcessor();
        controllerProcessor.processControllers(model, parsedProject);

        // Extract relationships between components
        RelationshipExtractor relationshipExtractor = new RelationshipExtractor();
        List<Relationship> relationships = relationshipExtractor.extractRelationships(model);

        // Add relationships to endpoints
        addRelationshipsToEndpoints(parsedProject, relationships);

        return parsedProject;
    }

    private void addRelationshipsToEndpoints(ParsedProject project, List<Relationship> relationships) {
        // Group dependency relationships by source class
        Map<String, List<DependencyData>> dependenciesByClass = new HashMap<>();

        for (Relationship rel : relationships) {
            if (rel.getType().equals(RelationshipExtractor.RELATIONSHIP_INJECTS)) {
                String sourceClass = rel.getSourceClass();

                if (!dependenciesByClass.containsKey(sourceClass)) {
                    dependenciesByClass.put(sourceClass, new ArrayList<>());
                }

                DependencyData dependency = new DependencyData();
                dependency.setName(rel.getName());
                dependency.setType(rel.getTargetClass());
                dependency.setInjectionType(rel.getInjectionType());

                dependenciesByClass.get(sourceClass).add(dependency);
            }
        }

        // Now update endpoints with dependencies
        for (EndpointData endpoint : project.getEndpoints()) {
            String controllerName = endpoint.getControllerName();
            String controllerPackage = endpoint.getControllerPackage();
            String qualifiedName = controllerPackage + "." + controllerName;

            if (dependenciesByClass.containsKey(qualifiedName)) {
                endpoint.setDependencies(dependenciesByClass.get(qualifiedName));
            }
        }
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java -jar autodoc.jar <source_directory>");
            System.exit(1);
        }

        try {
            // Create parser
            Parser parser = new Parser();

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
