package com.surf.config;

import com.surf.entity.WaveLevel;
import com.surf.repository.WaveLevelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
    
    private final WaveLevelRepository waveLevelRepository;
    
    @Override
    public void run(String... args) {
        if (waveLevelRepository.count() == 0) {
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
    }
}
