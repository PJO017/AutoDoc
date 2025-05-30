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
	// Create maps for our analysis
	controllerMap := make(map[string]*ControllerInfo)
	serviceTypes := make(map[string]*ServiceInfo)

	// Process all endpoints to build our data structures
	for _, ep := range ir.Endpoints {
		if ep.ControllerName == "" {
			continue
		}

		// Get or create controller info
		ctrl, exists := controllerMap[ep.ControllerName]
		if !exists {
			ctrl = &ControllerInfo{
				Name:      ep.ControllerName,
				Package:   ep.ControllerPackage,
				Endpoints: make(map[string][]string),
				Services:  make(map[string]DependencyDetail),
			}
			controllerMap[ep.ControllerName] = ctrl
		}

		// Add endpoint to controller
		methodPath := fmt.Sprintf("%s %s", ep.Method, ep.Path)
		ctrl.Endpoints[methodPath] = append(ctrl.Endpoints[methodPath], ep.Summary)

		// Process dependencies
		for _, dep := range ep.Dependencies {
			// Add to controller's service list with detail
			depDetail, exists := ctrl.Services[dep.Type]
			if !exists {
				depDetail = DependencyDetail{
					Name:          dep.Name,
					Type:          dep.Type,
					InjectionType: dep.InjectionType,
					Methods:       make(map[string]bool),
				}
			}
			depDetail.Methods[methodPath] = true
			ctrl.Services[dep.Type] = depDetail

			// Add to services map
			svc, exists := serviceTypes[dep.Type]
			if !exists {
				svc = &ServiceInfo{
					Name:   dep.Type,
					UsedBy: make(map[string]bool),
				}
				serviceTypes[dep.Type] = svc
			}
			svc.UsedBy[ep.ControllerName] = true
		}
	}

	// Start building the Mermaid class diagram
	var sb strings.Builder
	sb.WriteString("classDiagram\n")
	sb.WriteString("  %% Controller-Service Dependency Graph\n")
	sb.WriteString("  %% Generated by AutoDoc\n\n")

	// First define all controller classes with methods
	for _, ctrl := range controllerMap {
		sb.WriteString(fmt.Sprintf("  class %s {\n", sanitizeClassName(ctrl.Name)))
		sb.WriteString("    <<Controller>>\n")

		// Add top 5 endpoints as methods
		count := 0
		for endpoint := range ctrl.Endpoints {
			if count >= 5 {
				sb.WriteString("    +more()...\n")
				break
			}

			// Clean the method name
			methodName := endpoint
			if strings.Contains(methodName, " ") {
				parts := strings.SplitN(methodName, " ", 2)
				method := strings.ToLower(parts[0])
				path := parts[1]

				// Convert path to camelCase method name
				segments := strings.Split(path, "/")
				methodName = method
				for _, seg := range segments {
					if seg == "" {
						continue
					}

					// Strip any path parameters
					if strings.HasPrefix(seg, "{") && strings.HasSuffix(seg, "}") {
						// Convert {userId} to UserId
						paramName := seg[1 : len(seg)-1]
						methodName += strings.Title(paramName)
					} else {
						methodName += strings.Title(seg)
					}
				}
			}

			sb.WriteString(fmt.Sprintf("    +%s()\n", methodName))
			count++
		}

		// Show package
		if ctrl.Package != "" {
			shortPkg := ctrl.Package
			if lastDot := strings.LastIndex(shortPkg, "."); lastDot > 0 {
				shortPkg = shortPkg[lastDot+1:]
			}
			sb.WriteString(fmt.Sprintf("    +package %s\n", shortPkg))
		}

		sb.WriteString("  }\n\n")
	}

	// Define service classes with usage info
	for svcName, svc := range serviceTypes {
		sb.WriteString(fmt.Sprintf("  class %s {\n", sanitizeClassName(svcName)))

		// Determine stereotype based on name
		if strings.Contains(strings.ToLower(svcName), "repository") ||
			strings.Contains(strings.ToLower(svcName), "dao") {
			sb.WriteString("    <<Repository>>\n")
		} else if strings.Contains(strings.ToLower(svcName), "util") ||
			strings.Contains(strings.ToLower(svcName), "helper") {
			sb.WriteString("    <<Utility>>\n")
		} else {
			sb.WriteString("    <<Service>>\n")
		}

		// Show usage count
		usageCount := len(svc.UsedBy)
		sb.WriteString(fmt.Sprintf("    Used by %d controllers\n", usageCount))

		sb.WriteString("  }\n\n")
	}

	// Define relationships with details
	for ctrlName, ctrl := range controllerMap {
		// Add relationships for each service with detailed labels
		for svcName, depDetail := range ctrl.Services {
			// Show injection type
			label := depDetail.InjectionType

			// Show cardinality - always 1-to-1 for Spring services
			sb.WriteString(fmt.Sprintf("  %s \"1\" --> \"1\" %s : %s\n",
				sanitizeClassName(ctrlName),
				sanitizeClassName(svcName),
				label))
		}
	}

	// Add notes for heavily used services
	sb.WriteString("  %% Notes for important services\n")
	for svcName, svc := range serviceTypes {
		if len(svc.UsedBy) >= 3 {
			// This is a core service used by many controllers
			sb.WriteString(fmt.Sprintf("  note for %s \"Core service used by multiple controllers\"\n",
				sanitizeClassName(svcName)))
		}
	}

	// Write to file
	content := sb.String()
	if err := os.WriteFile(outputPath, []byte(content), 0644); err != nil {
		return fmt.Errorf("failed to write controller-service graph: %w", err)
	}

	return nil
}

