package com.surf.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EquipmentInfoDTO {
    
    private Long id;
    
    private String equipmentCode;
    
    private String equipmentName;
    
    private String equipmentType;
    
    private BigDecimal bufferThickness;
    
    private String specification;
    
    private String location;
    
    private String status;
    
    private String waveLevelCode;
    
    private String waveLevelName;
    
    private LocalDateTime bindingTime;
}
