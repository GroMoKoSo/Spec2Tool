package service;

import dto.ApiSpecificationDto;
import dto.ToolSpecificationDto;
import dto.ToolDto;
import mapper.OpenApiMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class ConversionServiceImpl implements ConversionService {

    Logger logger = LoggerFactory.getLogger(ConversionServiceImpl.class);

    @Override
    public ToolSpecificationDto convert(Map<String, Object> spec, String format) {
        if ("openapi".equalsIgnoreCase(format)) {
            try {
                return new OpenApiMapper().mapper(spec, format);
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