// Enhanced implementation using a flowchart style with rich details
func GenerateControllerServiceFlowchart(ir *parser.IR, outputPath string) error {
	// Create maps for our analysis
	controllerMap := make(map[string]*ControllerInfo)
	serviceTypes := make(map[string]*ServiceInfo)

	// Group by package/module
	packages := make(map[string][]string)

	// Process all endpoints to build our data structures
	for _, ep := range ir.Endpoints {
		if ep.ControllerName == "" {
			continue
		}

		// Get or create controller info
		ctrl, exists := controllerMap[ep.ControllerName]
		if !exists {
			ctrl = &ControllerInfo{
				Name:      ep.ControllerName,
				Package:   ep.ControllerPackage,
				Endpoints: make(map[string][]string),
				Services:  make(map[string]DependencyDetail),
			}
			controllerMap[ep.ControllerName] = ctrl

			// Track by package
			packages[ep.ControllerPackage] = append(packages[ep.ControllerPackage], ep.ControllerName)
		}

		// Add endpoint to controller
		methodPath := fmt.Sprintf("%s %s", ep.Method, ep.Path)
		ctrl.Endpoints[methodPath] = append(ctrl.Endpoints[methodPath], ep.Summary)

		// Process dependencies
		for _, dep := range ep.Dependencies {
			// Add to controller's service list with detail
			depDetail, exists := ctrl.Services[dep.Type]
			if !exists {
				depDetail = DependencyDetail{
					Name:          dep.Name,
					Type:          dep.Type,
					InjectionType: dep.InjectionType,
					Methods:       make(map[string]bool),
				}
			}
			depDetail.Methods[methodPath] = true
			ctrl.Services[dep.Type] = depDetail

			// Add to services map
			svc, exists := serviceTypes[dep.Type]
			if !exists {
				svc = &ServiceInfo{
					Name:   dep.Type,
					UsedBy: make(map[string]bool),
				}
				serviceTypes[dep.Type] = svc
			}
			svc.UsedBy[ep.ControllerName] = true
		}
	}

	// Start building the Mermaid flowchart
	var sb strings.Builder
	sb.WriteString("flowchart TD\n")
	sb.WriteString("  %% Controller-Service Dependency Graph\n")
	sb.WriteString("  %% Generated by AutoDoc\n\n")

	// Style definitions
	sb.WriteString("  %% Styling\n")
	sb.WriteString("  classDef controller fill:#e1f5fe,stroke:#01579b,stroke-width:2px,color:#01579b\n")
	sb.WriteString("  classDef service fill:#f3e5f5,stroke:#6a1b9a,stroke-width:2px,color:#6a1b9a\n")
	sb.WriteString("  classDef repository fill:#e8f5e9,stroke:#2e7d32,stroke-width:2px,color:#2e7d32\n")
	sb.WriteString("  classDef util fill:#fff3e0,stroke:#e65100,stroke-width:2px,color:#e65100\n")
	sb.WriteString("  classDef deprecated fill:#fafafa,stroke:#616161,stroke-width:1px,stroke-dasharray:5 5,color:#616161\n\n")

	// Group by package where possible
	packageNames := make([]string, 0, len(packages))
	for pkg := range packages {
		packageNames = append(packageNames, pkg)
	}
	sort.Strings(packageNames)

	// Controllers section with package grouping
	sb.WriteString("  subgraph Controllers\n")
	sb.WriteString("    direction TB\n")

	// Group controllers by package if we have package info
	if len(packageNames) > 0 {
		for _, pkg := range packageNames {
			if pkg == "" {
				continue // Skip empty packages
			}

			// Get package short name for the subgraph label
			packageShortName := pkg
			if lastDot := strings.LastIndex(pkg, "."); lastDot > 0 {
				packageShortName = pkg[lastDot+1:]
			}

			sb.WriteString(fmt.Sprintf("    subgraph %s[%s]\n", sanitizeId("pkg_"+pkg), packageShortName))
			sb.WriteString("      direction LR\n")

			// Add controllers in this package
			for _, ctrlName := range packages[pkg] {
				ctrl := controllerMap[ctrlName]

				// Create node with detailed info
				nodeId := sanitizeId(ctrlName)

				// Build the node label with endpoints
				label := fmt.Sprintf("%s\\n", ctrlName)

				// Add some of the key endpoints (limit to 5 to avoid huge boxes)
				count := 0
				for endpoint, _ := range ctrl.Endpoints {
					if count >= 5 {
						label += "...\\n"
						break
					}

					// Extract HTTP method for styling
					parts := strings.SplitN(endpoint, " ", 2)
					method := parts[0]
					path := parts[1]

					// Add styled endpoint
					label += fmt.Sprintf("• %s %s\\n", method, path)
					count++
				}

				sb.WriteString(fmt.Sprintf("      %s[\"%s\"]\n", nodeId, label))
				sb.WriteString(fmt.Sprintf("      class %s controller\n", nodeId))
			}

			sb.WriteString("    end\n\n")
		}
	}

	// Handle controllers without package info or empty package
	for ctrlName, ctrl := range controllerMap {
		if ctrl.Package != "" && packages[ctrl.Package] != nil {
			continue // Already processed above
		}

		nodeId := sanitizeId(ctrlName)

		// Create node with detailed info
		label := fmt.Sprintf("%s\\n", ctrlName)

		// Add some endpoints (limit to 5 to avoid huge boxes)
		count := 0
		for endpoint, _ := range ctrl.Endpoints {
			if count >= 5 {
				label += "...\\n"
				break
			}

			// Extract HTTP method for styling
			parts := strings.SplitN(endpoint, " ", 2)
			method := parts[0]
			path := parts[1]

			// Add styled endpoint
			label += fmt.Sprintf("• %s %s\\n", method, path)
			count++
		}

		sb.WriteString(fmt.Sprintf("    %s[\"%s\"]\n", nodeId, label))
		sb.WriteString(fmt.Sprintf("    class %s controller\n", nodeId))
	}

	sb.WriteString("  end\n\n")

	// Services section with categorization by type
	sb.WriteString("  subgraph Services\n")
	sb.WriteString("    direction TB\n")

	// Group services by type (service, repository, util)
	servicesByType := map[string][]string{
		"service":    {},
		"repository": {},
		"util":       {},
	}

	// Categorize services
	for svcName := range serviceTypes {
		lowerName := strings.ToLower(svcName)
		if strings.Contains(lowerName, "repository") || strings.Contains(lowerName, "dao") || strings.Contains(lowerName, "repo") {
			servicesByType["repository"] = append(servicesByType["repository"], svcName)
		} else if strings.Contains(lowerName, "util") || strings.Contains(lowerName, "helper") || strings.Contains(lowerName, "utils") {
			servicesByType["util"] = append(servicesByType["util"], svcName)
		} else {
			servicesByType["service"] = append(servicesByType["service"], svcName)
		}
	}

	// Service subgroups
	for groupName, groupServices := range servicesByType {
		if len(groupServices) == 0 {
			continue
		}

		sort.Strings(groupServices)

		title := strings.Title(groupName) + "s"
		sb.WriteString(fmt.Sprintf("    subgraph %s[%s]\n", sanitizeId("group_"+groupName), title))
		sb.WriteString("      direction LR\n")

		for _, svcName := range groupServices {
			svc := serviceTypes[svcName]
			nodeId := sanitizeId(svcName)

			// Calculate how many controllers use this service
			usageCount := len(svc.UsedBy)

			// Create service node with usage info
			sb.WriteString(fmt.Sprintf("      %s[\"%s\\n(%d controllers)\"]\n",
				nodeId, svcName, usageCount))
			sb.WriteString(fmt.Sprintf("      class %s %s\n", nodeId, groupName))
		}

		sb.WriteString("    end\n\n")
	}

	sb.WriteString("  end\n\n")

	// Add relationships with more detail
	sb.WriteString("  %% Relationships\n")
	for ctrlName, ctrl := range controllerMap {
		ctrlId := sanitizeId(ctrlName)

		// Add edges for each service dependency
		for svcName, depDetail := range ctrl.Services {
			svcId := sanitizeId(svcName)

			// Create edge with injection type as label
			sb.WriteString(fmt.Sprintf("  %s -->|%s| %s\n",
				ctrlId, depDetail.InjectionType, svcId))
		}
	}

	// Write to file
	content := sb.String()
	if err := os.WriteFile(outputPath, []byte(content), 0644); err != nil {
		return fmt.Errorf("failed to write controller-service graph: %w", err)
	}

	return nil
}

// Helper structures for richer graph generation
type ControllerInfo struct {
	Name      string
	Package   string
	Endpoints map[string][]string // map[methodPath][]summaries
	Services  map[string]DependencyDetail
}

type ServiceInfo struct {
	Name   string
	UsedBy map[string]bool // map[controllerName]true
}

type DependencyDetail struct {
	Name          string
	Type          string
	InjectionType string
	Methods       map[string]bool // map[methodPath]true - which endpoints use this service
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
