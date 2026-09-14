package com.surf.security;

/**
 * 访问被拒绝时抛出。馆长拥有全部权限；教练越权操作未授权档位/设备时抛出本异常。
 */
public class AccessDeniedException extends RuntimeException {

    public AccessDeniedException(String message) {
        super(message);
    }
}
