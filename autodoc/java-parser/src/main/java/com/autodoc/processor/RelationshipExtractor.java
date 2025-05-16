package com.autodoc.processor;

import com.autodoc.model.Relationship;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.*;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.filter.TypeFilter;

import java.util.*;

public class RelationshipExtractor {

    public static final String RELATIONSHIP_INJECTS = "INJECTS";
    public static final String RELATIONSHIP_CALLS = "CALLS";
    public static final String RELATIONSHIP_EXTENDS = "EXTENDS";
    public static final String RELATIONSHIP_IMPLEMENTS = "IMPLEMENTS";

    public List<Relationship> extractRelationships(CtModel model) {
        List<Relationship> relationships = new ArrayList<>();

        // Extract different types of relationships
        relationships.addAll(extractDependencyInjections(model));
        relationships.addAll(extractInheritanceRelationships(model));

        return relationships;
    }

    private List<Relationship> extractDependencyInjections(CtModel model) {
        List<Relationship> relationships = new ArrayList<>();

        // Find all fields with dependency injection annotations
        List<CtField<?>> fields = model.getElements(new TypeFilter<>(CtField.class));

        for (CtField<?> field : fields) {
            boolean isDependency = field.getAnnotations().stream()
                    .anyMatch(a -> {
                        String annoName = a.getAnnotationType().getSimpleName();
                        return annoName.equals("Autowired") ||
                                annoName.equals("Inject") ||
                                annoName.equals("Resource");
                    });

            if (isDependency) {
                CtType<?> declaringType = field.getDeclaringType();

                if (isComponent(declaringType) && isComponent(field.getType().getTypeDeclaration())) {
                    Relationship relationship = new Relationship();
                    relationship.setSourceClass(declaringType.getQualifiedName());
                    relationship.setTargetClass(field.getType().getQualifiedName());
                    relationship.setType(RELATIONSHIP_INJECTS);
                    relationship.setName(field.getSimpleName());
                    relationship.setInjectionType("field");

                    relationships.add(relationship);
                }
            }
        }

        // Find constructor injections
        List<CtConstructor<?>> constructors = model.getElements(new TypeFilter<>(CtConstructor.class));

        for (CtConstructor<?> constructor : constructors) {
            CtType<?> declaringType = constructor.getDeclaringType();

            // Skip if not a component
            if (!isComponent(declaringType))
                continue;

            boolean isInjectionConstructor = constructor.getAnnotations().stream()
                    .anyMatch(a -> a.getAnnotationType().getSimpleName().equals("Autowired"))
                    || constructor.getParameters().size() > 0;

            if (isInjectionConstructor) {
                for (CtParameter<?> param : constructor.getParameters()) {
                    if (isComponent(param.getType().getTypeDeclaration())) {
                        Relationship relationship = new Relationship();
                        relationship.setSourceClass(declaringType.getQualifiedName());
                        relationship.setTargetClass(param.getType().getQualifiedName());
                        relationship.setType(RELATIONSHIP_INJECTS);
                        relationship.setName(param.getSimpleName());
                        relationship.setInjectionType("constructor");

                        relationships.add(relationship);
                    }
                }
            }
        }

        return relationships;
    }

    private List<Relationship> extractInheritanceRelationships(CtModel model) {
        List<Relationship> relationships = new ArrayList<>();

        // Process class inheritance
        List<CtClass<?>> classes = model.getElements(new TypeFilter<>(CtClass.class));

        for (CtClass<?> cls : classes) {
            // Skip anonymous and inner classes
            if (cls.isAnonymous() || cls.isLocalType())
                continue;

            // Process extends relationship
            if (cls.getSuperclass() != null) {
                CtTypeReference<?> superClass = cls.getSuperclass();

                Relationship relationship = new Relationship();
                relationship.setSourceClass(cls.getQualifiedName());
                relationship.setTargetClass(superClass.getQualifiedName());
                relationship.setType(RELATIONSHIP_EXTENDS);

                relationships.add(relationship);
            }

            // Process implements relationships
            for (CtTypeReference<?> iface : cls.getSuperInterfaces()) {
                Relationship relationship = new Relationship();
                relationship.setSourceClass(cls.getQualifiedName());
                relationship.setTargetClass(iface.getQualifiedName());
                relationship.setType(RELATIONSHIP_IMPLEMENTS);

                relationships.add(relationship);
            }
        }

        // Process interface extension
        List<CtInterface<?>> interfaces = model.getElements(new TypeFilter<>(CtInterface.class));

        for (CtInterface<?> iface : interfaces) {
            for (CtTypeReference<?> superIface : iface.getSuperInterfaces()) {
                Relationship relationship = new Relationship();
                relationship.setSourceClass(iface.getQualifiedName());
                relationship.setTargetClass(superIface.getQualifiedName());
                relationship.setType(RELATIONSHIP_EXTENDS);

                relationships.add(relationship);
            }
        }

        return relationships;
    }

    private boolean isComponent(CtType<?> type) {
        if (type == null)
            return false;

        // Check component annotations
        boolean hasComponentAnnotation = type.getAnnotations().stream()
                .anyMatch(a -> {
                    String annoName = a.getAnnotationType().getSimpleName();
                    return annoName.equals("Component") ||
                            annoName.equals("Service") ||
                            annoName.equals("Repository") ||
                            annoName.equals("Controller") ||
                            annoName.equals("RestController");
                });

        if (hasComponentAnnotation)
            return true;

        // Check naming conventions
        String name = type.getSimpleName();
        return name.endsWith("Service") ||
                name.endsWith("Repository") ||
                name.endsWith("Dao") ||
                name.endsWith("Controller");
    }
}