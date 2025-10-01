# Spec2Tool

Spec2Tool is a microservice in the GroMoKoSo ecosystem.
Its primary task is to convert API specifications (e.g., OpenAPI documents) into MCP tool definitions that can be consumed by the MCP server.

This service enables seamless integration of external APIs into the GroMoKoSo tool layer without manual specification work.

## Prerequisites

- Java 17+
- Maven 
- Spring Boot

## Installation and usage

### Build

```
mvn clean install
```

### Run

```
mvn spring-boot:run
```

## Documentation

- [architecture_arc42.md](https://github.com/GroMoKoSo/GroMoKoSo-Meta/blob/master/docs/architecture_arc42.md)
