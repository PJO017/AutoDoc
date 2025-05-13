[![Build Status](https://img.shields.io/badge/build-passing-brightgreen)](https://example.com/build)
[![Version](https://img.shields.io/badge/version-1.0.0-blue)](https://github.com/your-repo/AutoDoc/releases)

# AutoDoc Overview

## Table of Contents

* [Introduction](#introduction)
* [Goals](#goals)
* [Prerequisites](#prerequisites)
* [Installation](#installation)
* [IR-First Workflow](#ir-first-workflow)
* [CLI Usage & Flags](#cli-usage--flags)
* [Quickstart Example](#quickstart-example)
* [Links & Further Reading](#links--further-reading)
* [Troubleshooting & FAQ](#troubleshooting--faq)

---

## Introduction

AutoDoc bridges the gap between evolving source code and up‑to‑date documentation by generating OpenAPI specs, Markdown tables, and Mermaid diagrams directly from your codebase. This IR‑first, two‑step process helps teams avoid drift, accelerates onboarding, and ensures your API docs always reflect the latest implementation.

## Goals

* **Accuracy & Maintainability:** Keep parsing logic in language‑specific modules; centralize output rules in the Go CLI.
* **Language‑Agnostic:** Swap in new parsers (Java, Kotlin, Python, etc.) without changing the CLI.
* **Extensible Outputs:** Generate OpenAPI JSON/YAML, endpoint/model tables, diagrams, and more via pluggable generators.
* **CI/CD Friendly:** Single `go-autodoc` binary can be called in any build pipeline.

## Prerequisites

* **Java JDK 11+** (for Java parser)
* **Maven 3.6+** (to build the Java parser jar)
* **Go 1.24+** (for the CLI)
* **Bash** (or compatible shell)

## Installation

1. Clone the repo:

   ```bash
   git clone https://github.com/your-repo/AutoDoc.git
   cd AutoDoc
   ```
2. Build everything:

   ```bash
   chmod +x build.sh
   ./build.sh
   ```
3. Binaries produced:

   * `java-parser/target/autodoc-1.0-SNAPSHOT.jar`
   * `bin/go-autodoc`

## IR-First Workflow

AutoDoc workflow splits into two clear steps:

1. **Parse & Emit IR**

   ```bash
   java -jar java-parser/target/autodoc-1.0-SNAPSHOT.jar --source src/main/java > parsed.json
   ```
2. **Consume IR & Generate Docs**

   ```bash
   go-autodoc \
     --input parsed.json \
     --info title="My API",version="1.0.0" \
     --servers url="https://api.example.com" \
     --tables endpoint-table,model-table \
     --diagrams endpoint-map \
     --output openapi.yaml
   ```

### IR JSON Schema

```jsonc
{
  "models": [
    {
      "name": "User",
      "description": "Data model for users",
      "fields": [
        {"name":"id","typeRef":{"base":"Long","args":[]},"required":true,"description":"Unique identifier"},
        {"name":"email","typeRef":{"base":"String","args":[]},"required":true,"description":"User email address"}
      ]
    }
  ],
  "endpoints": [
    {
      "path": "/users/{id}",
      "method": "GET",
      "summary": "Get user by ID",
      "description": "Fetch a single user record",
      "tags": ["User"],
      "parameters":[{"name":"id","in":"path","required":true,"description":"User ID","type":{"base":"Long","args":[]}}],
      "requestBodyType": null,
      "responseType": {"base":"User","args":[]}
    }
  ]
}
```

## CLI Usage & Flags

```text
Usage:
  go-autodoc [flags]

Flags:
  -s, --source string        Path to source directory (required if --input omitted)
  -i, --info strings         Metadata as key="value" pairs (e.g. title="My API",version="1.0.0")
  -S, --servers strings      Server entries as url="...",description="..."
  -l, --lang string          Parser language: java|kotlin|python (default "java")
      --input string         Path to existing IR JSON (skips parsing)
  -o, --output string        Output path for spec (default "openapi.yaml")
      --tables strings       Comma-separated table generators (endpoint-table,model-table)
      --diagrams strings     Comma-separated diagram generators (endpoint-map)
  -h, --help                 Help for go-autodoc
```

## Quickstart Example

1. Add a simple controller:

   ```java
   @RestController
   @RequestMapping("/hello")
   public class HelloController {
     @GetMapping
     public String greet() { return "Hello, World!"; }
   }
   ```
2. Generate docs:

   ```bash
   go-autodoc --info title="Hello API",version="0.1.0" --tables endpoint-table --output openapi.yaml
   ```
3. Inspect outputs:

   * `openapi.yaml` contains your new `/hello` GET endpoint.
   * `endpoint-table.md` lists the `GET /hello` route.

## Links & Further Reading

* **Deep Dive Architecture:** `docs/architecture.md`

## Troubleshooting & FAQ

**Q: I see no endpoints in the spec.**
A: Verify your controllers are annotated with `@RestController` or have valid path mappings. Use `--lang` if not Java.


**Q: Nested generics aren’t resolved.**
A: The Java parser currently supports single-level generics. Contribute a fix in `ModelParser.java`.

**Q: Can I skip IR generation?**
A: Yes—use `--input parsed.json` to feed an existing IR file directly.
