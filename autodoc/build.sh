#!/bin/bash
set -e

# Navigate to the java-parser directory and build the Java application
cd java-parser
mvn clean package

# Copy the built JAR into the Go parser package
# Uses wildcard to match the versioned JAR (e.g. autodoc-parser-1.0-SNAPSHOT.jar)
cp target/autodoc-parser-*.jar ../go-autodoc/parser/parser.jar

# Navigate back to the root directory
cd ..

# Build the Go application
cd go-autodoc
go build -o bin/go-autodoc
cd ..

echo "Build completed successfully."
echo "  • Go executable: go-autodoc/bin/go-autodoc"
echo "  • Parser JAR:    go-autodoc/parser/parser.jar"