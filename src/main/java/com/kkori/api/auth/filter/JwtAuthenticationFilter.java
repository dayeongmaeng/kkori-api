package com.kkori.api.auth.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kkori.api.auth.context.AuthContext;
import com.kkori.api.auth.context.AuthenticatedUser;
import com.kkori.api.auth.jwt.JwtClaims;
import com.kkori.api.auth.jwt.JwtTokenVerifier;
import com.kkori.api.common.dto.ApiResponse;
import com.kkori.api.common.dto.ErrorResponse;
import com.kkori.api.common.exception.BusinessException;
import com.kkori.api.common.exception.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenVerifier jwtTokenVerifier;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return "OPTIONS".equalsIgnoreCase(request.getMethod())
                || path.equals("/api/v1/health")
                || path.equals("/api/v1/auth/oauth/login")
                || path.equals("/api/v1/auth/refresh")
                || path.equals("/api/v1/devices/register")
                || path.matches("/api/v1/photos/[^/]+/share")
                || path.startsWith("/api-docs")
                || path.startsWith("/swagger-ui")
                || path.equals("/swagger-ui.html")
                || path.startsWith("/v3/api-docs")
                || path.equals("/oauth/kakao");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authorization = request.getHeader(AUTHORIZATION_HEADER);
        if (authorization == null || authorization.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }
        if (!authorization.startsWith(BEARER_PREFIX)) {
            log.warn("JWT auth failed: reason=invalid_bearer_format path={}", request.getRequestURI());
            writeUnauthorized(response, ErrorCode.AUTH_003);
            return;
        }

        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        JwtClaims claims;
        try {
            claims = jwtTokenVerifier.verifyAccessToken(token);
        } catch (BusinessException e) {
            log.warn("JWT auth failed: code={} path={}", e.getErrorCode().getCode(), request.getRequestURI());
            writeUnauthorized(response, e.getErrorCode());
            return;
        }

        try {
            AuthContext.set(new AuthenticatedUser(claims.userId(), claims.userExternalId()));
            request.setAttribute("userId", claims.userId());
            request.setAttribute("userExternalId", claims.userExternalId());
            filterChain.doFilter(request, response);
        } finally {
            AuthContext.clear();
        }
    }

    private void writeUnauthorized(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        ErrorCode code = errorCode == ErrorCode.AUTH_004 ? ErrorCode.AUTH_004 : ErrorCode.AUTH_003;
        response.setStatus(code.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(),
                ApiResponse.error(new ErrorResponse(code.getCode(), code.getMessage(), null)));
    }
}
