# AutoDoc: Architecture & Design Summary

## Overview

**AutoDoc** is a cross-language tool designed to automatically generate documentation from Java codebases, with an initial focus on producing OpenAPI (Swagger) specifications. The system consists of two core components:

1. **Java CLI Parser Tool** – Parses Java source code using JavaParser and outputs structured JSON.
2. **Golang CLI Orchestrator** – Manages user interaction, invokes the Java tool, processes JSON, and outputs OpenAPI documentation.

This architecture is modular, extensible, and optimized for performance and portability.

---

## Key Design Goals

* **Separation of Concerns**: Java handles language-specific parsing; Go handles orchestration and CLI interaction.
* **Static Analysis**: All documentation is derived from source code, not runtime reflection.
* **Extensibility**: System is designed to grow with features like Swagger UI generation, dependency graphs, and Git integration.
* **Cross-Platform Deployment**: Using Go's static binaries and Java's wide runtime support.

---

## Component 1: Java CLI Parser

### Purpose

Parses Java source code to extract:

* REST controllers and endpoints (via Spring annotations)
* Request and response models
* Field and type information for data models
* Other documentation-relevant metadata

### Implementation

* **Language**: Java
* **Parser Library**: [JavaParser](https://javaparser.org/)
* **Entry Point**: `AutoDocParser.java`
* **Input**: Java source code directory or individual files
* **Output**: Structured JSON describing API paths, methods, parameters, models, etc.

### Example Output File

* `autodoc-output.json`

### Sample Run Command

```bash
java -cp "./target/autodoc-parser-1.0-SNAPSHOT.jar:lib/*" com.autodoc.AutoDocParser ./src > autodoc-output.json
```

---

## Component 2: Go CLI Orchestrator

### Purpose

* Acts as the user interface
* Takes input arguments (e.g., project directory, output path)
* Spawns the Java CLI tool
* Waits for and loads the JSON output
* Converts JSON into a final OpenAPI spec

### Implementation

* **Language**: Go
* **Packages**: `os/exec`, `encoding/json`, `io/ioutil`, `cobra` (optional for CLI UX)

### Strategy

1. CLI accepts flags (e.g., `-project-dir`, `-output-dir`)
2. Constructs Java command dynamically
3. Spawns Java parser using `exec.Command`
4. Waits for process completion, captures stdout or file output
5. Parses JSON using Go structs
6. Renders OpenAPI spec to file (YAML or JSON)

### Communication Strategy

* Java writes structured JSON to a file or stdout
* Go reads from file or pipes and unmarshals it into Go structs
* The interface is decoupled, ensuring either tool can evolve independently

---

## Example JSON Schema (Simplified)

```json
{
  "paths": {
    "/products": {
      "get": {
        "summary": "Get all products",
        "responses": {
          "200": {
            "description": "OK",
            "content": {
              "application/json": {
                "schema": {
                  "$ref": "#/components/schemas/Product"
                }
              }
            }
          }
        }
      }
    }
  },
  "components": {
    "schemas": {
      "Product": {
        "type": "object",
        "properties": {
          "id": {"type": "integer"},
          "name": {"type": "string"}
        }
      }
    }
  }
}
```

---

## Why This Approach?

* **No Existing Tool Fits**: Existing tools like Springdoc or Swagger Core are runtime-based and do not parse source code statically.
* **Full Control**: Custom JSON schema enables granular control and future extensibility.
* **Performance & Portability**: Go ensures fast CLI and simple deployment. JavaParser is mature and well-suited for source analysis.
* **Developer-Friendly**: Modular architecture allows teams to contribute to either the Go or Java portion without needing deep cross-knowledge.

---

## Next Steps

* Finalize JSON output schema
* Build OpenAPI renderer in Go
* Handle edge cases (e.g., nested DTOs, inheritance)
* Support for additional metadata (tags, descriptions, etc.)
* Future: Swagger UI generation and Git versioned documentation

---

## Final Thoughts

The current architecture strikes a solid balance between power, flexibility, and simplicity. By keeping the parser in Java and the orchestrator in Go, you preserve clear boundaries and create a clean, enterprise-friendly toolchain for automated documentation generation.
