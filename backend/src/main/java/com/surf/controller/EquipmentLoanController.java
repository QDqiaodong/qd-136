package com.surf.controller;

import com.surf.dto.ApiResponse;
import com.surf.dto.EquipmentLoanCreateDTO;
import com.surf.entity.EquipmentLoan;
import com.surf.service.EquipmentLoanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 设备领用 / 归还接口。在借状态以领用台账为唯一依据：
 * 未还清时领用接口直接 400 拦截并写明已借给哪位教练，归还后才可再次领用。
 */
@RestController
@RequestMapping("/api/equipment-loan")
@RequiredArgsConstructor
public class EquipmentLoanController {

    private final EquipmentLoanService equipmentLoanService;

    /** 登记领用：设备存在未归还记录时拒绝。 */
    @PostMapping("/borrow")
    public ResponseEntity<ApiResponse<EquipmentLoan>> borrow(@Valid @RequestBody EquipmentLoanCreateDTO dto) {
        EquipmentLoan loan = equipmentLoanService.borrow(dto);
        return ResponseEntity.ok(ApiResponse.success("领用成功", loan));
    }

    /** 登记归还：写入归还时间，设备随后可再次领用。 */
    @PostMapping("/return/{equipmentId}")
    public ResponseEntity<ApiResponse<EquipmentLoan>> returnEquipment(
            @PathVariable Long equipmentId,
            @RequestBody(required = false) Map<String, String> body) {
        String operatorName = body == null ? null : body.get("operatorName");
        EquipmentLoan loan = equipmentLoanService.returnEquipment(equipmentId, operatorName);
        return ResponseEntity.ok(ApiResponse.success("归还成功", loan));
    }

    /** 当前全部在借记录（领用台账）。 */
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<EquipmentLoan>>> getActiveLoans() {
        return ResponseEntity.ok(ApiResponse.success(equipmentLoanService.getActiveLoans()));
    }

    /** 某台设备的领用 / 归还流水。 */
    @GetMapping("/history/{equipmentId}")
    public ResponseEntity<ApiResponse<List<EquipmentLoan>>> getHistory(@PathVariable Long equipmentId) {
        return ResponseEntity.ok(ApiResponse.success(equipmentLoanService.getHistory(equipmentId)));
    }
}
