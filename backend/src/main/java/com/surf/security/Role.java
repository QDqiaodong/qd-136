package com.surf.security;

/**
 * 系统内角色。
 * DIRECTOR 馆长：可见并操作全部浪高档位与辅助设备。
 * COACH    浪道教练：仅可见并调整被授权档位上的防滑扶手和缓冲挡垫。
 */
public enum Role {
    DIRECTOR,
    COACH;

    public static Role fromHeader(String raw) {
        if (raw == null) {
            return null;
        }
        String normalized = raw.trim().toUpperCase();
        for (Role role : values()) {
            if (role.name().equals(normalized)) {
                return role;
            }
        }
        return null;
    }
}
