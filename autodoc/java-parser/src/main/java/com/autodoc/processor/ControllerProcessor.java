package com.autodoc.processor;


import com.autodoc.model.*;
import spoon.reflect.CtModel;
import spoon.reflect.code.CtExpression;
import spoon.reflect.declaration.*;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.filter.TypeFilter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class ControllerProcessor {

    private static final List<String> MAPPINGS = Arrays.asList(
            "GetMapping", "PostMapping", "PutMapping", "DeleteMapping", "PatchMapping", "RequestMapping");
    
    private static final List<String> DEPENDENCY_ANNOTATIONS = Arrays.asList(
            "Autowired", "Inject", "Resource", "Value");
            
    private static final List<String> SERVICE_SUFFIXES = Arrays.asList(
            "Service", "Manager", "Processor", "Handler", "Delegate", "Provider", "Helper");

    public void processControllers(CtModel model, ParsedProject parsedProject) {
        // Find all classes in the model
        List<CtClass<?>> classes = model.getElements(new TypeFilter<>(CtClass.class));
        
        for (CtClass<?> cls : classes) {
            // Skip non-controller classes
            if (!isControllerClass(cls)) continue;
            
            // Extract endpoints from this controller
            processController(cls, parsedProject);
        }
    }
    
    private boolean isControllerClass(CtClass<?> cls) {
        // Check for controller annotations
        boolean hasControllerAnnotation = cls.getAnnotations().stream()
                .anyMatch(a -> a.getAnnotationType().getSimpleName().equals("RestController") || 
                             a.getAnnotationType().getSimpleName().equals("Controller"));
        
        // Skip exception handlers
        boolean isControllerAdvice = cls.getAnnotations().stream()
                .anyMatch(a -> a.getAnnotationType().getSimpleName().equals("ControllerAdvice"));
        
        return hasControllerAnnotation && !isControllerAdvice;
    }
    
    private void processController(CtClass<?> cls, ParsedProject parsedProject) {
        // Get base path from class-level RequestMapping
        String basePath = "";
        for (CtAnnotation<?> annotation : cls.getAnnotations()) {
            if (annotation.getAnnotationType().getSimpleName().equals("RequestMapping")) {
                basePath = extractPath(annotation);
                break;
            }
        }
        
        // Extract controller metadata
        String controllerName = cls.getSimpleName();
        String controllerPackage = cls.getPackage().getQualifiedName();
        
        // Extract dependencies
        List<DependencyData> dependencies = extractDependencies(cls);
        
        // Process each method with a mapping annotation
        for (CtMethod<?> method : cls.getMethods()) {
            // Find the first mapping annotation
            Optional<CtAnnotation<?>> mappingOpt = method.getAnnotations().stream()
                    .filter(a -> MAPPINGS.contains(a.getAnnotationType().getSimpleName()))
                    .findFirst();
            
            if (!mappingOpt.isPresent()) continue;
            
            CtAnnotation<?> mapping = mappingOpt.get();
            
            // Extract endpoint data
            String httpMethod = deriveHttpMethod(mapping);
            String methodPath = extractPath(mapping);
            String fullPath = combinePaths(basePath, methodPath);
            
            // Extract operation metadata
            String summary = extractOperationMember(method, "summary");
            String description = extractOperationMember(method, "description");
            
            // Extract tags
            List<String> tags = new ArrayList<>();
            cls.getAnnotations().stream()
                    .filter(a -> a.getAnnotationType().getSimpleName().equals("Tag"))
                    .findFirst()
                    .ifPresent(a -> {
                        a.getValues().forEach((key, value) -> {
                            if (key.equals("name") || key.equals("value")) {
                                tags.add(value.toString().replace("\"", ""));
                            }
                        });
                    });
            
            // Extract parameters
            List<ParameterData> parameters = new ArrayList<>();
            for (CtParameter<?> param : method.getParameters()) {
                boolean isPathVariable = param.getAnnotations().stream()
                        .anyMatch(a -> a.getAnnotationType().getSimpleName().equals("PathVariable"));
                
                boolean isRequestParam = param.getAnnotations().stream()
                        .anyMatch(a -> a.getAnnotationType().getSimpleName().equals("RequestParam"));
                
                if (isPathVariable) {
                    parameters.add(buildParam(param, "path", true));
                } else if (isRequestParam) {
                    boolean required = true; // Default is true for RequestParam
                    
                    // Check if required flag is explicitly set
                    for (CtAnnotation<?> anno : param.getAnnotations()) {
                        if (anno.getAnnotationType().getSimpleName().equals("RequestParam")) {
                            for (String key : anno.getValues().keySet()) {
                                if (key.equals("required")) {
                                    CtExpression<?> expr = anno.getValues().get(key);
                                    required = Boolean.parseBoolean(expr.toString());
                                }
                            }
                        }
                    }
                    
                    parameters.add(buildParam(param, "query", required));
                }
            }
            
            // Extract request body type
            TypeRefData requestBodyType = null;
            for (CtParameter<?> param : method.getParameters()) {
                boolean isRequestBody = param.getAnnotations().stream()
                        .anyMatch(a -> a.getAnnotationType().getSimpleName().equals("RequestBody"));
                
                if (isRequestBody) {
                    requestBodyType = typeRefFrom(param.getType());
                    break;
                }
            }
            
            // Extract response type
            TypeRefData responseType = typeRefFrom(method.getType());
            
            // Create endpoint
            EndpointData endpoint = new EndpointData();
            endpoint.setPath(fullPath);
            endpoint.setMethod(httpMethod);
            endpoint.setSummary(summary);
            endpoint.setDescription(description);
            endpoint.setTags(tags);
            endpoint.setParameters(parameters);
            endpoint.setRequestBodyType(requestBodyType);
            endpoint.setResponseType(responseType);
            endpoint.setControllerName(controllerName);
            endpoint.setControllerPackage(controllerPackage);
            endpoint.setDependencies(dependencies);
            
            // Check if deprecated
            boolean deprecated = method.getAnnotations().stream()
                    .anyMatch(a -> a.getAnnotationType().getSimpleName().equals("Deprecated"));
            endpoint.setDeprecated(deprecated);
            
            // Add to parsed project
            parsedProject.addEndpoint(endpoint);
        }
    }
    
    private String extractPath(CtAnnotation<?> annotation) {
        String path = "";
        
        // Try to get from value or path attribute
        for (String key : annotation.getValues().keySet()) {
            if (key.equals("value") || key.equals("path")) {
                CtExpression<?> expr = annotation.getValues().get(key);
                path = expr.toString().replace("\"", "");
                break;
            }
        }
        
        return path;
    }
    
    private String deriveHttpMethod(CtAnnotation<?> annotation) {
        String type = annotation.getAnnotationType().getSimpleName();
        
        switch (type) {
            case "GetMapping":
                return "GET";
            case "PostMapping":
                return "POST";
            case "PutMapping":
                return "PUT";
            case "DeleteMapping":
                return "DELETE";
            case "PatchMapping":
                return "PATCH";
            case "RequestMapping":
                // Check for method attribute
                for (String key : annotation.getValues().keySet()) {
                    if (key.equals("method")) {
                        String methodExpr = annotation.getValues().get(key).toString();
                        // Extract the method from RequestMethod.XXX
                        if (methodExpr.contains(".")) {
                            return methodExpr.substring(methodExpr.lastIndexOf('.') + 1);
                        }
                        return methodExpr;
                    }
                }
                return "GET"; // Default to GET
            default:
                return "GET";
        }
    }
    
    private String extractOperationMember(CtMethod<?> method, String memberName) {
        // Try to get from JavaDoc first
        String javadoc = method.getDocComment();
        if (javadoc != null && !javadoc.trim().isEmpty()) {
            return javadoc.trim();
        }
        
        // Try to get from @Operation annotation
        for (CtAnnotation<?> annotation : method.getAnnotations()) {
            if (annotation.getAnnotationType().getSimpleName().equals("Operation")) {
                for (String key : annotation.getValues().keySet()) {
                    if (key.equals(memberName)) {
                        return annotation.getValues().get(key).toString().replace("\"", "");
                    }
                }
            }
        }
        
        return "";
    }
    
    private ParameterData buildParam(CtParameter<?> param, String in, boolean required) {
        ParameterData paramData = new ParameterData();
        paramData.setName(param.getSimpleName());
        paramData.setIn(in);
        paramData.setRequired(required);
        
        // Extract description from JavaDoc or annotations
        String description = "";
        if (param.getDocComment() != null) {
            description = param.getDocComment();
        }
        paramData.setDescription(description);
        
        // Extract type
        paramData.setType(typeRefFrom(param.getType()));
        
        return paramData;
    }
    
    private TypeRefData typeRefFrom(CtTypeReference<?> type) {
        TypeRefData tr = new TypeRefData();
        
        tr.setBase(type.getSimpleName());
        
        // Process generic type arguments
        if (!type.getActualTypeArguments().isEmpty()) {
            List<TypeRefData> args = new ArrayList<>();
            for (CtTypeReference<?> argType : type.getActualTypeArguments()) {
                if (!argType.getSimpleName().equals("?")) {  // Skip wildcards
                    args.add(typeRefFrom(argType));
                }
            }
            tr.setArgs(args);
        } else {
            tr.setArgs(new ArrayList<>());
        }
        
        return tr;
    }
    
    private String combinePaths(String basePath, String methodPath) {
        if (basePath.isEmpty()) return methodPath;
        if (methodPath.isEmpty()) return basePath;
        
        // Ensure there's a single slash between paths
        if (basePath.endsWith("/") && methodPath.startsWith("/")) {
            return basePath + methodPath.substring(1);
        } else if (!basePath.endsWith("/") && !methodPath.startsWith("/")) {
            return basePath + "/" + methodPath;
        } else {
            return basePath + methodPath;
        }
    }
    
    private List<DependencyData> extractDependencies(CtClass<?> cls) {
        List<DependencyData> dependencies = new ArrayList<>();
        
        // Field injection
        for (CtField<?> field : cls.getFields()) {
            boolean isAnnotated = field.getAnnotations().stream()
                    .anyMatch(a -> DEPENDENCY_ANNOTATIONS.contains(a.getAnnotationType().getSimpleName()));
                    
            if (isAnnotated || isLikelyServiceField(field)) {
                DependencyData dependency = new DependencyData();
                dependency.setName(field.getSimpleName());
                dependency.setType(field.getType().getSimpleName());
                dependency.setInjectionType("field");
                dependencies.add(dependency);
            }
        }
        
        // Constructor injection
        for (CtConstructor<?> constructor : cls.getConstructors()) {
            // Most likely the injection constructor if it has parameters
            if (constructor.getParameters().size() > 0) {
                for (CtParameter<?> param : constructor.getParameters()) {
                    String typeName = param.getType().getSimpleName();
                    String paramName = param.getSimpleName();
                    
                    if (isLikelyService(typeName) || isLikelyService(paramName)) {
                        DependencyData dependency = new DependencyData();
                        dependency.setName(paramName);
                        dependency.setType(typeName);
                        dependency.setInjectionType("constructor");
                        dependencies.add(dependency);
                    }
                }
            }
        }
        
        return dependencies;
    }
    
    private boolean isLikelyServiceField(CtField<?> field) {
        String typeName = field.getType().getSimpleName();
        String fieldName = field.getSimpleName();
        
        return isLikelyService(typeName) || isLikelyService(fieldName);
    }
    
    private boolean isLikelyService(String name) {
        String lowercaseName = name.toLowerCase();
        
        return SERVICE_SUFFIXES.stream().anyMatch(name::endsWith) || 
               lowercaseName.contains("service") || 
               lowercaseName.contains("repository");
    }
}
