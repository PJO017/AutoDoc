package generator

import (
	"autodoc/parser"
	"strings"
)

// BuildOpenAPISpec composes an OpenAPI 3.0 spec from the IR.
func BuildOpenAPISpec(ir parser.IR, info map[string]string, servers []map[string]string) map[string]interface{} {
	schemas := buildSchemas(ir.Models)
	return map[string]interface{}{
		"openapi":    "3.0.0",
		"info":       info,
		"servers":    servers,
		"paths":      buildPaths(ir.Endpoints),
		"components": map[string]interface{}{"schemas": schemas},
	}
}

// buildSchemas generates component schemas from models, treating pure enums specially.
func buildSchemas(models []parser.ModelData) map[string]interface{} {
	// 1) collect all real model names
	known := make(map[string]struct{}, len(models))
	for _, m := range models {
		known[m.Name] = struct{}{}
	}

	comps := make(map[string]interface{}, len(models))
	for _, m := range models {
		// --- ENUM DETECTION: if model is marked as enum or every field has no TypeRef.Base, treat as enum
		isEnum := m.IsEnum || (len(m.Fields) > 0 && func() bool {
			for _, f := range m.Fields {
				if f.TypeRef.Base != "" {
					return false
				}
			}
			return true
		}())

		if isEnum {
			vals := make([]interface{}, len(m.Fields))
			for i, f := range m.Fields {
				vals[i] = f.Name
			}

			// Create enum schema with additional info
			enumSchema := map[string]interface{}{
				"type": "string",
				"enum": vals,
			}

			// Add description if available
			if m.Description != "" {
				enumSchema["description"] = m.Description
			}

			// Add deprecation if applicable
			if m.Deprecated {
				enumSchema["deprecated"] = true
			}

			comps[m.Name] = enumSchema
			continue
		}

		// --- OBJECT SCHEMA for non‐enums
		props := make(map[string]interface{}, len(m.Fields))
		required := []string{}

		for _, f := range m.Fields {
			// Build the property schema
			schema := buildPropertySchema(f, known)

			// Track required fields
			if f.Required {
				required = append(required, f.Name)
			}

			props[f.Name] = schema
		}

		// Build the model schema
		modelSchema := map[string]interface{}{
			"type":       "object",
			"properties": props,
		}

		// Add required fields array if any
		if len(required) > 0 {
			modelSchema["required"] = required
		}

		// Add description if available
		if m.Description != "" {
			modelSchema["description"] = m.Description
		}

		// Add example if available
		if m.Example != "" {
			modelSchema["example"] = m.Example
		}

		// Add deprecation if applicable
		if m.Deprecated {
			modelSchema["deprecated"] = true
		}

		comps[m.Name] = modelSchema
	}

	return comps
}

// buildPropertySchema creates an OpenAPI schema for a property/field
func buildPropertySchema(f parser.FieldData, known map[string]struct{}) map[string]interface{} {
	var schema map[string]interface{}

	// a) Handle generic collections
	if f.TypeRef.Base == "List" || f.TypeRef.Base == "Set" || f.TypeRef.Base == "Array" {
		inner := f.TypeRef.Args[0]
		schema = map[string]interface{}{
			"type":  "array",
			"items": schemaFor(inner, known),
		}

		// b) Primitive or known type
	} else if t := mapJavaType(f.TypeRef.Base); t != "" {
		schema = map[string]interface{}{"type": t}

		// c) Model reference
	} else if _, isModel := known[f.TypeRef.Base]; isModel {
		schema = map[string]interface{}{
			"$ref": "#/components/schemas/" + f.TypeRef.Base,
		}

		// d) Unknown, use string as default
	} else {
		schema = map[string]interface{}{"type": "string"}
	}

	// Add description if available
	if f.Description != "" {
		schema["description"] = f.Description
	}

	// Add example if available
	if f.Example != "" {
		schema["example"] = f.Example
	}

	// Add deprecation if applicable
	if f.Deprecated {
		schema["deprecated"] = true
	}

	// Add validation rules if any
	for k, v := range f.ValidationRules {
		// Map Java validation annotations to OpenAPI schema validation
		switch k {
		case "minLength", "maxLength", "pattern", "format", "minimum", "maximum":
			schema[k] = v
		}
	}

	return schema
}

// helper to handle nested generics & references with validation support
func schemaFor(r parser.TypeRefData, known map[string]struct{}) interface{} {
	if (r.Base == "List" || r.Base == "Set" || r.Base == "Array") && len(r.Args) > 0 {
		return map[string]interface{}{
			"type":  "array",
			"items": schemaFor(r.Args[0], known),
		}
	}
	if t := mapJavaType(r.Base); t != "" {
		return map[string]interface{}{"type": t}
	}
	return map[string]interface{}{"$ref": "#/components/schemas/" + r.Base}
}

