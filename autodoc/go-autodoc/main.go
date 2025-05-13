package main

import (
	"fmt"
	"os"
	"path/filepath"
	"strings"

	"autodoc/generator"
	"autodoc/parser"

	"gopkg.in/yaml.v3"

	"github.com/spf13/cobra"
)

var (
	// CLI flags
	source   string
	info     string
	servers  string
	output   string
	lang     string
	tables   string
	diagrams string
)

// rootCmd is the base command for go-autodoc
var rootCmd = &cobra.Command{
	Use:   "go-autodoc",
	Short: "Generate OpenAPI specs from code annotations",
	Long: `go-autodoc is a CLI tool to parse codebases
and emit OpenAPI specifications for your services.`,
	RunE: func(cmd *cobra.Command, args []string) error {
		return runGenerate()
	},
}

func init() {
	// Persistent flags available to all subcommands
	rootCmd.PersistentFlags().StringVarP(&source, "source", "s", "", "Path to source files (required)")
	rootCmd.PersistentFlags().StringVarP(&info, "info", "i", "title=\"API\",version=\"1.0.0\"", "API metadata as key=\"value\" pairs, comma-separated")
	rootCmd.PersistentFlags().StringVarP(&servers, "servers", "S", "url=\"https://api.example.com\"", "Server list as url=\"...\",description=\"...\" pairs, semicolon-separated")
	rootCmd.PersistentFlags().StringVarP(&output, "output", "o", "openapi.yaml", "Output spec file path")
	rootCmd.PersistentFlags().StringVarP(&lang, "lang", "l", "java", "Language parser to use (java|kotlin|python)")
	rootCmd.PersistentFlags().StringVar(&tables, "tables", "", "Comma-separated tables to generate (e.g., endpoint-table)")
	rootCmd.PersistentFlags().StringVar(&diagrams, "diagrams", "", "Comma-separated diagrams to generate (e.g., endpoint-map)")

	// Mark required flags
	rootCmd.MarkPersistentFlagRequired("source")

	// You can add subcommands here (e.g., version)
}

func main() {
	if err := rootCmd.Execute(); err != nil {
		fmt.Fprintln(os.Stderr, err)
		os.Exit(1)
	}
}

// runGenerate orchestrates parsing and spec generation
func runGenerate() error {
	// 1) Parse source into IR
	ir, err := parser.ParseWithLang(source, lang)
	if err != nil {
		return fmt.Errorf("parsing failed: %w", err)
	}

	// 2) Build OpenAPI spec
	apiInfo := parseInfo(info)
	srvConfigs := parseServers(servers)
	spec := generator.BuildOpenAPISpec(*ir, apiInfo, srvConfigs)

	// 3) Emit YAML
	outYAML, err := yaml.Marshal(spec)
	if err != nil {
		return fmt.Errorf("yaml marshal failed: %w", err)
	}
	if err := os.WriteFile(output, outYAML, 0644); err != nil {
		return fmt.Errorf("writing spec failed: %w", err)
	}

	fmt.Println("OpenAPI spec written to", output)

	// 3) Generate tables if requested
	if tables != "" {
		tablesList := strings.Split(tables, ",")
		// Determine base output directory for tables
		baseDir := filepath.Dir(output)
		for _, t := range tablesList {
			switch strings.TrimSpace(t) {
			case "endpoint-table":
				path := filepath.Join(baseDir, "endpoint-table.md")
				if err := generator.GenerateEndpointTable(ir, path); err != nil {
					return fmt.Errorf("endpoint-table generation failed: %w", err)
				}
				fmt.Println("Table generated:", path)
			// Add more table types here
			default:
				fmt.Fprintf(os.Stderr, "warning: unknown table type '%s'\n", t)
			}
		}
	}

	// 4) Generate diagrams if requested
	if diagrams != "" {
		dirs := strings.Split(diagrams, ",")
		// Determine base output directory for diagrams
		baseDir := filepath.Dir(output)
		for _, d := range dirs {
			switch strings.TrimSpace(d) {
			case "endpoint-map":
				path := filepath.Join(baseDir, "endpoint-map.mmd")
				if err := generator.GenerateEndpointMap(ir, path); err != nil {
					return fmt.Errorf("endpoint-map generation failed: %w", err)
				}
				fmt.Println("Diagram generated:", path)
			// Add more diagram types here
			default:
				fmt.Fprintf(os.Stderr, "warning: unknown diagram type '%s'\n", d)
			}
		}
	}
	return nil
}
