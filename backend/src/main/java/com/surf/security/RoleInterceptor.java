package com.surf.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 从请求头 X-Role 解析当前操作者角色，写入 {@link CurrentUser}，请求结束时清理。
 * 未携带或无法识别的角色头不在此拦截，默认角色由 AccessControlService 统一兜底。
 */
@Component
public class RoleInterceptor implements HandlerInterceptor {

    public static final String ROLE_HEADER = "X-Role";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Role role = Role.fromHeader(request.getHeader(ROLE_HEADER));
        if (role != null) {
            CurrentUser.setRole(role);
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        CurrentUser.clear();
    }
}
