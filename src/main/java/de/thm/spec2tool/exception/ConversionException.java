package de.thm.spec2tool.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR, reason = "Error during conversion of Tool")
public class ConversionException extends RuntimeException {
    public ConversionException(String message) {
        super(message);
    }
}
