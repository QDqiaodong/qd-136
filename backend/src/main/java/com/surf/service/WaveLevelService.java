package com.surf.service;

import com.surf.entity.WaveLevel;
import com.surf.repository.WaveLevelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WaveLevelService {
    
    private final WaveLevelRepository waveLevelRepository;
    
    @Transactional
    public WaveLevel createWaveLevel(WaveLevel waveLevel) {
        if (waveLevelRepository.existsByLevelCode(waveLevel.getLevelCode())) {
            throw new IllegalArgumentException("浪高档位编码已存在");
        }
        WaveLevel saved = waveLevelRepository.save(waveLevel);
        log.info("Created wave level: {}", saved.getLevelCode());
        return saved;
    }
    
    @Transactional
    public WaveLevel updateWaveLevel(Long id, WaveLevel waveLevel) {
        WaveLevel existing = waveLevelRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("浪高档位不存在"));
        
        if (waveLevel.getLevelName() != null) {
            existing.setLevelName(waveLevel.getLevelName());
        }
        if (waveLevel.getDescription() != null) {
            existing.setDescription(waveLevel.getDescription());
        }
        if (waveLevel.getSortOrder() != null) {
            existing.setSortOrder(waveLevel.getSortOrder());
        }
        
        WaveLevel updated = waveLevelRepository.save(existing);
        log.info("Updated wave level: {}", updated.getLevelCode());
        return updated;
    }
    
    @Transactional
    public void deleteWaveLevel(Long id) {
        waveLevelRepository.deleteById(id);
        log.info("Deleted wave level: {}", id);
    }
    
    public Optional<WaveLevel> getWaveLevelById(Long id) {
        return waveLevelRepository.findById(id);
    }
    
    public Optional<WaveLevel> getWaveLevelByCode(String code) {
        return waveLevelRepository.findByLevelCode(code);
    }
    
    public List<WaveLevel> getAllWaveLevels() {
        return waveLevelRepository.findAll();
    }
}
