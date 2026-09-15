package com.surf.service;

import com.surf.dto.WaveLevelSaveDTO;
import com.surf.entity.WaveLevel;
import com.surf.repository.WaveLevelRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 档位管理页对应的服务层行为：
 * 列表按显示顺序排、代号重复时写明占用方、缺项逐项点名、改名后再读仍是新值。
 */
@DataJpaTest
@Import(WaveLevelService.class)
class WaveLevelServiceTest {

    @Autowired
    private WaveLevelService waveLevelService;

    @Autowired
    private WaveLevelRepository waveLevelRepository;

    @Autowired
    private TestEntityManager entityManager;

    private WaveLevel saveLevel(String code, String name, int sortOrder) {
        return waveLevelRepository.save(WaveLevel.builder()
                .levelCode(code)
                .levelName(name)
                .description(name + "说明")
                .sortOrder(sortOrder)
                .build());
    }

    private WaveLevelSaveDTO dto(String code, String name, Integer sortOrder, String description) {
        WaveLevelSaveDTO dto = new WaveLevelSaveDTO();
        dto.setLevelCode(code);
        dto.setLevelName(name);
        dto.setSortOrder(sortOrder);
        dto.setDescription(description);
        return dto;
    }

    @Test
    void listIsSortedBySortOrder() {
        saveLevel("HIGH", "高浪", 3);
        saveLevel("LOW", "低浪", 1);
        saveLevel("MEDIUM", "中浪", 2);

        List<WaveLevel> all = waveLevelService.getAllWaveLevels();

        assertThat(all).extracting(WaveLevel::getLevelCode)
                .containsExactly("LOW", "MEDIUM", "HIGH");
    }

    @Test
    void duplicateCodeIsRejectedAndNamesOccupant() {
        saveLevel("LOW", "低浪", 1);

        // 代号不区分大小写：low 与 LOW 视为同一代号
        assertThatThrownBy(() -> waveLevelService.createWaveLevel(dto("low", "超低浪", 4, "重复代号")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("low")
                .hasMessageContaining("低浪");

        // 重复的档位没有落库
        assertThat(waveLevelRepository.count()).isEqualTo(1);
    }

    @Test
    void missingFieldsAreListedByName() {
        assertThatThrownBy(() -> waveLevelService.createWaveLevel(dto(null, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("档位代号")
                .hasMessageContaining("名称")
                .hasMessageContaining("显示顺序")
                .hasMessageContaining("说明");
    }

    @Test
    void updatedNamePersistsAcrossReads() {
        WaveLevel low = saveLevel("LOW", "低浪", 1);

        waveLevelService.updateWaveLevel(low.getId(), dto(null, "入门低浪", 5, "改名后的说明"));

        // 模拟“再进这个页面”：清掉持久化上下文，强制重新从库里读
        entityManager.flush();
        entityManager.clear();

        WaveLevel reloaded = waveLevelService.getWaveLevelById(low.getId()).orElseThrow();
        assertThat(reloaded.getLevelName()).isEqualTo("入门低浪");
        assertThat(reloaded.getSortOrder()).isEqualTo(5);
        // 代号是绑定关系的稳定标识，编辑不可改
        assertThat(reloaded.getLevelCode()).isEqualTo("LOW");
    }
}
