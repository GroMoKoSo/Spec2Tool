package de.thm.spec2tool.service;

import de.thm.spec2tool.dto.ToolSpecificationDto;

public abstract class AMapper {

    /**
     * Abstract method to convert an API spec into a tool specification.
     * @param spec The API specification as a String.
     * @param fileType The type of file (e.g., "json", "yaml").
     * @return A ToolSpecification object.
     */
    public abstract ToolSpecificationDto convert(String spec, String fileType);
}
