package de.thm.spec2tool.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record ToolSetDto(
        @NotEmpty
        @Schema(description = "Unique identifier for the tool set")
        String name,
        @NotEmpty
        @Schema(description = "Human-readable description of the tool set")
        String description,
        @Valid
        @NotEmpty
        @Schema(description = "List of tools to include in the tool set. One tool per API endpoint")
        ToolDto[] tools) {
}