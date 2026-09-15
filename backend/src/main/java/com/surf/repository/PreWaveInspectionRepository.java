package com.surf.repository;

import com.surf.entity.PreWaveInspection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PreWaveInspectionRepository extends JpaRepository<PreWaveInspection, Long> {

    /** 某晚全部点检记录，按更新时间倒序，便于最近点检的设备排在前面。 */
    List<PreWaveInspection> findByInspectionDateOrderByUpdatedAtDesc(LocalDate inspectionDate);

    /** 同一台设备同一点检日期的唯一记录（存在则说明这台设备今晚已点过）。 */
    Optional<PreWaveInspection> findByInspectionDateAndEquipmentId(LocalDate inspectionDate, Long equipmentId);

    long countByInspectionDate(LocalDate inspectionDate);
}
