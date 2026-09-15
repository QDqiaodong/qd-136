package com.surf.repository;

import com.surf.entity.EquipmentAdjustRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EquipmentAdjustRecordRepository extends JpaRepository<EquipmentAdjustRecord, Long> {

    List<EquipmentAdjustRecord> findByEquipmentIdOrderByAdjustTimeDesc(Long equipmentId);

    List<EquipmentAdjustRecord> findByOperatorOrderByAdjustTimeDesc(String operator);

    List<EquipmentAdjustRecord> findAllByOrderByAdjustTimeDesc();

    /**
     * 设备改号时调整流水同步改号：该设备的全部调整记录一并换成新号，
     * 改完用旧号在流水里再也查不到这台设备。
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE EquipmentAdjustRecord r SET r.equipmentCode = :newCode WHERE r.equipmentId = :equipmentId")
    int renameEquipmentCode(@Param("equipmentId") Long equipmentId, @Param("newCode") String newCode);
}
