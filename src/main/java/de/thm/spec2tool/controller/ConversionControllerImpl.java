package de.thm.spec2tool.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.thm.spec2tool.dto.ToolSetDto;
import de.thm.spec2tool.exception.ConversionException;
import de.thm.spec2tool.exception.InvalidTokenException;
import de.thm.spec2tool.security.TokenProvider;
import de.thm.spec2tool.dto.ToolSpecificationDto;
import de.thm.spec2tool.service.ConversionService;
import de.thm.spec2tool.service.ConversionServiceImpl;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ConversionControllerImpl implements ConversionController {

    private final TokenProvider tokenProvider;
    private final ConversionService conversionService;

    public ConversionControllerImpl(TokenProvider tokenProvider, ConversionServiceImpl conversionService) {
        this.tokenProvider = tokenProvider;
        this.conversionService = conversionService;
    }

    @Override
    public List<String> getSupportedSpecifications() {
        try {
            tokenProvider.getToken();
            return conversionService.getSupportedApiSpec();
        } catch (OAuth2AuthenticationException oaae) {
            throw new InvalidTokenException("The authentication token is invalid!");
        }
    }

    @Override
    public ToolSetDto convertSpecToTool(ToolSpecificationDto specification) {
        try {
            tokenProvider.getToken();
            return conversionService.convert(specification.spec(), specification.format());
        } catch (OAuth2AuthenticationException oaae) {
            throw new InvalidTokenException("The authentication token is invalid!");
        } catch (JsonProcessingException | IllegalArgumentException | NotImplementedException e) {
            throw new ConversionException("Cannot convert specification to tool!");
        }
    }
}
