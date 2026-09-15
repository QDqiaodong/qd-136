package com.surf.controller;

import com.surf.dto.ApiResponse;
import com.surf.dto.PreWaveInspectionDTO;
import com.surf.dto.PreWaveInspectionSubmitDTO;
import com.surf.dto.PreWaveInspectionSummaryDTO;
import com.surf.dto.PreWaveReconcileDTO;
import com.surf.dto.PreWaveReconcileResultDTO;
import com.surf.entity.Equipment;
import com.surf.service.PreWaveInspectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 开浪前点检盘点（馆长）：入浪防滑扶手 / 缓冲挡垫的打滑次数与挡垫移位，
 * 页上合计实时汇总，并支持与现场计数对账。
 */
@RestController
@RequestMapping("/api/inspection")
@RequiredArgsConstructor
public class PreWaveInspectionController {

    private final PreWaveInspectionService inspectionService;

    /** 今晚点检明细。 */
    @GetMapping("/today")
    public ResponseEntity<ApiResponse<List<PreWaveInspectionDTO>>> listToday() {
        return ResponseEntity.ok(ApiResponse.success(inspectionService.listToday()));
    }

    /** 今晚可点检的入浪辅助设备（防滑扶手、缓冲挡垫）。 */
    @GetMapping("/equipments")
    public ResponseEntity<ApiResponse<List<Equipment>>> inspectableEquipments() {
        return ResponseEntity.ok(ApiResponse.success(inspectionService.inspectableEquipments()));
    }

    /** 今晚打滑合计、移位合计（改任意一条数字后立即重算）。 */
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<PreWaveInspectionSummaryDTO>> summary() {
        return ResponseEntity.ok(ApiResponse.success(inspectionService.summaryToday()));
    }

    /** 录入 / 复点同一台设备：同设备同晚只保留一条（合计只算一次）。 */
    @PostMapping
    public ResponseEntity<ApiResponse<PreWaveInspectionService.PreWaveInspectionUpsertResult>> submit(
            @Valid @RequestBody PreWaveInspectionSubmitDTO dto) {
        PreWaveInspectionService.PreWaveInspectionUpsertResult result = inspectionService.submit(dto);
        String message = result.reInspected() ? "复点成功：已更新该设备记录，合计仍只算这一台" : "点检成功";
        return ResponseEntity.ok(ApiResponse.success(message, result));
    }

    /** 撤回一条点检记录，合计随之更新。 */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        inspectionService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("已撤回该点检记录", null));
    }

    /** 与现场计数对账。 */
    @PostMapping("/reconcile")
    public ResponseEntity<ApiResponse<PreWaveReconcileResultDTO>> reconcile(
            @Valid @RequestBody PreWaveReconcileDTO dto) {
        PreWaveReconcileResultDTO result = inspectionService.reconcile(dto);
        String message = result.isBalanced() ? "账实相符" : "账实不符，请复核现场计数";
        return ResponseEntity.ok(ApiResponse.success(message, result));
    }
}
