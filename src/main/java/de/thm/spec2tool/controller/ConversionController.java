package de.thm.spec2tool.controller;

import de.thm.spec2tool.dto.ToolSetDto;
import de.thm.spec2tool.dto.ToolSpecificationDto;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@Tag(name = "Spec2Tool", description = "All Endpoints related to convert specifications to gromokoso tool definition.")
public interface ConversionController {

    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully returned all supported specifications",
                    content = { @Content(mediaType = "application/json",
                            array = @ArraySchema( schema = @Schema(implementation = String.class)))}),
            @ApiResponse(responseCode = "401", description = "Not authorized to perform this request.",
                    content = @Content)}
    )
    @GetMapping("/convert")
    List<String> getSupportedSpecifications();

    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Added new User",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ToolSetDto.class)) }),
            @ApiResponse(responseCode = "401", description = "Not authorized to perform this request.",
                    content = @Content),
            @ApiResponse(responseCode = "422", description = "Invalid Data in Payload",
                    content = @Content)}
    )
    @PostMapping("/convert")
    ToolSetDto convertSpecToTool(@RequestBody ToolSpecificationDto specification);
}
