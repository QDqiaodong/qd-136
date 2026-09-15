package com.surf.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 现场对账：馆长在现场另记的两份小计数（纸质台账 / 对讲机回报），
 * 与页上算出的今晚合计逐项核对。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreWaveReconcileDTO {

    @NotNull(message = "现场打滑合计不能为空")
    @PositiveOrZero(message = "现场打滑合计不能为负")
    private Long onSiteSlipCount;

    @NotNull(message = "现场移位合计不能为空")
    @PositiveOrZero(message = "现场移位合计不能为负")
    private BigDecimal onSiteShiftCm;
}
