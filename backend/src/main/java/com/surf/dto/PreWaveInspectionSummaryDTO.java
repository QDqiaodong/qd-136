package com.surf.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 今晚点检合计：页上两个总数字段。
 * 明细每改一条数字（打滑次数 / 移位厘米），合计由后端重新汇总，前端立即跟上。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreWaveInspectionSummaryDTO {

    private LocalDate inspectionDate;

    /** 今晚已点设备数（按设备去重后的条数）。 */
    private long inspectedCount;

    /** 今晚握把打滑次数合计。 */
    private long totalSlipCount;

    /** 今晚挡垫移位合计（厘米）。 */
    private BigDecimal totalShiftCm;
}
