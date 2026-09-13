package com.surf.service;

import com.surf.dto.EquipmentAdjustDTO;
import com.surf.dto.WaveLevelBindingDTO;
import com.surf.entity.Equipment;
import com.surf.entity.EquipmentAdjustRecord;
import com.surf.entity.EquipmentWaveLevel;
import com.surf.entity.WaveLevel;
import com.surf.repository.EquipmentAdjustRecordRepository;
import com.surf.repository.EquipmentRepository;
import com.surf.repository.EquipmentWaveLevelRepository;
import com.surf.repository.WaveLevelRepository;
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
public class EquipmentWaveLevelService {
    
    private final EquipmentWaveLevelRepository equipmentWaveLevelRepository;
    private final EquipmentRepository equipmentRepository;
    private final WaveLevelRepository waveLevelRepository;
    private final EquipmentAdjustRecordRepository equipmentAdjustRecordRepository;
    
    @Transactional
    public EquipmentWaveLevel bindWaveLevel(WaveLevelBindingDTO dto) {
        Equipment equipment = equipmentRepository.findById(dto.getEquipmentId())
                .orElseThrow(() -> new IllegalArgumentException("设备不存在"));
        
        WaveLevel waveLevel = waveLevelRepository.findByLevelCode(dto.getWaveLevelCode())
                .orElseThrow(() -> new IllegalArgumentException("浪高档位不存在"));
        
        EquipmentWaveLevel binding = EquipmentWaveLevel.builder()
                .equipmentId(equipment.getId())
                .waveLevelId(waveLevel.getId())
                .waveLevelCode(waveLevel.getLevelCode())
                .waveLevelName(waveLevel.getLevelName())
                .bindingType(dto.getBindingType())
                .effectiveDate(LocalDateTime.now())
                .build();
        
        EquipmentWaveLevel saved = equipmentWaveLevelRepository.save(binding);
        log.info("Bound equipment {} to wave level {}", equipment.getEquipmentCode(), waveLevel.getLevelCode());
        return saved;
    }
    
    @Transactional
    public EquipmentWaveLevel adjustWaveLevel(EquipmentAdjustDTO dto) {
        Equipment equipment = equipmentRepository.findById(dto.getEquipmentId())
                .orElseThrow(() -> new IllegalArgumentException("设备不存在"));
        
        WaveLevel newWaveLevel = waveLevelRepository.findByLevelCode(dto.getNewWaveLevelCode())
                .orElseThrow(() -> new IllegalArgumentException("新浪高档位不存在"));
        
        Optional<EquipmentWaveLevel> currentBinding = equipmentWaveLevelRepository
                .findByEquipmentIdAndExpireDateIsNull(dto.getEquipmentId());
        
        String previousLevelCode = null;
        String previousLevelName = null;
        
        if (currentBinding.isPresent()) {
            EquipmentWaveLevel existing = currentBinding.get();
            previousLevelCode = existing.getWaveLevelCode();
            previousLevelName = existing.getWaveLevelName();
            
            if (previousLevelCode.equals(dto.getNewWaveLevelCode())) {
                throw new IllegalArgumentException("新档位与当前档位相同");
            }
            
            existing.setExpireDate(LocalDateTime.now());
            equipmentWaveLevelRepository.save(existing);
        }
        
        EquipmentWaveLevel newBinding = EquipmentWaveLevel.builder()
                .equipmentId(equipment.getId())
                .waveLevelId(newWaveLevel.getId())
                .waveLevelCode(newWaveLevel.getLevelCode())
                .waveLevelName(newWaveLevel.getLevelName())
                .bindingType("ADJUST")
                .effectiveDate(LocalDateTime.now())
                .build();
        
        EquipmentWaveLevel saved = equipmentWaveLevelRepository.save(newBinding);
        
        EquipmentAdjustRecord record = EquipmentAdjustRecord.builder()
                .equipmentId(equipment.getId())
                .equipmentCode(equipment.getEquipmentCode())
                .equipmentName(equipment.getEquipmentName())
                .previousWaveLevelCode(previousLevelCode)
                .previousWaveLevelName(previousLevelName)
                .newWaveLevelCode(newWaveLevel.getLevelCode())
                .newWaveLevelName(newWaveLevel.getLevelName())
                .adjustReason(dto.getAdjustReason())
                .operator(dto.getOperator())
                .remark(dto.getRemark())
                .build();
        
        equipmentAdjustRecordRepository.save(record);
        
        log.info("Adjusted equipment {} from {} to {}", 
                equipment.getEquipmentCode(), 
                previousLevelCode, 
                newWaveLevel.getLevelCode());
        
        return saved;
    }
    
    public Optional<EquipmentWaveLevel> getCurrentBinding(Long equipmentId) {
        return equipmentWaveLevelRepository.findByEquipmentIdAndExpireDateIsNull(equipmentId);
    }
    
    public List<EquipmentWaveLevel> getBindingsByWaveLevel(String waveLevelCode) {
        return equipmentWaveLevelRepository.findActiveByWaveLevelCode(waveLevelCode);
    }
    
    public List<EquipmentWaveLevel> getBindingHistory(Long equipmentId) {
        return equipmentWaveLevelRepository.findByEquipmentIdOrderByCreatedAtDesc(equipmentId);
    }
    
    public List<EquipmentAdjustRecord> getAdjustRecords(Long equipmentId) {
        if (equipmentId != null) {
            return equipmentAdjustRecordRepository.findByEquipmentIdOrderByAdjustTimeDesc(equipmentId);
        }
        return equipmentAdjustRecordRepository.findAllByOrderByAdjustTimeDesc();
    }
    
    public List<Object[]> countEquipmentByWaveLevel() {
        return equipmentWaveLevelRepository.countByWaveLevelCode();
    }
}
