package com.surf.security;

/**
 * 当前请求的操作者上下文，由 {@link RoleInterceptor} 从请求头解析后写入，请求结束时清理。
 */
public final class CurrentUser {

    private static final ThreadLocal<Role> ROLE_HOLDER = new ThreadLocal<>();

    private CurrentUser() {
    }

    public static void setRole(Role role) {
        ROLE_HOLDER.set(role);
    }

    /**
     * 当前角色；未携带角色头时返回配置的默认角色（见 AccessControlService），
     * 这里仅保存请求头解析出的原始值，默认值逻辑统一放在 AccessControlService。
     */
    public static Role getRole() {
        return ROLE_HOLDER.get();
    }

    public static void clear() {
        ROLE_HOLDER.remove();
    }
}
