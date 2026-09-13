package com.surf.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "equipment", indexes = {
    @Index(name = "idx_equipment_code", columnList = "equipment_code"),
    @Index(name = "idx_equipment_type", columnList = "equipment_type"),
    @Index(name = "idx_equipment_status", columnList = "status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Equipment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "equipment_code", nullable = false, unique = true, length = 50)
    private String equipmentCode;
    
    @Column(name = "equipment_name", nullable = false, length = 100)
    private String equipmentName;
    
    @Column(name = "equipment_type", nullable = false, length = 50)
    private String equipmentType;
    
    @Column(name = "buffer_thickness")
    private BigDecimal bufferThickness;
    
    @Column(name = "specification", length = 200)
    private String specification;
    
    @Column(name = "location", length = 100)
    private String location;
    
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE";
    
    @Column(name = "remark", length = 500)
    private String remark;
    
    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
