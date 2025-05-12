package main

import (
	"flag"
	"fmt"
	"log"
	"os"

	"autodoc/parser"
	"autodoc/generator"

	"gopkg.in/yaml.v3"
)

func main() {
	srcDir := flag.String("source", "", "Path to your Java/Kotlin/etc. source")
	info := flag.String("info", "title=\"API\",version=\"1.0.0\"", "API info metadata")
	servers := flag.String("servers", "url=\"https://api.example.com\"", "Server list")
	out := flag.String("output", "openapi.yaml", "Output spec file")
	flag.Parse()

	if *srcDir == "" {
		fmt.Fprintln(os.Stderr, "⚠️  --source is required")
		os.Exit(1)
	}

	// 1) **Parse** your code into the shared IR
	ir, err := parser.Parse(*srcDir)
	if err != nil {
		log.Fatalf("failed to parse source: %v", err)
	}

	// 2) **Build** the OpenAPI spec from that IR
	apiInfo := parseInfo(*info) // your helper to split key=val pairs
	srvConfigs := parseServers(*servers)
	spec := generator.BuildOpenAPISpec(*ir, apiInfo, srvConfigs)

	// 3) **Emit** YAML
	outYAML, err := yaml.Marshal(spec)
	if err != nil {
		log.Fatalf("yaml marshal failed: %v", err)
	}
	if err := os.WriteFile(*out, outYAML, 0644); err != nil {
		log.Fatalf("write failed: %v", err)
	}
	fmt.Println("OpenAPI spec written to", *out)
}
