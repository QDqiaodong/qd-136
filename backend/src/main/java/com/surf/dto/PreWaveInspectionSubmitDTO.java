package com.surf.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 开浪前点检录入 / 复点修改。
 * 同一台设备同一晚重复提交时按“更新”处理（合计只算一次）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreWaveInspectionSubmitDTO {

    @NotNull(message = "设备不能为空")
    private Long equipmentId;

    /** 握把打滑次数（次），防滑扶手必填，不能为负。 */
    @NotNull(message = "握把打滑次数不能为空")
    @PositiveOrZero(message = "握把打滑次数不能为负")
    private Integer slipCount;

    /** 挡垫移位（厘米），缓冲挡垫必填，不能为负。 */
    @NotNull(message = "挡垫移位不能为空")
    @PositiveOrZero(message = "挡垫移位不能为负")
    private BigDecimal shiftCm;

    private String remark;
}
