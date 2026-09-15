package com.surf.service;

import com.surf.dto.EquipmentCreateDTO;
import com.surf.dto.EquipmentUpdateDTO;
import com.surf.entity.Equipment;
import com.surf.entity.EquipmentWaveLevel;
import com.surf.repository.EquipmentRepository;
import com.surf.repository.EquipmentWaveLevelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EquipmentService {

    private final EquipmentRepository equipmentRepository;
    private final EquipmentWaveLevelRepository equipmentWaveLevelRepository;
    
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
        log.info("Deleted equipment: {}", equipment.getEquipmentCode());
    }
    
    public Optional<Equipment> getEquipmentById(Long id) {
        return equipmentRepository.findById(id);
    }
    
    public Optional<Equipment> getEquipmentByCode(String code) {
        return equipmentRepository.findByEquipmentCode(code);
    }
    
    public List<Equipment> getAllEquipments() {
        return equipmentRepository.findAllActive();
    }
    
    public List<Equipment> getEquipmentsByType(String type) {
        return equipmentRepository.findByEquipmentType(type);
    }
}
