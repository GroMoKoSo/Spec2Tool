package de.thm.spec2tool.service;

import de.thm.spec2tool.dto.ApiSpecificationDto;
import de.thm.spec2tool.dto.ToolSetDto;
import de.thm.spec2tool.mapper.OpenApiMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ConversionServiceImpl implements ConversionService {

    Logger logger = LoggerFactory.getLogger(ConversionServiceImpl.class);

    @Override
    public ToolSetDto convert(Map<String, Object> spec, String format) {
        if ("openapi".equalsIgnoreCase(format)) {
            try {
                return new OpenApiMapper().mapper(spec);
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        }
        return null;
    }

    @Override
    public List<ApiSpecificationDto> getSupportedApiSpec() {
        return List.of();
    }
}