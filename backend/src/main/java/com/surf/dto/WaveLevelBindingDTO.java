package com.surf.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WaveLevelBindingDTO {
    
    @NotNull(message = "设备ID不能为空")
    private Long equipmentId;
    
    @NotBlank(message = "浪高档位编码不能为空")
    private String waveLevelCode;
    
    @NotBlank(message = "绑定类型不能为空")
    private String bindingType;
}
