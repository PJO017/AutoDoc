# AutoDoc

AutoDoc is a cross-platform, command-line tool that automatically generates up-to-date OpenAPI documentation from your Java codebase. By statically analyzing your source, it eliminates drift between code and docs, accelerates onboarding, and keeps large teams aligned.

---

## Table of Contents

- [Features](#features)  
- [Architecture](#architecture)  
- [Prerequisites](#prerequisites)  
- [Installation](#installation)  
- [Usage](#usage)  
- [Project Structure](#project-structure)  
- [Extensibility](#extensibility)  
- [Contributing](#contributing)  
- [License](#license)  

---

## Features

- **Accurate, Up-to-Date Docs**: Static analysis via JavaParser keeps your OpenAPI spec in sync with code.  
- **Faster Onboarding**: New team members get immediate, comprehensive API docs.  
- **Team-Scale Clarity**: Large codebases stay documented without manual effort.  
- **CI-Friendly**: Integrate into your build or pipeline to regenerate docs on every commit.  
- **Extensible Outputs**: Out-of-the-box support for OpenAPI JSON, with hooks for Swagger UI, custom templates, dependency graphs, metrics, security scans, and more.  

---

## Architecture

AutoDoc is built as a two-tier system:

1. **Java CLI Parser Tool**  
   - Uses **JavaParser** for static AST analysis of models, controllers, and endpoints.  
   - Marshals metadata into intermediate data classes (`ModelData`, `EndpointData`, etc.).  
   - Builds an OpenAPI spec via `SchemaBuilder` and `OpenApiBuilder`, then serializes with Jackson.  

2. **Go CLI Orchestrator**  
   - A lightweight Go binary (`go-autodoc`) that invokes the Java parser JAR.  
   - Captures the JSON output, writes it to disk (or further processes it).  
   - Provides a simple, cross-platform wrapper for CI or local use.  

**Design Principles**  
- **Separation of Concerns**: Parsing and spec generation in Java; orchestration in Go.  
- **Static Analysis**: No runtime reflection—everything derives directly from source.  
- **Modularity & Extensibility**: Plug in new languages, output formats, or integrations.  
- **Cross-Platform**: JVM + Go binaries run on any major OS.  

---

## Prerequisites

- **Java JDK** 11+  
- **Maven**  
- **Go** 1.24+  
- **Bash** (for the build script)  

---

## Installation

### 1. Automated Build

Make sure `build.sh` is executable, then run it to build both components in one step:

```bash
chmod +x build.sh
./build.sh
````

This script will:

1. Navigate into `java-parser` and run `mvn clean package`.
2. Navigate into `go-autodoc` and run `go build -o bin/go-autodoc`.
3. Echo the location of the Go executable.&#x20;

### 2. Manual Build (Optional)

If you prefer to build each component separately:

1. **Build the Java Parser**

   ```bash
   cd java-parser
   mvn clean package
   cd ..
   ```
2. **Build the Go Orchestrator**

   ```bash
   cd go-autodoc
   go build -o go-autodoc
   cd ..
   ```

---

## Usage

After building, you can generate your OpenAPI spec in two ways:

1. **Via the Go Orchestrator**

   ```bash
   ./go-autodoc <path/to/java/source> <path/to/output-dir>
   # → Writes OpenAPI JSON to output-dir/output.json
   ```
2. **Directly with the Java JAR**

   ```bash
   java -jar java-parser/target/autodoc-1.0-SNAPSHOT.jar <path/to/java/source> \
     > openapi.json
   ```

---

## Project Structure

```
.
├── build.sh                      # Automated build script for Java & Go :contentReference[oaicite:2]{index=2}:contentReference[oaicite:3]{index=3}
├── java-parser/
│   ├── pom.xml
│   └── src/main/java/com/autodoc/
│       ├── AutodocApplication.java
│       ├── generator/
│       │   ├── JavaToOpenApiGenerator.java
│       │   └── ...
│       ├── model/
│       │   ├── FieldData.java
│       │   ├── ModelData.java
│       │   └── EndpointData.java
│       └── builder/
│           ├── SchemaBuilder.java
│           └── OpenApiBuilder.java
└── go-autodoc/
    ├── main.go
    ├── parser/parser.go
    ├── go.mod
    └── bin/
        └── go-autodoc            # Generated Go CLI
```

---

## Extensibility

* **New Languages**: Add a parser module (e.g., Kotlin, Python) alongside the Java parser.
* **Additional Outputs**: Hook into the JSON output to generate Markdown, HTML, or custom formats.
* **CI/CD Integration**: Automate doc regeneration in GitHub Actions, Jenkins, etc.
* **Plugins**: Build annotation-based extensions for custom tags, examples, or security schemes.

---

## Contributing

1. Fork the repo and checkout a feature branch.
2. Write tests or sample code for your feature.
3. Submit a pull request with a clear description & rationale.
4. We’ll review and merge — thanks for helping improve AutoDoc!



