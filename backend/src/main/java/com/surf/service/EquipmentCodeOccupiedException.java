package com.surf.service;

/**
 * 改号撞号：两人几乎同时把两台设备改成同一个新号时，数据库唯一约束只放行一台，
 * 后提交的那台在落库时撞约束，本异常携带新号/原号，由全局异常处理查明占用者后返回 400。
 */
public class EquipmentCodeOccupiedException extends RuntimeException {

    private final String newCode;
    private final String oldCode;

    public EquipmentCodeOccupiedException(String newCode, String oldCode) {
        super("设备编号 " + newCode + " 已被占用");
        this.newCode = newCode;
        this.oldCode = oldCode;
    }

    public String getNewCode() {
        return newCode;
    }

    public String getOldCode() {
        return oldCode;
    }
}
