package dto;
import java.util.List;


public record ApiSpecificationDto(String name, String monVersion, String maxVersion, List<String> fileTypes) {}

