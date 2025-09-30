package de.thm.spec2tool.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.thm.spec2tool.dto.ToolSetDto;
import de.thm.spec2tool.mapper.OpenApiMapper;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ConversionServiceImpl implements ConversionService {

    private final OpenApiMapper openApiMapper;

    Logger logger = LoggerFactory.getLogger(ConversionServiceImpl.class);

    public ConversionServiceImpl(OpenApiMapper openApiMapper) {
        this.openApiMapper = openApiMapper;
    }

    @Override
    public ToolSetDto convert(Map<String, Object> spec, String format) throws JsonProcessingException {
        if ("openapi".equalsIgnoreCase(format)) {
            return openApiMapper.convert(spec);
        } else {
            logger.error("Format not supported: {}", format);
            throw new NotImplementedException("No conversion for format " + format);
        }
    }

    @Override
    public List<String> getSupportedApiSpec() {
        return List.of("openapi");
    }
}