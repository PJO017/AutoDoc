#!/bin/bash
set -e

# Navigate to the java-parser directory and build the Java application
cd java-parser
mvn clean package

# Navigate back to the root directory
cd ..

# Build the Go application
cd go-autodoc
go build -o bin/go-autodoc
cd ..

echo "Build completed successfully. The executable is located at go-autodoc/go-autodoc."