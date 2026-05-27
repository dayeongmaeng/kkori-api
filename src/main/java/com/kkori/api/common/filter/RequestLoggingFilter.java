package com.kkori.api.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String REQUEST_ID_MDC_KEY = "requestId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestId = UUID.randomUUID().toString().replace("-", "");
        MDC.put(REQUEST_ID_MDC_KEY, requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);
        long startMs = System.currentTimeMillis();

        try {
            filterChain.doFilter(request, response);
        } finally {
            long elapsedMs = System.currentTimeMillis() - startMs;
            Long userId = (Long) request.getAttribute("userId");
            if (userId != null) {
                log.info("HTTP {} {} {} {}ms requestId={} userId={}",
                        request.getMethod(), request.getRequestURI(),
                        response.getStatus(), elapsedMs, requestId, userId);
            } else {
                log.info("HTTP {} {} {} {}ms requestId={}",
                        request.getMethod(), request.getRequestURI(),
                        response.getStatus(), elapsedMs, requestId);
            }
            MDC.clear();
        }
    }
}
