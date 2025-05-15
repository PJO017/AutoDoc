package com.autodoc.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.autodoc.model.DependencyData;
import com.autodoc.model.EndpointData;
import com.autodoc.model.ParameterData;
import com.autodoc.model.TypeRefData;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;

public class ControllerParser {
    private static final List<String> MAPPINGS = List.of(
            "GetMapping", "PostMapping", "PutMapping", "DeleteMapping", "PatchMapping", "RequestMapping");
    
    private static final List<String> DEPENDENCY_ANNOTATIONS = List.of(
            "Autowired", "Inject", "Resource", "Value");
            
    private static final List<String> SERVICE_SUFFIXES = List.of(
            "Service", "Manager", "Processor", "Handler", "Delegate", "Provider", "Helper");

    public void parseControllerClasses(CompilationUnit cu, ParsedProject parsed) {
        for (ClassOrInterfaceDeclaration cls : cu.findAll(ClassOrInterfaceDeclaration.class)) {
            if (!cls.isAnnotationPresent("RestController") && !cls.isAnnotationPresent("Controller"))
                continue;
            // skip exception handlers, etc.
            if (cls.isAnnotationPresent("ControllerAdvice"))
                continue;

            String basePath = "";
            if (cls.isAnnotationPresent("RequestMapping")) {
                basePath = extractPath(cls.getAnnotationByName("RequestMapping").get());
            }
            
            // Extract controller name and package
            String controllerName = cls.getNameAsString();
            String controllerPackage = cu.getPackageDeclaration()
                    .map(pd -> pd.getNameAsString())
                    .orElse("");
                    
            // Extract service dependencies
            List<DependencyData> dependencies = extractDependencies(cls);

            for (MethodDeclaration m : cls.getMethods()) {
                Optional<AnnotationExpr> map = m.getAnnotations().stream()
                        .filter(a -> MAPPINGS.contains(a.getNameAsString()))
                        .findFirst();
                if (map.isEmpty())
                    continue;

                AnnotationExpr anno = map.get();
                String method = deriveHttpMethod(anno);
                String path = basePath + extractPath(anno);
                String summary = extractOperationMember(m, "summary");
                String description = extractOperationMember(m, "description");

                // Tags - use the controller's package in this case
                List<String> tags = new ArrayList<>();
                if (!controllerName.isEmpty()) {
                    // Use controller name without "Controller" suffix if present
                    String tag = controllerName;
                    if (tag.endsWith("Controller")) {
                        tag = tag.substring(0, tag.length() - "Controller".length());
                    }
                    tags.add(tag);
                }

                // Parameters
                List<ParameterData> params = new ArrayList<>();
                for (Parameter p : m.getParameters()) {
                    if (p.isAnnotationPresent("PathVariable")) {
                        params.add(buildParam(p, "path", true));
                    } else if (p.isAnnotationPresent("RequestParam")) {
                        // FIX: Properly handle all annotation types for RequestParam
                        boolean req = true; // Default is true for Spring's @RequestParam
                        
                        Optional<AnnotationExpr> requestParamAnnotation = p.getAnnotationByName("RequestParam");
                        if (requestParamAnnotation.isPresent()) {
                            AnnotationExpr annotationExpr = requestParamAnnotation.get();
                            
                            if (annotationExpr instanceof NormalAnnotationExpr) {
                                // If it has named parameters like @RequestParam(required = false)
                                req = ((NormalAnnotationExpr) annotationExpr).getPairs().stream()
                                        .filter(pv -> pv.getNameAsString().equals("required"))
                                        .map(MemberValuePair::getValue)
                                        .findFirst()
                                        .map(v -> {
                                            try {
                                                return v.asBooleanLiteralExpr().getValue();
                                            } catch (Exception e) {
                                                // In case it's not a boolean literal
                                                return true;
                                            }
                                        })
                                        .orElse(true);
                            }
                            // If it's a MarkerAnnotationExpr like @RequestParam, required is true
                            // If it's a SingleMemberAnnotationExpr like @RequestParam("id"), required is true
                        }
                        
                        params.add(buildParam(p, "query", req));
                    }
                }

                // RequestBody type
                TypeRefData requestBodyType = m.getParameters().stream()
                        .filter(p -> p.isAnnotationPresent("RequestBody"))
                        .findFirst()
                        .map(p -> typeRefFrom(p.getType()))
                        .orElse(null);

                // Response type
                TypeRefData responseType = typeRefFrom(m.getType());

                EndpointData ep = new EndpointData();
                ep.setPath(path);
                ep.setMethod(method);
                ep.setSummary(summary);
                ep.setDescription(description);
                ep.setTags(tags);
                ep.setParameters(params);
                ep.setRequestBodyType(requestBodyType);
                ep.setResponseType(responseType);
                ep.setControllerName(controllerName);
                ep.setControllerPackage(controllerPackage);
                ep.setDependencies(dependencies);

                parsed.addEndpoint(ep);
            }
        }
    }
    
