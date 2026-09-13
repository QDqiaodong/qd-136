package com.surf.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EquipmentAdjustDTO {
    
    @NotNull(message = "设备ID不能为空")
    private Long equipmentId;
    
    @NotBlank(message = "新浪高档位编码不能为空")
    private String newWaveLevelCode;
    
    @Size(max = 500, message = "调整原因长度不能超过500")
    private String adjustReason;
    
    @NotBlank(message = "操作人不能为空")
    @Size(max = 50, message = "操作人长度不能超过50")
    private String operator;
    
    @Size(max = 500, message = "备注长度不能超过500")
    private String remark;
}
