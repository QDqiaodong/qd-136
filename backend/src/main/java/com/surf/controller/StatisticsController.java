package com.surf.controller;

import com.surf.dto.ApiResponse;
import com.surf.dto.WaveLevelStatisticsDTO;
import com.surf.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {
    
    private final StatisticsService statisticsService;
    
    @GetMapping("/wave-level")
    public ResponseEntity<ApiResponse<List<WaveLevelStatisticsDTO>>> getWaveLevelStatistics(
            @RequestParam(required = false) String waveLevelCode) {
        List<WaveLevelStatisticsDTO> statistics = statisticsService.getWaveLevelStatistics(waveLevelCode);
        return ResponseEntity.ok(ApiResponse.success(statistics));
    }
    
    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> getOverview() {
        Map<String, Integer> overview = new HashMap<>();
        overview.put("totalEquipment", statisticsService.getTotalEquipmentCount());
        overview.put("totalWaveLevel", statisticsService.getTotalWaveLevelCount());
        overview.put("totalBinding", statisticsService.getTotalBindingCount());
        return ResponseEntity.ok(ApiResponse.success(overview));
    }
}
