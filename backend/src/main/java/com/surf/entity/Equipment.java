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

    // ===== 领用在借状态（不入库） =====
    // 设备名单读出时由服务层按领用台账（equipment_loan）回填，台账与名单共用同一份状态：
    // 在借即标在借并写明借用人，归还后清空。关页再开重新从台账计算，不会残留旧标记。

    /** 是否仍在借（有未归还的领用记录）。 */
    @Transient
    private Boolean borrowed = false;

    /** 当前借用人（当班教练姓名）；未在借为 null。 */
    @Transient
    private String borrowedBy;

    /** 领用时间；未在借为 null。 */
    @Transient
    private LocalDateTime borrowedAt;

    /** 当前在借的领用记录 id；未在借为 null，供前端发起归还。 */
    @Transient
    private Long activeLoanId;

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
