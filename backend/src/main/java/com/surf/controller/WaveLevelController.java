package com.surf.controller;

import com.surf.dto.ApiResponse;
import com.surf.dto.WaveLevelSaveDTO;
import com.surf.entity.WaveLevel;
import com.surf.service.WaveLevelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wave-level")
@RequiredArgsConstructor
public class WaveLevelController {
    
    private final WaveLevelService waveLevelService;
    
    @PostMapping
    public ResponseEntity<ApiResponse<WaveLevel>> create(@RequestBody WaveLevelSaveDTO dto) {
        WaveLevel created = waveLevelService.createWaveLevel(dto);
        return ResponseEntity.ok(ApiResponse.success("浪高档位创建成功", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<WaveLevel>> update(
            @PathVariable Long id,
            @RequestBody WaveLevelSaveDTO dto) {
        WaveLevel updated = waveLevelService.updateWaveLevel(id, dto);
        return ResponseEntity.ok(ApiResponse.success("浪高档位更新成功", updated));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        waveLevelService.deleteWaveLevel(id);
        return ResponseEntity.ok(ApiResponse.success("浪高档位删除成功", null));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<WaveLevel>> getById(@PathVariable Long id) {
        return waveLevelService.getWaveLevelById(id)
                .map(waveLevel -> ResponseEntity.ok(ApiResponse.success(waveLevel)))
                .orElse(ResponseEntity.ok(ApiResponse.error(404, "浪高档位不存在")));
    }
    
    @GetMapping
    public ResponseEntity<ApiResponse<List<WaveLevel>>> getAll() {
        List<WaveLevel> waveLevels = waveLevelService.getAllWaveLevels();
        return ResponseEntity.ok(ApiResponse.success(waveLevels));
    }
}
