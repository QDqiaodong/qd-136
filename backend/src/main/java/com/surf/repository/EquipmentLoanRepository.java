package com.surf.repository;

import com.surf.entity.EquipmentLoan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EquipmentLoanRepository extends JpaRepository<EquipmentLoan, Long> {

    /** 某台设备当前仍在借（未归还）的那条领用记录；已还清则为空。 */
    Optional<EquipmentLoan> findFirstByEquipmentIdAndReturnedAtIsNull(Long equipmentId);

    /** 全部仍在借的记录（设备名单回填在借状态、领用台账各取一次）。 */
    List<EquipmentLoan> findByReturnedAtIsNull();

    /** 某台设备的领用/归还流水，按领用时间倒序。 */
    List<EquipmentLoan> findByEquipmentIdOrderByBorrowedAtDesc(Long equipmentId);
}
