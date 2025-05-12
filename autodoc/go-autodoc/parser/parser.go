package parser

import (
	"bytes"
	"os/exec"
	"log"
)

// CallJavaParser runs the Java program and returns the JSON output
func CallJavaParser(javaSrcDir string) (string, error) {
	// Command to run the Java tool
	cmd := exec.Command("java", "-cp", "../java-parser/target/autodoc-parser-1.0-SNAPSHOT.jar", "com.autodoc.AutodocApplication", javaSrcDir)

	// Capture the output from the Java tool
	var out bytes.Buffer
	cmd.Stdout = &out
	cmd.Stderr = &out

	// Run the command
	err := cmd.Run()
	if err != nil {
		log.Printf("Error running Java parser: %v", err)
		return "", err
	}

	// Return the captured output (JSON data)
	return out.String(), nil
}
