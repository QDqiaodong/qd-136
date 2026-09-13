package com.surf.repository;

import com.surf.entity.EquipmentAdjustRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EquipmentAdjustRecordRepository extends JpaRepository<EquipmentAdjustRecord, Long> {
    
    List<EquipmentAdjustRecord> findByEquipmentIdOrderByAdjustTimeDesc(Long equipmentId);
    
    List<EquipmentAdjustRecord> findByOperatorOrderByAdjustTimeDesc(String operator);
    
    List<EquipmentAdjustRecord> findAllByOrderByAdjustTimeDesc();
}
