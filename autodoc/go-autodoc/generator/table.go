package generator

import (
	"fmt"
	"os"
	"sort"
	"strings"

	"autodoc/parser"
)

// GenerateEndpointTable writes a Markdown table of endpoints to the given output path.
func GenerateEndpointTable(ir *parser.IR, outputPath string) error {
	// Collect rows as slices of strings: Controller, Method, Path, Params, Summary
	type row struct{ controller, method, path, params, summary string }
	var rows []row

	// Helper to derive controller/group key

	// Break every path into segments
	paths := make([][]string, len(ir.Endpoints))
	for i, ep := range ir.Endpoints {
		paths[i] = strings.FieldsFunc(ep.Path, func(r rune) bool { return r == '/' })
	}
	common := getCommonPrefix(paths)

	for i, ep := range ir.Endpoints {
		// Determine controller name
		var ctrl string
		if len(ep.Tags) > 0 && ep.Tags[0] != "" {
			ctrl = ep.Tags[0]
		} else if len(paths[i]) > len(common) {
			ctrl = strings.Title(paths[i][len(common)])
		} else {
			ctrl = "Default"
		}

		// Build params string
		var parts []string
		for _, p := range ep.Parameters {
			visitorReq := ""
			if p.Required {
				visitorReq = ", required"
			}
			parts = append(parts, fmt.Sprintf("%s (%s%s)", p.Name, p.In, visitorReq))
		}
		paramStr := ""
		if len(parts) > 0 {
			paramStr = strings.Join(parts, ", ")
		}

		// Use ep.Summary if available
		sum := ep.Summary

		rows = append(rows, row{ctrl, ep.Method, ep.Path, paramStr, sum})
	}

	// Sort rows by controller then path
	sort.Slice(rows, func(i, j int) bool {
		if rows[i].controller != rows[j].controller {
			return rows[i].controller < rows[j].controller
		}
		return rows[i].path < rows[j].path
	})

	// Build Markdown table
	var sb strings.Builder
	sb.WriteString("| Controller | Method | Path | Params | Description |\n")
	sb.WriteString("|------------|--------|------|--------|-------------|\n")
	for _, r := range rows {
		ctrl := r.controller
		method := r.method
		path := r.path
		params := r.params
		summary := r.summary
		if ctrl == "" {
			ctrl = "-"
		}
		if method == "" {
			method = "-"
		}
		if path == "" {
			path = "-"
		}
		if params == "" {
			params = "-"
		}
		if summary == "" {
			summary = "-"
		}
		sb.WriteString(fmt.Sprintf("| %s | %s | `%s` | %s | %s |\n", ctrl, method, path, params, summary))
	}

	// Write file
	err := os.WriteFile(outputPath, []byte(sb.String()), 0644)
	if err != nil {
		return fmt.Errorf("failed to write endpoint table: %w", err)
	}
	return nil
}

// GenerateModelTable writes a Markdown file with a separate table for each model.
func GenerateModelTable(ir *parser.IR, outputPath string) error {
	var sb strings.Builder

	// Sort models by name
	models := make([]parser.ModelData, len(ir.Models))
	copy(models, ir.Models)
	sort.Slice(models, func(i, j int) bool {
		return models[i].Name < models[j].Name
	})

	for _, m := range models {
		name := m.Name
		if name == "" {
			name = "-"
		}

		// Add model heading
		sb.WriteString(fmt.Sprintf("### %s\n\n", name))

		// Add description
		desc := m.Description
		if desc == "" {
			desc = "-"
		}
		sb.WriteString(fmt.Sprintf("_Description_: %s\n\n", desc))

		// Add deprecation notice if applicable
		if m.Deprecated {
			sb.WriteString("**DEPRECATED**")
			if m.DeprecationNotes != "" {
				sb.WriteString(fmt.Sprintf(": %s", m.DeprecationNotes))
			}
			sb.WriteString("\n\n")
		}

		// Add since version if available
		if m.Since != "" {
			sb.WriteString(fmt.Sprintf("_Since_: %s\n\n", m.Since))
		}

		// Add inheritance info if available
		if len(m.ExtendsList) > 0 {
			sb.WriteString(fmt.Sprintf("_Extends_: %s\n\n", strings.Join(m.ExtendsList, ", ")))
		}

		if len(m.ImplementsList) > 0 {
			sb.WriteString(fmt.Sprintf("_Implements_: %s\n\n", strings.Join(m.ImplementsList, ", ")))
		}

		// Add table header
		if m.IsEnum {
			sb.WriteString("| Value | Description |\n")
			sb.WriteString("|-------|-------------|\n")
		} else {
			sb.WriteString("| Field | Type | Required | Description | Validation |\n")
			sb.WriteString("|-------|------|----------|-------------|------------|\n")
		}

		// Add table rows
		if len(m.Fields) == 0 {
			if m.IsEnum {
				sb.WriteString("| - | - |\n")
			} else {
				sb.WriteString("| - | - | - | - | - |\n")
			}
		} else {
			if m.IsEnum {
				for _, f := range m.Fields {
					fieldName := f.Name
					if fieldName == "" {
						fieldName = "-"
					}
					fieldDesc := f.Description
					if fieldDesc == "" {
						fieldDesc = "-"
					}
					sb.WriteString(fmt.Sprintf("| %s | %s |\n", fieldName, fieldDesc))
				}
			} else {
				for _, f := range m.Fields {
					fieldName := f.Name
					if fieldName == "" {
						fieldName = "-"
					}

					// Add deprecated marker
					if f.Deprecated {
						fieldName = fmt.Sprintf("~~%s~~ (deprecated)", fieldName)
					}

					fieldType := f.TypeRef.Base
					if fieldType == "" {
						fieldType = "-"
					}

					// Format generic types
					if len(f.TypeRef.Args) > 0 {
						args := make([]string, len(f.TypeRef.Args))
						for i, arg := range f.TypeRef.Args {
							args[i] = arg.Base
						}
						fieldType = fmt.Sprintf("%s<%s>", fieldType, strings.Join(args, ", "))
					}

					required := "no"
					if f.Required {
						required = "yes"
					}

					fieldDesc := f.Description
					if fieldDesc == "" {
						fieldDesc = "-"
					}

					// Add example if available
					if f.Example != "" {
						fieldDesc = fmt.Sprintf("%s (Example: `%s`)", fieldDesc, f.Example)
					}

					// Format validation rules
					validation := "-"
					if len(f.ValidationRules) > 0 {
						validations := []string{}
						for k, v := range f.ValidationRules {
							validations = append(validations, fmt.Sprintf("%s: %v", k, v))
						}
						validation = strings.Join(validations, ", ")
					}

					sb.WriteString(fmt.Sprintf("| %s | %s | %s | %s | %s |\n",
						fieldName, fieldType, required, fieldDesc, validation))
				}
			}
		}
		sb.WriteString("\n")
	}

	err := os.WriteFile(outputPath, []byte(sb.String()), 0644)
	if err != nil {
		return fmt.Errorf("failed to write model table: %w", err)
	}
	return nil
}
