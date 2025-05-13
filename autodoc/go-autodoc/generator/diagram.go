package generator

import (
	"fmt"
	"os"
	"sort"
	"strings"

	"autodoc/parser"
)

func getCommonPrefix(paths [][]string) []string {
	if len(paths) == 0 {
		return nil
	}
	prefix := paths[0]
	for _, parts := range paths[1:] {
		// shorten prefix until it matches parts
		for i := 0; i < len(prefix) && i < len(parts); i++ {
			if prefix[i] != parts[i] {
				prefix = prefix[:i]
				break
			}
		}
		if len(parts) < len(prefix) {
			prefix = prefix[:len(parts)]
		}
	}
	return prefix
}

// GenerateEndpointMap writes a Mermaid flowchart grouping endpoints by tag.
func GenerateEndpointMap(ir *parser.IR, outputPath string) error {
	// 1) Break every path into segments
	allParts := make([][]string, len(ir.Endpoints))
	for i, ep := range ir.Endpoints {
		// split and drop empty leading ""
		segs := strings.FieldsFunc(ep.Path, func(r rune) bool { return r == '/' })
		allParts[i] = segs
	}

	// 2) Find the common wrapper prefix
	common := getCommonPrefix(allParts)
	// common might be ["api","v1"] or [] if none

	// 3) Group by the segment immediately after the common prefix
	groups := make(map[string][]parser.EndpointData)
	for idx, ep := range ir.Endpoints {
		parts := allParts[idx]
		var key string
		if len(ep.Tags) > 0 && ep.Tags[0] != "" {
			key = ep.Tags[0]
		} else if len(parts) > len(common) {
			key = strings.Title(parts[len(common)])
		} else {
			key = "Default"
		}
		groups[key] = append(groups[key], ep)

	}

	// Sort group names for deterministic output
	names := make([]string, 0, len(groups))
	for name := range groups {
		names = append(names, name)
	}
	sort.Strings(names)

	// Build Mermaid content
	var sb strings.Builder
	sb.WriteString("flowchart TB\n")
	for _, name := range names {
		sb.WriteString(fmt.Sprintf("  subgraph %s\n", sanitize(name)))
		sb.WriteString("    direction TB\n")
		for _, ep := range groups[name] {
			// Build parameter list text
			var params []string
			for _, p := range ep.Parameters {
				req := ""
				if p.Required {
					req = " (req)"
				}
				params = append(params, fmt.Sprintf("%s:%s%s", p.Name, p.In, req))
			}
			paramText := ""
			if len(params) > 0 {
				paramText = "\nParams: " + strings.Join(params, ", ")
			}

			// Build node label
			label := fmt.Sprintf("%s %s%s", ep.Method, ep.Path, paramText)
			// create a node ID from method and path
			id := sanitize(ep.Method + "_" + ep.Path)
			node := fmt.Sprintf("    %s[\"%s\"]\n", id, label)
			sb.WriteString(node)
		}
		sb.WriteString("  end\n\n")
	}

	// Write to file
	content := sb.String()
	if err := os.WriteFile(outputPath, []byte(content), 0644); err != nil {
		return fmt.Errorf("failed to write diagram: %w", err)
	}

	return nil
}

// sanitize converts a string into a Mermaid-safe identifier
func sanitize(s string) string {
	// replace non-alphanumeric with underscore
	s = strings.ReplaceAll(s, "/", "_")
	s = strings.ReplaceAll(s, "{", "_")
	s = strings.ReplaceAll(s, "}", "_")
	s = strings.ReplaceAll(s, "-", "_")
	s = strings.ReplaceAll(s, " ", "_")
	// collapse multiple underscores
	for strings.Contains(s, "__") {
		s = strings.ReplaceAll(s, "__", "_")
	}
	return s
}
