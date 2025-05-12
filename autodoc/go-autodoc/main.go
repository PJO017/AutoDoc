package main

import (
	"fmt"
	"os"
	"log"

	"autodoc/parser"
)

func main() {
	// Parsing the arguments for Java file path
	if len(os.Args) < 3 {
		fmt.Println("Usage: go-autodoc <java-source-dir> <output-dir>")
		os.Exit(1)
	}

	javaSrcDir := os.Args[1]
	outputDir := os.Args[2]

	// Call the Java tool to parse the Java file
	classInfoJSON, err := parser.CallJavaParser(javaSrcDir)
	if err != nil {
		log.Fatalf("Error calling Java parser: %v", err)
	}

	// Print out the JSON received from Java tool (for debugging)
	fmt.Println("Received JSON from Java tool:")
	fmt.Println(classInfoJSON)

	// Optionally, you can unmarshal and further process the data to generate OpenAPI or other docs
	// For example, write the JSON to the output directory:
	err = os.WriteFile(outputDir+"/output.json", []byte(classInfoJSON), 0644)
	if err != nil {
		log.Fatalf("Error writing JSON to file: %v", err)
	}
	fmt.Println("JSON output written to:", outputDir+"/output.json")
}
