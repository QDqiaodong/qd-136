package com.surf.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 开浪前点检记录。同一台设备同晚只有一条，合计天然不重复。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreWaveInspectionDTO {

    private Long id;
    private LocalDate inspectionDate;
    private Long equipmentId;
    private String equipmentCode;
    private String equipmentName;
    private String equipmentType;
    private Integer slipCount;
    private BigDecimal shiftCm;
    private String remark;
    private LocalDateTime updatedAt;
}
