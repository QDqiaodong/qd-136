package com.surf.repository;

import com.surf.entity.EquipmentWaveLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EquipmentWaveLevelRepository extends JpaRepository<EquipmentWaveLevel, Long> {
    
    Optional<EquipmentWaveLevel> findByEquipmentIdAndExpireDateIsNull(Long equipmentId);

    List<EquipmentWaveLevel> findAllByEquipmentIdAndExpireDateIsNull(Long equipmentId);
    
    List<EquipmentWaveLevel> findByWaveLevelCodeAndExpireDateIsNull(String waveLevelCode);
    
    @Query("SELECT ewl FROM EquipmentWaveLevel ewl WHERE ewl.waveLevelCode = :waveLevelCode AND ewl.expireDate IS NULL")
    List<EquipmentWaveLevel> findActiveByWaveLevelCode(@Param("waveLevelCode") String waveLevelCode);
    
    @Query("SELECT ewl FROM EquipmentWaveLevel ewl WHERE ewl.equipmentId = :equipmentId ORDER BY ewl.createdAt DESC")
    List<EquipmentWaveLevel> findByEquipmentIdOrderByCreatedAtDesc(@Param("equipmentId") Long equipmentId);
    
    @Query("SELECT ewl.waveLevelCode, COUNT(ewl) FROM EquipmentWaveLevel ewl WHERE ewl.expireDate IS NULL GROUP BY ewl.waveLevelCode")
    List<Object[]> countByWaveLevelCode();
    
    void deleteByEquipmentIdAndExpireDateIsNull(Long equipmentId);
}
