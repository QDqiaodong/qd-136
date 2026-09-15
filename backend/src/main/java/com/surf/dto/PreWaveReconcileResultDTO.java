package com.surf.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 现场对账结果：页上合计 vs 现场计数。
 * diff 为 0 即账实相符；任一不符 balanced=false。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreWaveReconcileResultDTO {

    /** 页上：今晚打滑合计。 */
    private long sheetSlipCount;
    /** 现场：打滑合计。 */
    private long onSiteSlipCount;
    /** 打滑差异 = 页上 - 现场。 */
    private long slipDiff;
    /** 打滑项是否相符。 */
    private boolean slipMatched;

    /** 页上：今晚移位合计（厘米）。 */
    private BigDecimal sheetShiftCm;
    /** 现场：移位合计（厘米）。 */
    private BigDecimal onSiteShiftCm;
    /** 移位差异 = 页上 - 现场。 */
    private BigDecimal shiftDiff;
    /** 移位项是否相符。 */
    private boolean shiftMatched;

    /** 两项全部相符。 */
    private boolean balanced;
}
