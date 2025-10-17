package com.project.transaction_service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.transaction_service.dto.data.UserData;
import com.project.transaction_service.dto.response.Base;
import com.project.transaction_service.utils.AuthValidator;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {
    private final AuthValidator authValidator;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String token = request.getHeader(HttpHeaders.AUTHORIZATION);

        String path = request.getRequestURI();
        if (path.startsWith("/api/auth")) {
            filterChain.doFilter(request, response);
            return;
        }

        if (token == null || token.isEmpty()) {
            writeErrorResponse(response, HttpStatus.UNAUTHORIZED, "Missing Authorization header");
            return;
        }

        try {
            UserData user = authValidator.validateToken(token);

            // Masukkan ke SecurityContext
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(user, null, List.of());
            SecurityContextHolder.getContext().setAuthentication(authentication);

            filterChain.doFilter(request, response);
        } catch (Exception e) {
            writeErrorResponse(response, HttpStatus.UNAUTHORIZED, "Invalid or expired token");
        }
    }


    private void writeErrorResponse(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json");

        Base<Object> apiResponse = Base.builder()
                .error(message)
                .status(status.value())
                .build();

        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }
}
