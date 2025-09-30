package service;

import dto.ApiSpecificationDto;
import dto.ToolSpecificationDto;

import java.util.List;
import java.util.Map;

public interface ConversionService {

    public ToolSpecificationDto convert(Map<String,Object> spec , String format);

    public List<ApiSpecificationDto> getSupportedApiSpec();

}
