package de.thm.spec2tool.service;

import de.thm.spec2tool.dto.ApiSpecificationDto;
import de.thm.spec2tool.dto.ToolSetDto;

import java.util.List;
import java.util.Map;

public interface ConversionService {

    ToolSetDto convert(Map<String,Object> spec , String format);

    List<ApiSpecificationDto> getSupportedApiSpec();

}
