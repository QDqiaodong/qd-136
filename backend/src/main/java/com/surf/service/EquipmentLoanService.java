package com.surf.service;

import com.surf.dto.EquipmentLoanCreateDTO;
import com.surf.entity.Equipment;
import com.surf.entity.EquipmentLoan;
import com.surf.repository.EquipmentLoanRepository;
import com.surf.repository.EquipmentRepository;
import com.surf.security.AccessControlService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 设备领用 / 归还。
 *
 * 在借状态以领用台账（{@link EquipmentLoan}）为唯一依据：
 * returnedAt 为 null 即“仍在借”，设备名单据此标在借；
 * 某台设备存在未归还记录时，拒绝再次领用，报错写明已借给哪位教练；
 * 归还后写入 returnedAt，设备才回到可领用状态。状态随台账落库，关页再开不丢。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EquipmentLoanService {

    private final EquipmentLoanRepository equipmentLoanRepository;
    private final EquipmentRepository equipmentRepository;
    private final AccessControlService accessControlService;

    @Transactional
    public EquipmentLoan borrow(EquipmentLoanCreateDTO dto) {
        Equipment equipment = equipmentRepository.findById(dto.getEquipmentId())
                .orElseThrow(() -> new IllegalArgumentException("设备不存在"));
        if (!"ACTIVE".equals(equipment.getStatus())) {
            throw new IllegalArgumentException("设备已停用，不能领用");
        }
        // 教练只能领用防滑扶手、缓冲挡垫两类辅助设备，越权 403
        accessControlService.assertCanViewEquipment(equipment);

        String borrowerName = dto.getBorrowerName().trim();

        // 未还清不许再借：台账里存在 returnedAt 为空的记录即仍在借
        Optional<EquipmentLoan> active =
                equipmentLoanRepository.findFirstByEquipmentIdAndReturnedAtIsNull(equipment.getId());
        if (active.isPresent()) {
            EquipmentLoan onLoan = active.get();
            throw new IllegalArgumentException(
                    "该设备已借给 " + onLoan.getBorrowedBy() + "，尚未归还，不能重复领用");
        }

        EquipmentLoan loan = EquipmentLoan.builder()
                .equipmentId(equipment.getId())
                .equipmentCode(equipment.getEquipmentCode())
                .equipmentName(equipment.getEquipmentName())
                .borrowedBy(borrowerName)
                .borrowedAt(LocalDateTime.now())
                .remark(dto.getRemark())
                .build();
        EquipmentLoan saved = equipmentLoanRepository.save(loan);

        log.info("[{}] Equipment {} borrowed by {}",
                accessControlService.currentRole(), equipment.getEquipmentCode(), borrowerName);
        return saved;
    }

    @Transactional
    public EquipmentLoan returnEquipment(Long equipmentId, String operatorName) {
        Equipment equipment = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new IllegalArgumentException("设备不存在"));
        accessControlService.assertCanViewEquipment(equipment);

        EquipmentLoan loan = equipmentLoanRepository
                .findFirstByEquipmentIdAndReturnedAtIsNull(equipmentId)
                .orElseThrow(() -> new IllegalArgumentException("该设备当前不在借，无需归还"));

        loan.setReturnedAt(LocalDateTime.now());
        if (operatorName != null && !operatorName.trim().isEmpty()) {
            loan.setReturnedBy(operatorName.trim());
        }
        EquipmentLoan saved = equipmentLoanRepository.save(loan);

        log.info("[{}] Equipment {} returned (was borrowed by {})",
                accessControlService.currentRole(), equipment.getEquipmentCode(), loan.getBorrowedBy());
        return saved;
    }

    /** 某台设备当前在借记录，已还清返回空。 */
    public Optional<EquipmentLoan> getActiveLoan(Long equipmentId) {
        return equipmentLoanRepository.findFirstByEquipmentIdAndReturnedAtIsNull(equipmentId);
    }

    /** 当前全部在借记录。 */
    public List<EquipmentLoan> getActiveLoans() {
        return equipmentLoanRepository.findByReturnedAtIsNull();
    }

    /** 某台设备的领用/归还流水。 */
    public List<EquipmentLoan> getHistory(Long equipmentId) {
        Equipment equipment = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new IllegalArgumentException("设备不存在"));
        accessControlService.assertCanViewEquipment(equipment);
        return equipmentLoanRepository.findByEquipmentIdOrderByBorrowedAtDesc(equipmentId);
    }
}
