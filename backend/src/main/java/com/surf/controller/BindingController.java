package com.surf.controller;

import com.surf.dto.ApiResponse;
import com.surf.dto.EquipmentAdjustDTO;
import com.surf.dto.WaveLevelBindingDTO;
import com.surf.entity.EquipmentAdjustRecord;
import com.surf.entity.EquipmentWaveLevel;
import com.surf.service.EquipmentWaveLevelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/binding")
@RequiredArgsConstructor
public class BindingController {
    
    private final EquipmentWaveLevelService equipmentWaveLevelService;
    
    @PostMapping("/bind")
    public ResponseEntity<ApiResponse<EquipmentWaveLevel>> bind(@Valid @RequestBody WaveLevelBindingDTO dto) {
        EquipmentWaveLevel binding = equipmentWaveLevelService.bindWaveLevel(dto);
        return ResponseEntity.ok(ApiResponse.success("绑定成功", binding));
    }
    
    @PostMapping("/adjust")
    public ResponseEntity<ApiResponse<EquipmentWaveLevel>> adjust(@Valid @RequestBody EquipmentAdjustDTO dto) {
        EquipmentWaveLevel binding = equipmentWaveLevelService.adjustWaveLevel(dto);
        return ResponseEntity.ok(ApiResponse.success("调整成功", binding));
    }
    
    @GetMapping("/equipment/{equipmentId}")
    public ResponseEntity<ApiResponse<EquipmentWaveLevel>> getCurrentBinding(@PathVariable Long equipmentId) {
        return equipmentWaveLevelService.getCurrentBinding(equipmentId)
                .map(binding -> ResponseEntity.ok(ApiResponse.success(binding)))
                .orElse(ResponseEntity.ok(ApiResponse.error(404, "未找到绑定信息")));
    }
    
    @GetMapping("/wave-level/{waveLevelCode}")
    public ResponseEntity<ApiResponse<List<EquipmentWaveLevel>>> getByWaveLevel(@PathVariable String waveLevelCode) {
        List<EquipmentWaveLevel> bindings = equipmentWaveLevelService.getBindingsByWaveLevel(waveLevelCode);
        return ResponseEntity.ok(ApiResponse.success(bindings));
    }
    
    @GetMapping("/history/{equipmentId}")
    public ResponseEntity<ApiResponse<List<EquipmentWaveLevel>>> getBindingHistory(@PathVariable Long equipmentId) {
        List<EquipmentWaveLevel> history = equipmentWaveLevelService.getBindingHistory(equipmentId);
        return ResponseEntity.ok(ApiResponse.success(history));
    }
    
    @GetMapping("/adjust-records")
    public ResponseEntity<ApiResponse<List<EquipmentAdjustRecord>>> getAdjustRecords(
            @RequestParam(required = false) Long equipmentId) {
        List<EquipmentAdjustRecord> records = equipmentWaveLevelService.getAdjustRecords(equipmentId);
        return ResponseEntity.ok(ApiResponse.success(records));
    }
}