// buildParameters handles primitives vs model references for path/query params.
func buildParameters(params []parser.Parameter) []map[string]interface{} {
	out := make([]map[string]interface{}, 0, len(params))
	for _, p := range params {
		// figure out primitive vs model
		schema := make(map[string]interface{})
		switch t := p.TypeRef.Base; t {
		case "int", "Integer", "long", "Long":
			schema["type"] = "integer"
		case "double", "Double", "float", "Float":
			schema["type"] = "number"
		case "boolean", "Boolean":
			schema["type"] = "boolean"
		case "String", "char", "Character", "string", "java.lang.String":
			schema["type"] = "string"
		case "LocalDateTime":
			schema["type"] = "string"
			schema["format"] = "date-time"
		default:
			schema["$ref"] = "#/components/schemas/" + t
		}

		out = append(out, map[string]interface{}{
			"name":     p.Name,
			"in":       p.In,
			"required": p.Required,
			"schema":   schema,
		})
	}
	return out
}

// buildRequestBody creates requestBody object for non-GET endpoints.
func buildRequestBody(req *parser.TypeRefData) map[string]interface{} {
	return map[string]interface{}{
		"required": true,
		"content": map[string]interface{}{
			"application/json": map[string]interface{}{
				"schema": map[string]interface{}{"$ref": "#/components/schemas/" + req.Base},
			},
		},
	}
}

// buildResponses now inlines List<T> and wraps ApiResponse<T> via allOf. :contentReference[oaicite:2]{index=2}:contentReference[oaicite:3]{index=3}
func buildResponses(r parser.TypeRefData) map[string]interface{} {
	var schema map[string]interface{}

	switch {
	// direct List<T> or Set<T>
	case r.Base == "List" || r.Base == "Set":
		schema = resolveSchema(r)

	// wrapper types like ApiResponse<T>
	case len(r.Args) > 0:
		schema = map[string]interface{}{
			"allOf": []interface{}{
				map[string]interface{}{"$ref": "#/components/schemas/" + r.Base},
				map[string]interface{}{
					"properties": map[string]interface{}{
						"data": resolveSchema(r.Args[0]),
					},
				},
			},
		}

	// simple reference (model or primitive)
	default:
		schema = resolveSchema(r)
	}

	return map[string]interface{}{
		"200": map[string]interface{}{
			"description": "Successful Response",
			"content": map[string]interface{}{
				"application/json": map[string]interface{}{
					"schema": schema,
				},
			},
		},
	}
}

// resolveSchema is recursive: it inlines List/Set as arrays,
// maps primitives, and falls back to $ref for models. :contentReference[oaicite:0]{index=0}:contentReference[oaicite:1]{index=1}
func resolveSchema(r parser.TypeRefData) map[string]interface{} {
	// 1) Collections → inline arrays
	if (r.Base == "List" || r.Base == "Set") && len(r.Args) > 0 {
		return map[string]interface{}{
			"type":  "array",
			"items": resolveSchema(r.Args[0]),
		}
	}
	// 2) Primitives
	if t := mapJavaType(r.Base); t != "" {
		return map[string]interface{}{"type": t}
	}
	// 3) Model reference
	return map[string]interface{}{"$ref": "#/components/schemas/" + r.Base}
}

// buildPaths assembles all path items and operations.
func buildPaths(eps []parser.EndpointData) map[string]interface{} {
	paths := map[string]interface{}{}
	for _, ep := range eps {
		op := map[string]interface{}{}
		
		// Add basic operation data
		op["parameters"] = buildParameters(ep.Parameters)
		if ep.RequestBodyType != nil && strings.ToUpper(ep.Method) != "GET" {
			op["requestBody"] = buildRequestBody(ep.RequestBodyType)
		}
		op["responses"] = buildResponses(ep.ResponseType)
		
		// Add description if available
		if ep.Description != "" {
			op["description"] = ep.Description
		}
		
		// Add summary if available
		if ep.Summary != "" {
			op["summary"] = ep.Summary
		}
		
		// Add tags if available
		if len(ep.Tags) > 0 {
			op["tags"] = ep.Tags
		}
		
		// Add deprecated flag if applicable
		if ep.Deprecated {
			op["deprecated"] = true
		}

		m := strings.ToLower(ep.Method)
		item, _ := paths[ep.Path].(map[string]interface{})
		if item == nil {
			item = map[string]interface{}{}
		}
		item[m] = op
		paths[ep.Path] = item
	}
	return paths
}

// mapJavaType returns OpenAPI types for Java primitives/wrappers.
func mapJavaType(javaType string) string {
	switch javaType {
	case "int", "Integer", "long", "Long", "short", "Short", "byte", "Byte":
		return "integer"
	case "double", "Double", "float", "Float":
		return "number"
	case "boolean", "Boolean":
		return "boolean"
	case "String", "string", "char", "Character", "java.lang.String":
		return "string"
	case "LocalDateTime", "Date", "LocalDate", "Instant":
		return "string"
	default:
		return ""
	}
}
