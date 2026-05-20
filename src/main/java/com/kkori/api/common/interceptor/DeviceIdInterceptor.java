package com.kkori.api.common.interceptor;

import com.kkori.api.common.exception.BusinessException;
import com.kkori.api.common.exception.ErrorCode;
import com.kkori.api.auth.context.AuthContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class DeviceIdInterceptor implements HandlerInterceptor {

    public static final String DEVICE_ID_ATTRIBUTE = "deviceId";
    private static final String DEVICE_ID_HEADER = "X-Device-Id";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String deviceId = request.getHeader(DEVICE_ID_HEADER);
        if ((deviceId == null || deviceId.isBlank()) && AuthContext.currentUser().isPresent()) {
            return true;
        }
        if (deviceId == null || deviceId.isBlank()) {
            throw new BusinessException(ErrorCode.DEVICE_001);
        }
        request.setAttribute(DEVICE_ID_ATTRIBUTE, deviceId);
        return true;
    }
}
