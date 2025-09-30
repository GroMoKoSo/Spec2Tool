package de.thm.spec2tool.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

@Component
public class TokenProvider {

    Logger logger = LoggerFactory.getLogger(TokenProvider.class);

    public String getToken() throws OAuth2AuthenticationException {
        return getJWTToken().getTokenValue();
    }

    public String getUsernameFromToken() throws OAuth2AuthenticationException  {
        return getJWTToken().getClaim("preferred_username");
    }

    private Jwt getJWTToken() throws OAuth2AuthenticationException  {
        logger.info("Trying to get JWT token");
        var token = SecurityContextHolder.getContext().getAuthentication();
        if (!(token instanceof JwtAuthenticationToken jwtToken)) {
            logger.error("JWT token is not a JWT token");
            throw new OAuth2AuthenticationException ("JWT token is not valid");
        }
        return jwtToken.getToken();
    }
}
