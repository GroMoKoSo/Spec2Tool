package de.thm.spec2tool.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

import java.util.Map;

public record ToolDto(
        @NotEmpty
        @Schema(description = "Unique identifier for the tool")
        String name,
        @NotEmpty
        @Schema(description = "Human-readable description of functionality")
        String description,
        @NotEmpty
        @Schema(description = "HTTP request method")
        String requestMethod,
        @NotEmpty
        @Schema(description = "Endpoint URL of the API")
        String endpoint,
        @Schema(description = "JSON Schema defining expected parameters")
        Map<String, Object> inputSchema) {
}
