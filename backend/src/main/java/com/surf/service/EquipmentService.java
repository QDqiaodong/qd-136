package com.surf.service;

import com.surf.dto.EquipmentCreateDTO;
import com.surf.dto.EquipmentUpdateDTO;
import com.surf.entity.Equipment;
import com.surf.entity.EquipmentLoan;
import com.surf.entity.EquipmentWaveLevel;
import com.surf.repository.EquipmentAdjustRecordRepository;
import com.surf.repository.EquipmentLoanRepository;
import com.surf.repository.EquipmentRepository;
import com.surf.repository.EquipmentWaveLevelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EquipmentService {

    private final EquipmentRepository equipmentRepository;
    private final EquipmentWaveLevelRepository equipmentWaveLevelRepository;
    private final EquipmentLoanRepository equipmentLoanRepository;
    private final EquipmentAdjustRecordRepository equipmentAdjustRecordRepository;
    
    @Transactional
    public Equipment createEquipment(EquipmentCreateDTO dto) {
        if (equipmentRepository.existsByEquipmentCode(dto.getEquipmentCode())) {
            throw new IllegalArgumentException("设备编号已存在");
        }
        
        Equipment equipment = Equipment.builder()
                .equipmentCode(dto.getEquipmentCode())
                .equipmentName(dto.getEquipmentName())
                .equipmentType(dto.getEquipmentType())
                .bufferThickness(dto.getBufferThickness())
                .specification(dto.getSpecification())
                .location(dto.getLocation())
                .remark(dto.getRemark())
                .build();
        
        Equipment saved = equipmentRepository.save(equipment);
        log.info("Created equipment: {}", saved.getEquipmentCode());
        return saved;
    }
    
    @Transactional
    public Equipment updateEquipment(Long id, EquipmentUpdateDTO dto) {
        Equipment equipment = equipmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("设备不存在"));

        if (dto.getEquipmentName() != null) {
            equipment.setEquipmentName(dto.getEquipmentName());
        }
        if (dto.getEquipmentType() != null) {
            equipment.setEquipmentType(dto.getEquipmentType());
        }
        if (dto.getBufferThickness() != null) {
            equipment.setBufferThickness(dto.getBufferThickness());
        }
        if (dto.getSpecification() != null) {
            equipment.setSpecification(dto.getSpecification());
        }
        if (dto.getLocation() != null) {
            equipment.setLocation(dto.getLocation());
        }
        if (dto.getRemark() != null) {
            equipment.setRemark(dto.getRemark());
        }

        // 改设备编号：领用台账、设备调整流水必须同事务跟着改成新号
        String newCode = dto.getEquipmentCode() == null ? null : dto.getEquipmentCode().trim();
        if (newCode != null) {
            if (newCode.isEmpty()) {
                throw new IllegalArgumentException("设备编号不能为空");
            }
            if (!newCode.equals(equipment.getEquipmentCode())) {
                renameEquipmentCode(equipment, newCode);
            }
        }

        Equipment updated = equipmentRepository.save(equipment);
        log.info("Updated equipment: {}", updated.getEquipmentCode());
        return updated;
    }

    /**
     * 改设备编号。新号被另一台在册设备占用时拦截并写明占用者，本台编号维持原样；
     * 改号成功后，领用台账（含未还清的在借行）与设备调整流水同事务换成新号，
     * 三处用同一个号，馆长对账才对得上；用旧号去两本账里再查不到这台设备。
     */
    private void renameEquipmentCode(Equipment equipment, String newCode) {
        String oldCode = equipment.getEquipmentCode();

        // 预检：新号已被另一台设备占用时直接拦截，报错写明被哪一台占着
        equipmentRepository.findByEquipmentCode(newCode)
                .filter(occupant -> !occupant.getId().equals(equipment.getId()))
                .ifPresent(occupant -> {
                    throw new IllegalArgumentException(occupiedMessage(newCode, oldCode, occupant));
                });

        equipment.setEquipmentCode(newCode);
        try {
            // 立即落库抢占新号：两人同时改同一个新号时，equipment_code 唯一约束只放行一台，
            // 后提交的在 flush 时撞约束回滚，绝不允许两本档案共用一个号
            equipmentRepository.saveAndFlush(equipment);
        } catch (DataIntegrityViolationException e) {
            throw new EquipmentCodeOccupiedException(newCode, oldCode);
        }

        // 两本账同步改号：未还清的在借行也一并改，不因还在借就留下旧号
        int loanRows = equipmentLoanRepository.renameEquipmentCode(equipment.getId(), newCode);
        int adjustRows = equipmentAdjustRecordRepository.renameEquipmentCode(equipment.getId(), newCode);
        log.info("Renamed equipment code {} -> {}: {} loan rows, {} adjust records followed",
                oldCode, newCode, loanRows, adjustRows);
    }

    /** 占用提示：写明新号被哪一台设备占着，并告知本台编号维持原样。 */
    public static String occupiedMessage(String newCode, String oldCode, Equipment occupant) {
        return "设备编号 " + newCode + " 已被 " + occupant.getEquipmentName()
                + "（编号 " + occupant.getEquipmentCode() + "）占用，本台编号维持 " + oldCode;
    }
    
    @Transactional
    public void deleteEquipment(Long id) {
        Equipment equipment = equipmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("设备不存在"));
        equipment.setStatus("DELETED");
        equipmentRepository.save(equipment);

        // 停用当下摘掉档位：该设备仍生效的绑定同事务打上失效时间，
        // 统计看板的档位台数/名单与绑定状态随即不再包含这台设备
        List<EquipmentWaveLevel> activeBindings = equipmentWaveLevelRepository
                .findAllByEquipmentIdAndExpireDateIsNull(id);
        for (EquipmentWaveLevel binding : activeBindings) {
            binding.setExpireDate(LocalDateTime.now());
            equipmentWaveLevelRepository.save(binding);
            log.info("Expired binding of disabled equipment {} on wave level {}",
                    equipment.getEquipmentCode(), binding.getWaveLevelCode());
        }

        // 停用设备不再占用在借台账：仍在借的领用记录同事务结清，
        // 避免名单已无此设备、领用台账却还挂着在借
        equipmentLoanRepository.findFirstByEquipmentIdAndReturnedAtIsNull(id)
                .ifPresent(loan -> {
                    loan.setReturnedAt(LocalDateTime.now());
                    equipmentLoanRepository.save(loan);
                    log.info("Closed active loan of disabled equipment {} (was borrowed by {})",
                            equipment.getEquipmentCode(), loan.getBorrowedBy());
                });
        log.info("Deleted equipment: {}", equipment.getEquipmentCode());
    }
    
    public Optional<Equipment> getEquipmentById(Long id) {
        Optional<Equipment> equipment = equipmentRepository.findById(id);
        equipment.ifPresent(this::fillLoanStatus);
        return equipment;
    }

    public Optional<Equipment> getEquipmentByCode(String code) {
        return equipmentRepository.findByEquipmentCode(code);
    }

    public List<Equipment> getAllEquipments() {
        return fillLoanStatus(equipmentRepository.findAllActive());
    }

    public List<Equipment> getEquipmentsByType(String type) {
        return fillLoanStatus(equipmentRepository.findByEquipmentType(type));
    }

    /**
     * 批量回填在借状态：一次取出全部未归还领用记录，避免逐台查询。
     * 名单上的在借/空闲完全以领用台账为准，两本账因此对得上。
     */
    private List<Equipment> fillLoanStatus(List<Equipment> equipments) {
        Map<Long, EquipmentLoan> activeLoans = equipmentLoanRepository.findByReturnedAtIsNull().stream()
                .collect(Collectors.toMap(EquipmentLoan::getEquipmentId, Function.identity(), (a, b) -> a));
        for (Equipment equipment : equipments) {
            applyLoan(equipment, activeLoans.get(equipment.getId()));
        }
        return equipments;
    }

    /** 单台回填在借状态。 */
    private void fillLoanStatus(Equipment equipment) {
        EquipmentLoan active = equipmentLoanRepository
                .findFirstByEquipmentIdAndReturnedAtIsNull(equipment.getId())
                .orElse(null);
        applyLoan(equipment, active);
    }

    private void applyLoan(Equipment equipment, EquipmentLoan active) {
        if (active != null) {
            equipment.setBorrowed(true);
            equipment.setBorrowedBy(active.getBorrowedBy());
            equipment.setBorrowedAt(active.getBorrowedAt());
            equipment.setActiveLoanId(active.getId());
        } else {
            // 已还清的设备必须清空旧标记：关页再开仍是“空闲”，不再被标在借
            equipment.setBorrowed(false);
            equipment.setBorrowedBy(null);
            equipment.setBorrowedAt(null);
            equipment.setActiveLoanId(null);
        }
    }
}
