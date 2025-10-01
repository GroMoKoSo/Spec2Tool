# Spec2Tool

Spec2Tool is a microservice in the GroMoKoSo ecosystem.
Its primary task is to convert API specifications (e.g., OpenAPI documents) into MCP tool definitions that can be consumed by the MCP server.

This service enables seamless integration of external APIs into the GroMoKoSo tool layer without manual specification work.

To build the complete GroMoKoSo System, please visit the [Gromokoso-Meta Repository](https://github.com/GroMoKoSo/GroMoKoSo-Meta).

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

- [GroMoKoSo Documentation](https://github.com/GroMoKoSo/GroMoKoSo-Meta/blob/master/docs/architecture_arc42.md)
- [Spec2Tool API-apping](https://github.com/GroMoKoSo/Spec2Tool/blob/master/docs/mapper.md)
