package de.thm.spec2tool.dto;
import java.util.List;


public record ApiSpecificationDto(String name, String minVersion, String maxVersion, List<String> fileTypes) {}

