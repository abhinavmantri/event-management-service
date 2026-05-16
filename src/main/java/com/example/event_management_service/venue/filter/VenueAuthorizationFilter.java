package com.example.event_management_service.venue.filter;

import com.example.event_management_service.shared.model.UserRole;
import com.example.event_management_service.shared.service.JWTService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

@Component
public class VenueAuthorizationFilter extends OncePerRequestFilter {
    private static final String VENUES_API_PATH = "/venues";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String ROLE_CLAIM = "role";

    private final JWTService jwtService;

    public VenueAuthorizationFilter(JWTService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        boolean isCreateVenuePath = VENUES_API_PATH.equals(requestUri) || requestUri.endsWith(VENUES_API_PATH);
        return !"POST".equalsIgnoreCase(request.getMethod())
                || !isCreateVenuePath;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authorizationToken = request.getHeader(AUTHORIZATION_HEADER);
        if (!StringUtils.hasText(authorizationToken)) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"message\":\"Authorization token is required\"}");
            return;
        }

        try {
            String jwtToken = authorizationToken.startsWith(BEARER_PREFIX)
                    ? authorizationToken.substring(BEARER_PREFIX.length()).trim()
                    : authorizationToken.trim();
            Map<String, Object> claims = jwtService.validateAndExtractClaims(jwtToken);
            String role = claims.get(ROLE_CLAIM) == null ? null : claims.get(ROLE_CLAIM).toString().trim();
            if (!UserRole.ADMIN.name().equalsIgnoreCase(role)
                    && !UserRole.ORGANIZER.name().equalsIgnoreCase(role)) {
                response.setStatus(HttpStatus.FORBIDDEN.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write("{\"message\":\"Insufficient role for venue endpoint\"}");
                return;
            }
        } catch (IllegalArgumentException ex) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"message\":\"Invalid authorization token\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
