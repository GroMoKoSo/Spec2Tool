package de.thm.spec2tool.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.thm.spec2tool.dto.ToolSetDto;

import java.util.List;
import java.util.Map;

public interface ConversionService {

    ToolSetDto convert(Map<String,Object> spec , String format) throws JsonProcessingException;

    List<String> getSupportedApiSpec();

}
