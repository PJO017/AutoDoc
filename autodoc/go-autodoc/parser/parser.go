package parser

import (
	_ "embed"
	"encoding/json"
	"log"
	"os"
	"os/exec"
)

//go:embed parser.jar
var parserJar []byte

// CallJavaParser runs the Java program and returns the JSON output
func CallJavaParser(javaSrcDir string) (string, error) {
	// 1) dump jar to a temp file
	tmp, err := os.CreateTemp("", "java-parser-*.jar")
	if err != nil {
		log.Printf("Error creating temp: %v", err)
		return "", err
	}
	defer os.Remove(tmp.Name())
	if _, err := tmp.Write(parserJar); err != nil {
		tmp.Close()
		log.Printf("Error writing temp: %v", err)
		return "", err
	}
	tmp.Close()

	// 2) exec “java -jar /path/to/tmp.jar …”
	cmd := exec.Command("java", "-jar", tmp.Name(), javaSrcDir)
	out, err := cmd.CombinedOutput()
	if err != nil {
		log.Printf("Error running Java parser: %v", err)
		log.Printf("Output: %s", out)
		log.Printf("Error: %v", err)
	}
	return string(out), err

}

// Parse runs the Java parser on `srcDir`, then unmarshals the JSON into an IR.
func Parse(srcDir string) (*IR, error) {
	// 1) Invoke the Java JAR (existing) :contentReference[oaicite:2]{index=2}:contentReference[oaicite:3]{index=3}
	jsonStr, err := CallJavaParser(srcDir)
	if err != nil {
		return nil, err
	}

	// 2) Unmarshal into your IR types
	var ir IR
	if err := json.Unmarshal([]byte(jsonStr), &ir); err != nil {
		return nil, err
	}

	return &ir, nil
}
