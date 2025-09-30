package service;

import dto.ApiSpecificationDto;
import dto.ToolSpecificationDto;

public abstract class AMapper {

    // Constant holding the specification type
    public static final String SPEC = "ApiSpecification";

    /**
     * Abstract method to convert an API spec into a tool specification.
     * @param spec The API specification as a String.
     * @param fileType The type of file (e.g., "json", "yaml").
     * @return A ToolSpecification object.
     */
    public abstract ToolSpecificationDto convert(String spec, String fileType);
}
