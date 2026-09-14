package com.surf.service;

import com.surf.entity.Equipment;
import com.surf.entity.WaveLevel;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 缓冲挡垫与浪高档位的适浪厚度规则。
 *
 * 仅“缓冲挡垫”受厚度约束：目标档位配置了最小缓冲厚度（minBufferThickness）时，
 * 挡垫的缓冲厚度必须达到该下限，否则禁止绑定/调整到该档位。
 * 其他设备（如防滑扶手）与未配置下限的档位不受此规则限制。
 */
@Service
public class BufferThicknessRuleService {

    /** 受厚度规则约束的设备类型。 */
    public static final String BUFFER_PAD_TYPE = "缓冲挡垫";

    /**
     * 校验设备是否满足目标档位的缓冲厚度要求；不满足直接抛 IllegalArgumentException，
     * 由全局异常处理转为 400，调用方必须在任何数据落库之前调用。
     */
    public void assertThicknessSatisfied(Equipment equipment, WaveLevel targetWaveLevel) {
        if (equipment == null || targetWaveLevel == null) {
            return;
        }
        if (!BUFFER_PAD_TYPE.equals(equipment.getEquipmentType())) {
            return;
        }
        BigDecimal minimum = targetWaveLevel.getMinBufferThickness();
        if (minimum == null) {
            return;
        }
        BigDecimal thickness = equipment.getBufferThickness();
        if (thickness == null || thickness.compareTo(minimum) < 0) {
            String actualText = thickness == null
                    ? "未登记"
                    : thickness.stripTrailingZeros().toPlainString() + "cm";
            throw new IllegalArgumentException(String.format(
                    "厚度不足：%s 的缓冲厚度为%s，未达到%s档位的厚度下限 %scm，不能绑定到该档位",
                    equipment.getEquipmentName(),
                    actualText,
                    targetWaveLevel.getLevelName(),
                    minimum.stripTrailingZeros().toPlainString()));
        }
    }
}
