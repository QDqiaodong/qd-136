package com.surf.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "equipment_wave_level", indexes = {
    @Index(name = "idx_ewl_equipment_id", columnList = "equipment_id"),
    @Index(name = "idx_ewl_wave_level_id", columnList = "wave_level_id"),
    @Index(name = "idx_ewl_wave_level_code", columnList = "wave_level_code")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EquipmentWaveLevel {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "equipment_id", nullable = false)
    private Long equipmentId;
    
    @Column(name = "wave_level_id", nullable = false)
    private Long waveLevelId;
    
    @Column(name = "wave_level_code", nullable = false, length = 20)
    private String waveLevelCode;
    
    @Column(name = "wave_level_name", nullable = false, length = 50)
    private String waveLevelName;
    
    @Column(name = "binding_type", nullable = false, length = 20)
    @Builder.Default
    private String bindingType = "INITIAL";
    
    @Column(name = "effective_date", nullable = false)
    private LocalDateTime effectiveDate;
    
    @Column(name = "expire_date")
    private LocalDateTime expireDate;
    
    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @PreUpdate
    public void preUpdate() {
        this.createdAt = LocalDateTime.now();
    }
}
