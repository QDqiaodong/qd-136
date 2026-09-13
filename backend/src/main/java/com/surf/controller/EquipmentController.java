package com.surf.controller;

import com.surf.dto.ApiResponse;
import com.surf.dto.EquipmentCreateDTO;
import com.surf.dto.EquipmentUpdateDTO;
import com.surf.entity.Equipment;
import com.surf.service.EquipmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/equipment")
@RequiredArgsConstructor
public class EquipmentController {
    
    private final EquipmentService equipmentService;
    
    @PostMapping
    public ResponseEntity<ApiResponse<Equipment>> create(@Valid @RequestBody EquipmentCreateDTO dto) {
        Equipment equipment = equipmentService.createEquipment(dto);
        return ResponseEntity.ok(ApiResponse.success("设备创建成功", equipment));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Equipment>> update(
            @PathVariable Long id, 
            @Valid @RequestBody EquipmentUpdateDTO dto) {
        Equipment equipment = equipmentService.updateEquipment(id, dto);
        return ResponseEntity.ok(ApiResponse.success("设备更新成功", equipment));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        equipmentService.deleteEquipment(id);
        return ResponseEntity.ok(ApiResponse.success("设备删除成功", null));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Equipment>> getById(@PathVariable Long id) {
        return equipmentService.getEquipmentById(id)
                .map(equipment -> ResponseEntity.ok(ApiResponse.success(equipment)))
                .orElse(ResponseEntity.ok(ApiResponse.error(404, "设备不存在")));
    }
    
    @GetMapping
    public ResponseEntity<ApiResponse<List<Equipment>>> getAll() {
        List<Equipment> equipments = equipmentService.getAllEquipments();
        return ResponseEntity.ok(ApiResponse.success(equipments));
    }
    
    @GetMapping("/type/{type}")
    public ResponseEntity<ApiResponse<List<Equipment>>> getByType(@PathVariable String type) {
        List<Equipment> equipments = equipmentService.getEquipmentsByType(type);
        return ResponseEntity.ok(ApiResponse.success(equipments));
    }
}
