package com.surf.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "equipment_adjust_record", indexes = {
    @Index(name = "idx_ear_equipment_id", columnList = "equipment_id"),
    @Index(name = "idx_ear_adjust_time", columnList = "adjust_time")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EquipmentAdjustRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "equipment_id", nullable = false)
    private Long equipmentId;
    
    @Column(name = "equipment_code", nullable = false, length = 50)
    private String equipmentCode;
    
    @Column(name = "equipment_name", nullable = false, length = 100)
    private String equipmentName;
    
    @Column(name = "previous_wave_level_code", length = 20)
    private String previousWaveLevelCode;
    
    @Column(name = "previous_wave_level_name", length = 50)
    private String previousWaveLevelName;
    
    @Column(name = "new_wave_level_code", nullable = false, length = 20)
    private String newWaveLevelCode;
    
    @Column(name = "new_wave_level_name", nullable = false, length = 50)
    private String newWaveLevelName;
    
    @Column(name = "adjust_reason", length = 500)
    private String adjustReason;
    
    @Column(name = "operator", nullable = false, length = 50)
    private String operator;
    
    @Column(name = "adjust_time", nullable = false)
    @Builder.Default
    private LocalDateTime adjustTime = LocalDateTime.now();
    
    @Column(name = "remark", length = 500)
    private String remark;
}
