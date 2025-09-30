package de.thm.spec2tool.dto;

import java.util.Map;

public record ToolSpecificationDto(String format, Map<String, Object> spec) {}
