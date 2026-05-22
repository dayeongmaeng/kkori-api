package com.kkori.api.common.config;

import com.kkori.api.common.interceptor.DeviceIdInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final DeviceIdInterceptor deviceIdInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(deviceIdInterceptor)
                .addPathPatterns("/api/v1/**")
                .excludePathPatterns(
                        "/api/v1/health",
                        "/api/v1/devices/register",
                        "/api/v1/auth/oauth/login",
                        "/api/v1/auth/refresh",
                        "/api/v1/auth/logout",
                        "/api/v1/photos/*/share"
                );
    }
}
