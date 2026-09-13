package com.surf.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EquipmentUpdateDTO {
    
    @Size(max = 100, message = "设备名称长度不能超过100")
    private String equipmentName;
    
    @Size(max = 50, message = "设备类型长度不能超过50")
    private String equipmentType;
    
    private BigDecimal bufferThickness;
    
    @Size(max = 200, message = "规格长度不能超过200")
    private String specification;
    
    @Size(max = 100, message = "位置长度不能超过100")
    private String location;
    
    @Size(max = 500, message = "备注长度不能超过500")
    private String remark;
}
