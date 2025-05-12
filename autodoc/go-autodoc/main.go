package main

import (
	"fmt"
	"os"

	"autodoc/parser"
	"autodoc/generator"
	"gopkg.in/yaml.v3"
	
	"github.com/spf13/cobra"
)

var (
	// CLI flags
	source  string
	info    string
	servers string
	output  string
	lang    string
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
	return nil
}
