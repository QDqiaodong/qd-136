package com.surf.service;

import com.surf.dto.EquipmentCreateDTO;
import com.surf.dto.EquipmentUpdateDTO;
import com.surf.entity.Equipment;
import com.surf.entity.EquipmentLoan;
import com.surf.entity.EquipmentWaveLevel;
import com.surf.repository.EquipmentLoanRepository;
import com.surf.repository.EquipmentRepository;
import com.surf.repository.EquipmentWaveLevelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
        
        Equipment updated = equipmentRepository.save(equipment);
        log.info("Updated equipment: {}", updated.getEquipmentCode());
        return updated;
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
