package com.surf.service;

import com.surf.dto.WaveLevelSaveDTO;
import com.surf.entity.WaveLevel;
import com.surf.repository.WaveLevelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WaveLevelService {

    private final WaveLevelRepository waveLevelRepository;

    @Transactional
    public WaveLevel createWaveLevel(WaveLevelSaveDTO dto) {
        assertRequired(dto, true);

        String levelCode = dto.getLevelCode().trim();
        // 代号查重：写明代号被哪个档位占用，馆长好知道该换哪个代号
        waveLevelRepository.findByLevelCodeIgnoreCase(levelCode).ifPresent(occupant -> {
            throw new IllegalArgumentException(
                    "档位代号 " + levelCode + " 已被档位「" + occupant.getLevelName() + "」占用，请更换代号");
        });

        WaveLevel waveLevel = WaveLevel.builder()
                .levelCode(levelCode)
                .levelName(dto.getLevelName().trim())
                .sortOrder(dto.getSortOrder())
                .description(dto.getDescription().trim())
                .build();
        WaveLevel saved = waveLevelRepository.save(waveLevel);
        log.info("Created wave level: {}", saved.getLevelCode());
        return saved;
    }

    @Transactional
    public WaveLevel updateWaveLevel(Long id, WaveLevelSaveDTO dto) {
        WaveLevel existing = waveLevelRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("浪高档位不存在"));

        // 编辑时代号不可改，只校验其余必填项
        assertRequired(dto, false);

        existing.setLevelName(dto.getLevelName().trim());
        existing.setSortOrder(dto.getSortOrder());
        existing.setDescription(dto.getDescription().trim());

        WaveLevel updated = waveLevelRepository.save(existing);
        log.info("Updated wave level: {}", updated.getLevelCode());
        return updated;
    }

    /**
     * 必填校验：缺哪项点哪项，一次性列全。
     * 档位代号仅新增时必填；编辑时代号不可改，不参与校验。
     */
    private void assertRequired(WaveLevelSaveDTO dto, boolean requireCode) {
        List<String> missing = new ArrayList<>();
        if (requireCode && !StringUtils.hasText(dto.getLevelCode())) {
            missing.add("档位代号");
        }
        if (!StringUtils.hasText(dto.getLevelName())) {
            missing.add("名称");
        }
        if (dto.getSortOrder() == null) {
            missing.add("显示顺序");
        }
        if (!StringUtils.hasText(dto.getDescription())) {
            missing.add("说明");
        }
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("缺少必填项：" + String.join("、", missing));
        }
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

    /** 全部档位，按显示顺序升序。 */
    public List<WaveLevel> getAllWaveLevels() {
        return waveLevelRepository.findAllByOrderBySortOrderAscIdAsc();
    }
}
