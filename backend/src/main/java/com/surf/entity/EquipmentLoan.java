package com.surf.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 设备领用台账：一条记录对应一次“领用—归还”。
 *
 * 归还时间（{@link #returnedAt}）为 null 即“仍在借”，是设备名单显示在借的唯一依据；
 * 写入归还时间后视为已还清，设备才可被再次领用。台账与设备名单共用同一份状态，
 * 不会出现“设备名单仍空闲、实际已借给教练”的两本账对不上。
 */
@Entity
@Table(name = "equipment_loan", indexes = {
    @Index(name = "idx_el_equipment_id", columnList = "equipment_id"),
    @Index(name = "idx_el_borrowed_at", columnList = "borrowed_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EquipmentLoan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "equipment_id", nullable = false)
    private Long equipmentId;

    /** 领用当下冗余设备编号/名称，设备改名或停用后台账仍能读出原信息。 */
    @Column(name = "equipment_code", nullable = false, length = 50)
    private String equipmentCode;

    @Column(name = "equipment_name", nullable = false, length = 100)
    private String equipmentName;

    /** 领用人：当班教练姓名（登记时手填，必填）。 */
    @Column(name = "borrowed_by", nullable = false, length = 50)
    private String borrowedBy;

    @Column(name = "borrowed_at", nullable = false)
    @Builder.Default
    private LocalDateTime borrowedAt = LocalDateTime.now();

    /** 归还时间：null 表示尚未归还（在借）；非 null 表示已还清。 */
    @Column(name = "returned_at")
    private LocalDateTime returnedAt;

    /** 实际归还登记人，可为空（由谁点归还就记谁，这里记姓名文本）。 */
    @Column(name = "returned_by", length = 50)
    private String returnedBy;

    @Column(name = "remark", length = 500)
    private String remark;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
