# AutoDoc Architecture

## Goals

* **Accurate, Maintainable Docs**: Keep parsing logic in language-specific modules; concentrate all OpenAPI and doc-generation rules in the Go CLI.
* **Language-Agnostic**: Support Java, Kotlin, Python, etc., via pluggable parser front-ends.
* **Extensible Outputs**: Out-of-the-box OpenAPI JSON/YAML, Markdown tables, Mermaid diagrams, with hooks for more formats.
* **CI/CD Friendly**: Single cross-platform CLI integrates seamlessly into pipelines.

---

## System Overview

AutoDoc is composed of two main components:

1. **Parser Modules**

   * **Purpose**: Statically analyze source code and emit a language-agnostic IR JSON.
   * **Java Parser** (current reference implementation)

     * **Entry Point**: `AutodocApplication` gathers all `CompilationUnit`s via JavaParser and invokes sub-parsers.
     * **ModelParser**:

       * Scans `ClassOrInterfaceDeclaration` and `EnumDeclaration`, skipping non-model classes (services, controllers, etc.).
       * Extracts class-level descriptions from `@Schema`/Javadoc, then fields into `FieldData` with name, type (`TypeRefData`), `required` (from `@NotNull`), and Javadoc comments.
     * **ControllerParser**:

       * Finds `@RestController`/`@Controller` classes, skips `@ControllerAdvice`.
       * Reads class-level `@RequestMapping` base path.
       * For each `@GetMapping`, `@PostMapping`, etc., extracts HTTP method, path, `@Operation`/Javadoc summary and description, tags, parameters (`@PathVariable`, `@RequestParam`), request-body types, and response types into `EndpointData` and `ParameterData`.
     * **IR Classes** (Java side):

       * `ParsedProject` holds `List<ModelData>` and `List<EndpointData>`.
       * `ModelData` (name, description, `List<FieldData>`).
       * `FieldData` (name, `TypeRefData`, required, description).
       * `EndpointData` (path, method, summary, description, tags, `List<ParameterData>`, request/response types).
       * `ParameterData` (name, in, required, description, `TypeRefData`).
       * `TypeRefData` (`base`, generic `args`).
     * **Output**: Serializes the populated `ParsedProject` to JSON via Jackson (pretty-printed).

2. **Go CLI Orchestrator (`go-autodoc`)**

   * **Purpose**: Invoke parser, consume IR, and generate OpenAPI spec plus optional tables/diagrams.
   * **Parser Invocation**:

     * Embeds the Java parser JAR via `//go:embed parser.jar` and `CallJavaParser`, writing it to a temp file and executing `java -jar` under the hood.
     * `parser.Parse(srcDir)` unmarshals the resulting JSON into Go IR types (`parser.IR`, `parser.ModelData`, `parser.EndpointData`, etc.).
   * **Spec Builder**:

     * `generator.BuildOpenAPISpec` takes `parser.IR`, `info` map, and `servers` list to produce an `interface{}` representing the OpenAPI 3.0 document.
     * **Schemas**: `buildSchemas` handles enums vs. objects, mapping Java types to OpenAPI primitives or `$ref`s.
     * **Paths**: `buildPaths` assembles each `EndpointData` into path items, uses `buildParameters`, `buildRequestBody`, and `buildResponses` to inline generic collections, wrap response types, and reference schemas as needed.
   * **CLI Flags** (`main.go` via Cobra):

     * `--source, -s` (required): path to source directory
     * `--info, -i`: metadata (`title="..."`,`version="..."`), parsed by `parseInfo`
     * `--servers, -S`: server entries (`url="...",description="..."`), parsed by `parseServers`
     * `--output, -o`: spec output path (default `openapi.yaml`)
     * `--lang, -l`: parser language (`java|kotlin|python`)
     * `--tables`: comma-separated table generators (e.g. `endpoint-table`,`model-table`)
     * `--diagrams`: comma-separated diagram generators (e.g. `endpoint-map`)
   * **Supplementary Document Generators**:

     * **Endpoint Table**: `generator.GenerateEndpointTable` writes `endpoint-table.md`, grouping by controller/tag and listing method, path, params, and description.
     * **Model Table**: `generator.GenerateModelTable` writes `model-table.md`, one section per model with a table of fields, types, required flags, and descriptions.
     * **Endpoint Map Diagram**: `generator.GenerateEndpointMap` writes `endpoint-map.mmd`, a Mermaid flowchart grouping endpoints by tag or path prefix.
   * **Execution Flow** (`runGenerate`):

     1. Parse IR (`parser.ParseWithLang`)
     2. Build and write OpenAPI YAML via `yaml.Marshal`
     3. Conditionally invoke table and diagram generators based on flags

---

## IR JSON Schema

```jsonc
{
  "models": [ ModelData ],
  "endpoints": [ EndpointData ]
}
```

* **ModelData**: `{ "name": string, "description": string, "fields": [ FieldData ] }`
* **FieldData**: `{ "name": string, "typeRef": { "base": string, "args": [] }, "required": boolean, "description": string }`
* **EndpointData**: `{ "path": string, "method": string, "summary": string, "description": string, "tags": [string], "parameters": [ ParameterData ], "requestBodyType": TypeRefData|null, "responseType": TypeRefData }`
* **ParameterData**: `{ "name": string, "in": "path"|"query", "required": boolean, "description": string, "type": TypeRefData }`
* **TypeRefData**: `{ "base": string, "args": [ TypeRefData ] }`

---

## Data Flow

1. **Parse**

   ```bash
   java -jar java-parser/target/autodoc-1.0-SNAPSHOT.jar src/main/java > parsed.json
   ```
2. **Generate Spec & Docs**

   ```bash
   go-autodoc \
     --source src/main/java \
     --info title="My API",version="1.0.0" \
     --servers url="https://api.example.com" \
     --output openapi.yaml \
     --tables endpoint-table,model-table \
     --diagrams endpoint-map
   ```
3. **Outputs**

   * `openapi.yaml`: valid OpenAPI v3 spec
   * `endpoint-table.md`, `model-table.md`: comprehensive Markdown tables
   * `endpoint-map.mmd`: Mermaid diagram of your API surface

---

## Supporting Documents

* **`overview.md`**: IR-first workflow and CLI usage.
* **`README.md`**: Combined Java parser & Go CLI usage examples and flags.
* **Developer Guide**: How to add new parsers and extend generators.
* **CI/CD Config**: Pipeline steps for parser and CLI invocations.

---

## Extensibility

* **New Language Parsers**: Implement the same IR shape in any language; drop into `--lang`.
* **Additional Doc Generators**: Hook new table or diagram types in `main.go`.
* **Vendor Extensions**: Extend IR (e.g. `securitySchemes`) and update Go builder.
* **Security Requirements**: Add `--security-requirements` flag support for global/operation-level security.
