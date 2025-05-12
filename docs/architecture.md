# AutoDoc Architecture

This document describes the architecture of AutoDoc, a modular system that produces OpenAPI specifications in two decoupled phases.

---

## Goals

* **Modularity**: Keep parsing logic separate from spec generation.
* **Language Agnostic**: Support multiple parser front‑ends (Java, Kotlin, etc.) emitting the same IR.
* **Centralized OpenAPI Logic**: Concentrate all schema and spec‑building rules in the Go CLI.
* **Extensibility**: Easily add new metadata (vendor extensions, deprecation flags) or security requirements without touching parsers.

---

## System Overview

AutoDoc consists of two primary modules:

1. **Parser Module (`java-parser`)**

   * **Purpose**: Analyze application source (controllers, DTOs/entities) and emit a minimal Intermediate Representation (IR).
   * **Inputs**: Source code directory (e.g. `src/main/java`).
   * **Outputs**: `parsed.json`, matching the IR schema below.

2. **Spec Generator (`go-autodoc`)**

   * **Purpose**: Consume the IR and produce a fully valid OpenAPI v3 document.
   * **Inputs**: `parsed.json` plus user‑supplied metadata (`--info`, `--servers`, optional `--security-requirements`).
   * **Outputs**: `openapi.yaml` (or JSON).

---

## IR JSON Schema

```jsonc
{
  "models": [ ModelData ],
  "endpoints": [ EndpointData ],
  "securitySchemes"?: [ SecuritySchemeData ]
}
```

* **ModelData**

  ```jsonc
  {
    "name": "UserDto",
    "description": "Class-level Javadoc or annotations",
    "fields": [
      {
        "name": "id",
        "required": true,
        "description": "Field-level Javadoc or @NotNull",
        "typeRef": { "base": "Long", "args": [] }
      }
    ]
  }
  ```

* **EndpointData**

  ```jsonc
  {
    "path": "/users/{id}",
    "method": "GET",
    "summary": "Optional @Operation summary",
    "description": "Optional @Operation description",
    "tags": ["User"],
    "parameters": [ ParameterData ],
    "requestBodyType": { "base": "UserDto", "args": [] } | null,
    "responseType": { "base":"ApiResponse","args":[ { "base":"UserDto","args":[] } ] }
  }
  ```

* **ParameterData**

  ```jsonc
  {
    "name": "id",
    "in": "path",
    "required": true,
    "description": "",
    "typeRef": { "base": "Long", "args": [] }
  }
  ```

* **TypeRefData**

  ```jsonc
  {
    "base": "List",        // raw type name
    "args": [               // generic arguments (empty list if none)
      { "base": "Product", "args": [] }
    ]
  }
  ```

---

## Data Flow

1. **Parse**:

   ```bash
   java -jar autodoc-parser.jar --source src/main/java > parsed.json
   ```
2. **Generate Spec**:

   ```bash
   go-autodoc \
     --input parsed.json \
     --info title="My API",version="1.0.0" \
     --servers url="https://api.example.com" \
     --output openapi.yaml
   ```
3. **Publish**: Deploy `openapi.yaml` to your API gateway or documentation site.

---

## Supporting Documents

* **`overview.md`**: Reflect the IR‑first workflow and CLI usage.
* **`README.md`** (java-parser, go-autodoc): Update usage examples and flags.
* **Developer Guide**: Add sections on creating new parsers and extending the CLI builder.
* **CI/CD Config**: Ensure pipelines execute parser then CLI rather than legacy spec generators.

---

## Extensibility

* **New Parsers**: Add support for other languages by emitting the same IR.
* **Custom Metadata**: Extend IR with vendor‑specific fields or deprecation markers.
* **Security Requirements**: Support global and operation‑level security entries via IR and CLI flags.

