package com.autodoc.parser;

import com.autodoc.model.EndpointData;
import com.autodoc.model.ParameterData;
import com.autodoc.model.TypeRefData;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ControllerParser {
    private static final List<String> MAPPINGS = List.of(
            "GetMapping", "PostMapping", "PutMapping", "DeleteMapping", "PatchMapping", "RequestMapping");

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

                // Tags from @Tag on class
                List<String> tags = new ArrayList<>();
                cls.getAnnotationByName("Tag")
                        .flatMap(a -> a instanceof SingleMemberAnnotationExpr
                                ? Optional.of(((SingleMemberAnnotationExpr) a).getMemberValue().toString())
                                : Optional.empty())
                        .ifPresent(tags::add);

                // Parameters
                List<ParameterData> params = new ArrayList<>();
                for (Parameter p : m.getParameters()) {
                    if (p.isAnnotationPresent("PathVariable")) {
                        params.add(buildParam(p, "path", true));
                    } else if (p.isAnnotationPresent("RequestParam")) {
                        boolean req = p.getAnnotationByName("RequestParam")
                                .map(a -> ((NormalAnnotationExpr) a).getPairs().stream()
                                        .filter(pv -> pv.getNameAsString().equals("required"))
                                        .map(MemberValuePair::getValue)
                                        .findFirst()
                                        .map(v -> v.asBooleanLiteralExpr().getValue())
                                        .orElse(false))
                                .orElse(false);
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

                parsed.addEndpoint(ep);
            }
        }
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