    private List<DependencyData> extractDependencies(ClassOrInterfaceDeclaration cls) {
        List<DependencyData> dependencies = new ArrayList<>();
        
        // Check fields with @Autowired, @Inject, etc.
        for (FieldDeclaration field : cls.getFields()) {
            boolean isDependency = field.getAnnotations().stream()
                    .anyMatch(a -> DEPENDENCY_ANNOTATIONS.contains(a.getNameAsString()));
                    
            if (isDependency || isLikelyServiceField(field)) {
                for (var variable : field.getVariables()) {
                    Type type = variable.getType();
                    if (type.isClassOrInterfaceType()) {
                        ClassOrInterfaceType classType = type.asClassOrInterfaceType();
                        String typeName = classType.getNameAsString();
                        String fieldName = variable.getNameAsString();
                        
                        DependencyData dependency = new DependencyData();
                        dependency.setName(fieldName);
                        dependency.setType(typeName);
                        dependency.setInjectionType("field");
                        dependencies.add(dependency);
                    }
                }
            }
        }
        
        // Check constructor injection
        for (ConstructorDeclaration constructor : cls.getConstructors()) {
            // Most likely the injection constructor if it has parameters
            if (constructor.getParameters().size() > 0) {
                for (Parameter param : constructor.getParameters()) {
                    Type type = param.getType();
                    if (type.isClassOrInterfaceType()) {
                        ClassOrInterfaceType classType = type.asClassOrInterfaceType();
                        String typeName = classType.getNameAsString();
                        String paramName = param.getNameAsString();
                        
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
        }
        
        return dependencies;
    }
    
    private boolean isLikelyServiceField(FieldDeclaration field) {
        // Check if field type or name suggests it's a service
        return field.getVariables().stream().anyMatch(v -> 
            isLikelyService(v.getType().asString()) || isLikelyService(v.getNameAsString()));
    }
    
    private boolean isLikelyService(String name) {
        String lowercaseName = name.toLowerCase();
        return SERVICE_SUFFIXES.stream().anyMatch(suffix -> 
            name.endsWith(suffix) || lowercaseName.contains("service") || lowercaseName.contains("repository"));
    }

    private String extractPath(AnnotationExpr a) {
        if (a instanceof SingleMemberAnnotationExpr) {
            return trim(((SingleMemberAnnotationExpr) a).getMemberValue().toString());
        }
        if (a instanceof NormalAnnotationExpr) {
            for (MemberValuePair p : ((NormalAnnotationExpr) a).getPairs()) {
                if (p.getNameAsString().matches("value|path")) {
                    return trim(p.getValue().toString());
                }
            }
        }
        // MarkerAnnotationExpr has no path
        return "";
    }

    private String deriveHttpMethod(AnnotationExpr a) {
        String n = a.getNameAsString();
        return switch (n) {
            case "GetMapping" -> "GET";
            case "PostMapping" -> "POST";
            case "PutMapping" -> "PUT";
            case "DeleteMapping" -> "DELETE";
            case "PatchMapping" -> "PATCH";
            default -> {
                // RequestMapping → check method=…
                if (a instanceof NormalAnnotationExpr nae) {
                    for (MemberValuePair p : nae.getPairs()) {
                        if (p.getNameAsString().equals("method")) {
                            String raw = p.getValue().toString();
                            yield raw.substring(raw.lastIndexOf('.') + 1);
                        }
                    }
                }
                yield "GET";
            }
        };
    }

    private String extractOperationMember(MethodDeclaration m, String name) {
        // Prefer Javadoc if present
        if (m.getJavadoc().isPresent()) {
            String content = m.getJavadoc().get().getDescription().toText().trim();
            if (!content.isEmpty()) {
                return content;
            }
        }
        // Fallback to @Operation annotation
        return m.getAnnotationByName("Operation")
                .filter(a -> a instanceof NormalAnnotationExpr)
                .map(a -> ((NormalAnnotationExpr) a).getPairs().stream()
                        .filter(p -> p.getNameAsString().equals(name))
                        .map(p -> trim(p.getValue().toString()))
                        .findFirst().orElse(""))
                .orElse("");
    }

    private ParameterData buildParam(Parameter p, String in, boolean req) {
        ParameterData pd = new ParameterData();
        pd.setName(p.getNameAsString());
        pd.setIn(in);
        pd.setRequired(req);
        pd.setDescription("");
        pd.setType(typeRefFrom(p.getType()));
        return pd;
    }

    private TypeRefData typeRefFrom(Type t) {
        if (t.isClassOrInterfaceType()) {
            ClassOrInterfaceType cit = t.asClassOrInterfaceType();
            TypeRefData tr = new TypeRefData();
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
            return tr;
        }
        // primitives or void
        TypeRefData tr = new TypeRefData();
        tr.setBase(t.asString());
        return tr;
    }

    private String trim(String s) {
        return s.replaceAll("^\"|\"$", "");
    }
}