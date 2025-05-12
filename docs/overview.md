# AutoDoc: Project Overview

## Summary

**AutoDoc** is a command-line tool that automatically generates documentation from Java codebases. Its primary focus is on producing OpenAPI Specifications (Swagger) for REST APIs written using Spring Boot or similar Java web frameworks. The tool aims to reduce manual effort, improve consistency, and streamline developer onboarding by keeping documentation in sync with source code.

## Goals & Use Cases

### Primary Goals

* Generate accurate and up-to-date API documentation.
* Simplify onboarding for new developers.
* Maintain project clarity across large teams.
* Integrate into rapid development cycles.

### Key Use Cases

* **Agile Development**: Automatically update documentation as code evolves.
* **Team Onboarding**: Help new team members quickly understand code structure and API endpoints.
* **Project Communication**: Provide clear API specifications for technical leads and project managers.
* **Component Documentation**: Generate overviews of key components and services across the application.
* **Model Documentation**: Provide field-level and type information for request/response models.
* **Service Documentation**: Detail dependencies and responsibilities of service-layer classes.
* **Security Overview**: Include planned documentation of authentication and authorization mechanisms.

## Target Users

* **Engineers**: Direct users generating documentation from their Java projects.
* **Technical Leads/Architects**: Ensuring documentation consistency and clarity across the project.
* **Project Managers**: Access to standardized documentation for communication and planning.

## Key Features (MVP)

* **Java API Documentation Extraction**: Extracts information from REST controllers using Spring annotations and generates OpenAPI specs.
* **Data Model Documentation**: Parses Java classes to extract model field names, types, and annotations.
* **Component Overview**: High-level module descriptions based on directory and package structures.

## Technical Design Choices

* **OpenAPI First**: Chosen for its machine-readability, interactive Swagger UI potential, and wide adoption in modern API design.
* **Go for CLI Orchestrator**:

  * High performance and fast startup.
  * Concurrency for parallel processing.
  * Easy deployment via statically linked binaries.
* **Java for Parsing**:

  * Utilizes the mature [JavaParser](https://javaparser.org/) library.
  * Allows deep analysis of source code with support for annotations, syntax trees, and comments.

## Implementation Architecture

* **Java CLI Tool**: Parses Java files and outputs structured JSON.
* **Go CLI Tool**: Takes user input, calls Java tool, reads JSON, and generates OpenAPI spec.
* **Communication Strategy**:

  * Go spawns Java process using `os/exec`.
  * Java writes output to file or stdout.
  * Go reads and unmarshals JSON to generate final documentation.

## Planned Enhancements (Future Work)

* Swagger UI generation
* Dependency graph visualization
* Code complexity metrics
* Security vulnerability scanning
* Data flow diagram generation
* Customizable documentation templates
* Git integration for branch/tag-specific docs
* Additional language support (e.g., Kotlin)
* Annotation-based documentation extensions (custom tags, examples, etc.)

## Conclusion

AutoDoc combines the strengths of Java static analysis and Go CLI ergonomics to deliver a fast, reliable, and extensible documentation generator tailored for Java codebases. It lays the foundation for a comprehensive suite of tooling to support modern enterprise development workflows.
