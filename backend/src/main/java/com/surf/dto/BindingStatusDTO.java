package com.surf.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 绑定页“绑定状态”列表行：设备 + 当前生效绑定的展平结构。
 * 列表内容已按当前角色授权范围在服务端收窄，前端直接展示即可。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BindingStatusDTO {

    // 设备信息
    private Long equipmentId;
    private String equipmentCode;
    private String equipmentName;
    private String equipmentType;
    private BigDecimal bufferThickness;
    private String location;

    // 当前生效绑定信息
    private Long bindingId;
    private String waveLevelCode;
    private String waveLevelName;
    private String bindingType;
    private LocalDateTime effectiveDate;
}
