package com.surf.service;

import com.surf.dto.EquipmentInfoDTO;
import com.surf.dto.WaveLevelStatisticsDTO;
import com.surf.entity.Equipment;
import com.surf.entity.EquipmentWaveLevel;
import com.surf.entity.WaveLevel;
import com.surf.repository.EquipmentRepository;
import com.surf.repository.EquipmentWaveLevelRepository;
import com.surf.repository.WaveLevelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsService {
    
    private final EquipmentWaveLevelRepository equipmentWaveLevelRepository;
    private final EquipmentRepository equipmentRepository;
    private final WaveLevelRepository waveLevelRepository;
    
    public List<WaveLevelStatisticsDTO> getWaveLevelStatistics(String waveLevelCode) {
        List<WaveLevelStatisticsDTO> result = new ArrayList<>();
        
        List<WaveLevel> waveLevels;
        if (waveLevelCode != null && !waveLevelCode.isEmpty()) {
            waveLevels = waveLevelRepository.findByLevelCode(waveLevelCode)
                    .map(List::of)
                    .orElse(List.of());
        } else {
            waveLevels = waveLevelRepository.findAll();
        }
        
        Map<String, List<EquipmentWaveLevel>> bindingsMap = equipmentWaveLevelRepository
                .findAll().stream()
                .filter(b -> b.getExpireDate() == null)
                .collect(Collectors.groupingBy(EquipmentWaveLevel::getWaveLevelCode));

        // 只取在用（ACTIVE）设备：已停用设备的绑定不进档位名单，也不进台数
        Map<Long, Equipment> equipmentMap = equipmentRepository.findAllActive().stream()
                .collect(Collectors.toMap(Equipment::getId, e -> e));
        
        for (WaveLevel waveLevel : waveLevels) {
            List<EquipmentWaveLevel> bindings = bindingsMap.getOrDefault(waveLevel.getLevelCode(), List.of());
            
            List<EquipmentInfoDTO> equipmentList = bindings.stream()
                    .map(binding -> {
                        Equipment equipment = equipmentMap.get(binding.getEquipmentId());
                        if (equipment == null) {
                            return null;
                        }
                        return EquipmentInfoDTO.builder()
                                .id(equipment.getId())
                                .equipmentCode(equipment.getEquipmentCode())
                                .equipmentName(equipment.getEquipmentName())
                                .equipmentType(equipment.getEquipmentType())
                                .bufferThickness(equipment.getBufferThickness())
                                .specification(equipment.getSpecification())
                                .location(equipment.getLocation())
                                .status(equipment.getStatus())
                                .waveLevelCode(binding.getWaveLevelCode())
                                .waveLevelName(binding.getWaveLevelName())
                                .bindingTime(binding.getCreatedAt())
                                .build();
                    })
                    .filter(e -> e != null)
                    .collect(Collectors.toList());
            
            WaveLevelStatisticsDTO statistics = WaveLevelStatisticsDTO.builder()
                    .waveLevelCode(waveLevel.getLevelCode())
                    .waveLevelName(waveLevel.getLevelName())
                    .description(waveLevel.getDescription())
                    .equipmentCount(equipmentList.size())
                    .equipmentList(equipmentList)
                    .build();
            
            result.add(statistics);
        }
        
        result.sort((a, b) -> {
            WaveLevel aLevel = waveLevelRepository.findByLevelCode(a.getWaveLevelCode()).orElse(null);
            WaveLevel bLevel = waveLevelRepository.findByLevelCode(b.getWaveLevelCode()).orElse(null);
            if (aLevel != null && bLevel != null) {
                return aLevel.getSortOrder().compareTo(bLevel.getSortOrder());
            }
            return 0;
        });
        
        log.info("Generated statistics for {} wave levels", result.size());
        return result;
    }
    
    public Integer getTotalEquipmentCount() {
        // 与设备管理列表口径一致：只算在册（ACTIVE）设备，已停用不计入
        return equipmentRepository.findAllActive().size();
    }

    public Integer getTotalWaveLevelCount() {
        return (int) waveLevelRepository.count();
    }

    public Integer getTotalBindingCount() {
        // 与绑定状态名单口径一致：只算设备仍在用的生效绑定
        Set<Long> activeEquipmentIds = equipmentRepository.findAllActive().stream()
                .map(Equipment::getId)
                .collect(Collectors.toSet());
        return (int) equipmentWaveLevelRepository.findAll().stream()
                .filter(b -> b.getExpireDate() == null)
                .filter(b -> activeEquipmentIds.contains(b.getEquipmentId()))
                .count();
    }
}
