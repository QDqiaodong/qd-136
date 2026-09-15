package com.surf.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 开浪前点检盘点记录。
 * 一台设备在同一个点检日期最多只有一条有效记录：
 * 同设备再点一遍走“更新”而不是新增，因此合计里同一台设备只算一次。
 */
@Entity
@Table(name = "pre_wave_inspection", uniqueConstraints = {
        @UniqueConstraint(name = "uk_pwi_inspection_date_equipment",
                columnNames = {"inspection_date", "equipment_id"})
}, indexes = {
        @Index(name = "idx_pwi_inspection_date", columnList = "inspection_date"),
        @Index(name = "idx_pwi_equipment_id", columnList = "equipment_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreWaveInspection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 点检日期（开浪当晚）。 */
    @Column(name = "inspection_date", nullable = false)
    private LocalDate inspectionDate;

    @Column(name = "equipment_id", nullable = false)
    private Long equipmentId;

    /** 冗余设备编号/名称/类型，避免设备档案变更影响当晚盘点台账。 */
    @Column(name = "equipment_code", nullable = false, length = 50)
    private String equipmentCode;

    @Column(name = "equipment_name", nullable = false, length = 100)
    private String equipmentName;

    /** 设备类型：防滑扶手 / 缓冲挡垫。 */
    @Column(name = "equipment_type", nullable = false, length = 50)
    private String equipmentType;

    /** 握把打滑次数（次）。仅防滑扶手有效，缓冲挡垫为 0。 */
    @Column(name = "slip_count", nullable = false)
    @Builder.Default
    private Integer slipCount = 0;

    /** 挡垫移位（厘米）。仅缓冲挡垫有效，防滑扶手为 0。 */
    @Column(name = "shift_cm", nullable = false, precision = 8, scale = 1)
    @Builder.Default
    private BigDecimal shiftCm = BigDecimal.ZERO;

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
