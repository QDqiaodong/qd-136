package com.surf.config;

import com.surf.entity.Equipment;
import com.surf.entity.EquipmentWaveLevel;
import com.surf.entity.WaveLevel;
import com.surf.repository.EquipmentRepository;
import com.surf.repository.EquipmentWaveLevelRepository;
import com.surf.repository.WaveLevelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;

@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final WaveLevelRepository waveLevelRepository;
    private final EquipmentRepository equipmentRepository;
    private final EquipmentWaveLevelRepository equipmentWaveLevelRepository;

    @Override
    @Transactional
    public void run(String... args) {
        initWaveLevels();
        initEquipmentsAndBindings();
    }

    private void initWaveLevels() {
        if (waveLevelRepository.count() > 0) {
            return;
        }
        WaveLevel low = WaveLevel.builder()
                .levelCode("LOW")
                .levelName("低浪")
                .description("适合初学者的低浪档位")
                .sortOrder(1)
                .build();

        WaveLevel medium = WaveLevel.builder()
                .levelCode("MEDIUM")
                .levelName("中浪")
                .description("适合进阶者的中浪档位")
                .sortOrder(2)
                .build();

        WaveLevel high = WaveLevel.builder()
                .levelCode("HIGH")
                .levelName("高浪")
                .description("适合高级冲浪者的高浪档位")
                .sortOrder(3)
                .build();

        waveLevelRepository.saveAll(Arrays.asList(low, medium, high));
        log.info("Initialized wave levels: LOW, MEDIUM, HIGH");
    }

    /**
     * 为每个档位各准备一台防滑扶手、一块缓冲挡垫。
     * 其中 HIGH（高浪）档位不在教练默认授权范围内，用于验证越权拦截。
     */
    private void initEquipmentsAndBindings() {
        if (equipmentRepository.count() > 0) {
            return;
        }

        seed("FS-LOW-01", "低浪区防滑扶手", "防滑扶手", null, "L1", "低浪区左侧",
                level("LOW"), "INITIAL");
        seed("HC-LOW-01", "低浪区缓冲挡垫", "缓冲挡垫", new BigDecimal("5.0"), "M", "低浪区末端",
                level("LOW"), "INITIAL");

        seed("FS-MED-01", "中浪区防滑扶手", "防滑扶手", null, "L2", "中浪区左侧",
                level("MEDIUM"), "INITIAL");
        seed("HC-MED-01", "中浪区缓冲挡垫", "缓冲挡垫", new BigDecimal("8.0"), "L", "中浪区末端",
                level("MEDIUM"), "INITIAL");

        seed("FS-HIGH-01", "高浪区防滑扶手", "防滑扶手", null, "L3", "高浪区左侧",
                level("HIGH"), "INITIAL");
        seed("HC-HIGH-01", "高浪区缓冲挡垫", "缓冲挡垫", new BigDecimal("12.0"), "XL", "高浪区末端",
                level("HIGH"), "INITIAL");

        log.info("Initialized auxiliary equipments and bindings for LOW / MEDIUM / HIGH");
    }

    private WaveLevel level(String code) {
        return waveLevelRepository.findByLevelCode(code)
                .orElseThrow(() -> new IllegalStateException("浪高档位未初始化: " + code));
    }

    private void seed(String code, String name, String type, BigDecimal thickness,
                      String specification, String location,
                      WaveLevel waveLevel, String bindingType) {
        Equipment equipment = Equipment.builder()
                .equipmentCode(code)
                .equipmentName(name)
                .equipmentType(type)
                .bufferThickness(thickness)
                .specification(specification)
                .location(location)
                .status("ACTIVE")
                .build();
        equipment = equipmentRepository.save(equipment);

        EquipmentWaveLevel binding = EquipmentWaveLevel.builder()
                .equipmentId(equipment.getId())
                .waveLevelId(waveLevel.getId())
                .waveLevelCode(waveLevel.getLevelCode())
                .waveLevelName(waveLevel.getLevelName())
                .bindingType(bindingType)
                .effectiveDate(LocalDateTime.now())
                .build();
        equipmentWaveLevelRepository.save(binding);
    }
}
