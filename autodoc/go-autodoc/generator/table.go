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
		sb.WriteString(fmt.Sprintf("| %s | %s | `%s` | %s | %s |\n", r.controller, r.method, r.path, r.params, r.summary))
	}

	// Write file
	err := os.WriteFile(outputPath, []byte(sb.String()), 0644)
	if err != nil {
		return fmt.Errorf("failed to write endpoint table: %w", err)
	}
	return nil
}
