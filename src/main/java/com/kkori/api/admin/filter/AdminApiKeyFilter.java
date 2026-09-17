package com.kkori.api.admin.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kkori.api.admin.config.AdminProperties;
import com.kkori.api.admin.dto.response.AdminErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * kkutudio-admin(admin-api)이 서버 대 서버로 호출하는 {@code /internal/admin/**}를 보호한다.
 * 최종 사용자를 위한 {@link com.kkori.api.auth.filter.JwtAuthenticationFilter}와는 별개의,
 * 고정 API 키 기반 인증이다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(AdminProperties.class)
public class AdminApiKeyFilter extends OncePerRequestFilter {

    private static final String ADMIN_PATH_PREFIX = "/internal/admin";
    private static final String API_KEY_HEADER = "X-Admin-Api-Key";

    private final AdminProperties adminProperties;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(ADMIN_PATH_PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String configuredKey = adminProperties.apiKey();
        String providedKey = request.getHeader(API_KEY_HEADER);

        if (configuredKey == null || configuredKey.isBlank()) {
            log.error("Admin API blocked: ADMIN_API_KEY not configured, path={}", request.getRequestURI());
            writeUnauthorized(response);
            return;
        }
        if (providedKey == null || !constantTimeEquals(configuredKey, providedKey)) {
            log.warn("Admin API auth failed: path={}", request.getRequestURI());
            writeUnauthorized(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean constantTimeEquals(String expected, String actual) {
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8)
        );
    }

    private void writeUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), new AdminErrorResponse("invalid or missing admin api key"));
    }
}
