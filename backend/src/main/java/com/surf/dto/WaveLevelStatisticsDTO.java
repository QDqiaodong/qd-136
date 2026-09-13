package com.surf.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WaveLevelStatisticsDTO {
    
    private String waveLevelCode;
    
    private String waveLevelName;
    
    private String description;
    
    private Integer equipmentCount;
    
    private List<EquipmentInfoDTO> equipmentList;
}
