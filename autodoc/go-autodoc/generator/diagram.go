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

// GenerateControllerServiceGraph generates a Mermaid class diagram showing controller-service dependencies
func GenerateControllerServiceGraph(ir *parser.IR, outputPath string) error {
	// Create a map of controller names to their dependencies
	controllerDeps := make(map[string][]parser.Dependency)
	
	// Set to track unique dependencies for deduplication
	allServices := make(map[string]bool)
	
	// Group dependencies by controller
	for _, ep := range ir.Endpoints {
		if ep.ControllerName != "" {
			for _, dep := range ep.Dependencies {
				// Add to the map of dependencies
				controllerDeps[ep.ControllerName] = append(controllerDeps[ep.ControllerName], dep)
				
				// Track unique service types
				allServices[dep.Type] = true
			}
		}
	}
	
	// Get sorted controller names for consistent output
	controllerNames := make([]string, 0, len(controllerDeps))
	for name := range controllerDeps {
		controllerNames = append(controllerNames, name)
	}
	sort.Strings(controllerNames)
	
	// Start building the Mermaid class diagram
	var sb strings.Builder
	sb.WriteString("classDiagram\n")
	
	// First define all controller and service classes
	for _, controllerName := range controllerNames {
		sb.WriteString(fmt.Sprintf("  class %s {\n", sanitizeClassName(controllerName)))
		sb.WriteString("    <<Controller>>\n")
		sb.WriteString("  }\n\n")
	}
	
	// Define all unique service classes
	serviceNames := make([]string, 0, len(allServices))
	for service := range allServices {
		serviceNames = append(serviceNames, service)
	}
	sort.Strings(serviceNames)
	
	for _, serviceName := range serviceNames {
		sb.WriteString(fmt.Sprintf("  class %s {\n", sanitizeClassName(serviceName)))
		sb.WriteString("    <<Service>>\n")
		sb.WriteString("  }\n\n")
	}
	
	// Add relationships between controllers and services
	for _, controllerName := range controllerNames {
		// Deduplicate dependencies to avoid multiple arrows
		uniqueDeps := make(map[string]bool)
		
		for _, dep := range controllerDeps[controllerName] {
			if !uniqueDeps[dep.Type] {
				sb.WriteString(fmt.Sprintf("  %s --> %s : uses\n", 
					sanitizeClassName(controllerName), 
					sanitizeClassName(dep.Type)))
				uniqueDeps[dep.Type] = true
			}
		}
	}
	
	// Write to file
	content := sb.String()
	if err := os.WriteFile(outputPath, []byte(content), 0644); err != nil {
		return fmt.Errorf("failed to write controller-service graph: %w", err)
	}
	
	return nil
}

// Alternative implementation using a flowchart style which has better layout options
func GenerateControllerServiceFlowchart(ir *parser.IR, outputPath string) error {
	// Create a map of controller names to their dependencies
	controllerDeps := make(map[string][]parser.Dependency)
	
	// Set to track unique dependencies for deduplication
	allServices := make(map[string]bool)
	
	// Group dependencies by controller
	for _, ep := range ir.Endpoints {
		if ep.ControllerName != "" {
			controllerDeps[ep.ControllerName] = append(controllerDeps[ep.ControllerName], ep.Dependencies...)
			
			// Track unique service types
			for _, dep := range ep.Dependencies {
				allServices[dep.Type] = true
			}
		}
	}
	
	// Get sorted controller names for consistent output
	controllerNames := make([]string, 0, len(controllerDeps))
	for name := range controllerDeps {
		controllerNames = append(controllerNames, name)
	}
	sort.Strings(controllerNames)
	
	// Start building the Mermaid flowchart
	var sb strings.Builder
	sb.WriteString("flowchart TD\n")
	
	// Create controller subgraph
	sb.WriteString("  subgraph Controllers\n")
	for _, controllerName := range controllerNames {
		id := sanitizeId(controllerName)
		sb.WriteString(fmt.Sprintf("    %s[\"%s\"]\n", id, controllerName))
	}
	sb.WriteString("  end\n\n")
	
	// Create services subgraph
	sb.WriteString("  subgraph Services\n")
	serviceNames := make([]string, 0, len(allServices))
	for service := range allServices {
		serviceNames = append(serviceNames, service)
	}
	sort.Strings(serviceNames)
	
	for _, serviceName := range serviceNames {
		id := sanitizeId(serviceName)
		sb.WriteString(fmt.Sprintf("    %s[\"%s\"]\n", id, serviceName))
	}
	sb.WriteString("  end\n\n")
	
	// Add relationships
	for _, controllerName := range controllerNames {
		// Deduplicate dependencies
		uniqueDeps := make(map[string]bool)
		
		for _, dep := range controllerDeps[controllerName] {
			if !uniqueDeps[dep.Type] {
				sb.WriteString(fmt.Sprintf("  %s --> %s\n", 
					sanitizeId(controllerName), 
					sanitizeId(dep.Type)))
				uniqueDeps[dep.Type] = true
			}
		}
	}
	
	// Write to file
	content := sb.String()
	if err := os.WriteFile(outputPath, []byte(content), 0644); err != nil {
		return fmt.Errorf("failed to write controller-service graph: %w", err)
	}
	
	return nil
}

// Sanitize class names for Mermaid class diagrams
func sanitizeClassName(name string) string {
	// Remove generic type parameters if present
	if idx := strings.IndexAny(name, "<"); idx > 0 {
		name = name[:idx]
	}
	
	// Remove "Controller" suffix if present to make the diagram cleaner
	name = strings.TrimSuffix(name, "Controller")
	
	return name
}

// Sanitize node IDs for Mermaid flowcharts
func sanitizeId(s string) string {
	// For flowchart node IDs, replace special characters
	s = strings.ReplaceAll(s, "<", "_")
	s = strings.ReplaceAll(s, ">", "_")
	s = strings.ReplaceAll(s, " ", "_")
	s = strings.ReplaceAll(s, "-", "_")
	return s
}