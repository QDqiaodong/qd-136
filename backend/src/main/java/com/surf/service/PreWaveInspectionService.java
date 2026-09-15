package com.surf.service;

import com.surf.dto.PreWaveInspectionDTO;
import com.surf.dto.PreWaveInspectionSubmitDTO;
import com.surf.dto.PreWaveInspectionSummaryDTO;
import com.surf.dto.PreWaveReconcileDTO;
import com.surf.dto.PreWaveReconcileResultDTO;
import com.surf.entity.Equipment;
import com.surf.entity.PreWaveInspection;
import com.surf.repository.EquipmentRepository;
import com.surf.repository.PreWaveInspectionRepository;
import com.surf.security.AccessControlService;
import com.surf.security.AccessDeniedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 开浪前点检盘点。
 *
 * <p>业务规则：
 * <ul>
 *   <li>仅馆长可做开浪前点检；教练访问直接 403。</li>
 *   <li>点检对象限定为入浪辅助设备：防滑扶手、缓冲挡垫。</li>
 *   <li>同设备同晚只保留一条记录：复点走更新，合计只算一次。</li>
 *   <li>合计两个数：今晚打滑次数合计（防滑扶手）、今晚挡垫移位合计（缓冲挡垫）。</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PreWaveInspectionService {

    /** 点检对象设备类型，与 AccessControlService 的辅助设备类型保持一致。 */
    public static final String HANDRAIL_TYPE = "防滑扶手";
    public static final String BUFFER_PAD_TYPE = "缓冲挡垫";

    private final PreWaveInspectionRepository inspectionRepository;
    private final EquipmentRepository equipmentRepository;
    private final AccessControlService accessControlService;

    /** 开浪前点检是馆长开浪前的盘点职责，教练一律不可访问。 */
    private void assertDirector() {
        if (!accessControlService.isDirector()) {
            throw new AccessDeniedException("没有权限：开浪前点检盘点仅馆长可操作");
        }
    }

    /** 今晚点检明细。 */
    public List<PreWaveInspectionDTO> listToday() {
        assertDirector();
        return toDTOs(inspectionRepository.findByInspectionDateOrderByUpdatedAtDesc(LocalDate.now()));
    }

    /** 今晚可点检的设备：入浪辅助设备（防滑扶手、缓冲挡垫）且在用。 */
    public List<Equipment> inspectableEquipments() {
        assertDirector();
        return equipmentRepository.findAllActive().stream()
                .filter(e -> HANDRAIL_TYPE.equals(e.getEquipmentType())
                        || BUFFER_PAD_TYPE.equals(e.getEquipmentType()))
                .collect(Collectors.toList());
    }

    /**
     * 录入 / 复点。同一台设备今晚已点过时，用新数字覆盖旧记录（不新增行），
     * 因此“同一台设备再点一遍，合计里只算一次”。
     *
     * @return 更新后的今晚全部明细与合计
     */
    @Transactional
    public PreWaveInspectionUpsertResult submit(PreWaveInspectionSubmitDTO dto) {
        assertDirector();

        Equipment equipment = equipmentRepository.findById(dto.getEquipmentId())
                .orElseThrow(() -> new IllegalArgumentException("设备不存在"));
        if (equipment.getStatus() != null && !"ACTIVE".equals(equipment.getStatus())) {
            throw new IllegalArgumentException("设备已停用，不能点检");
        }

        String type = equipment.getEquipmentType();
        if (!HANDRAIL_TYPE.equals(type) && !BUFFER_PAD_TYPE.equals(type)) {
            throw new IllegalArgumentException("点检对象仅限防滑扶手与缓冲挡垫");
        }
        // 非对应类型的指标强制归零：扶手不算移位，挡垫不算打滑
        int slipCount = HANDRAIL_TYPE.equals(type) ? dto.getSlipCount() : 0;
        BigDecimal shiftCm = BUFFER_PAD_TYPE.equals(type) ? dto.getShiftCm() : BigDecimal.ZERO;

        LocalDate today = LocalDate.now();
        PreWaveInspection record = inspectionRepository
                .findByInspectionDateAndEquipmentId(today, equipment.getId())
                .orElse(null);

        boolean reInspected = record != null;
        if (record == null) {
            record = PreWaveInspection.builder()
                    .inspectionDate(today)
                    .equipmentId(equipment.getId())
                    .build();
        }

        record.setEquipmentCode(equipment.getEquipmentCode());
        record.setEquipmentName(equipment.getEquipmentName());
        record.setEquipmentType(type);
        record.setSlipCount(slipCount);
        record.setShiftCm(shiftCm);
        record.setRemark(dto.getRemark());
        inspectionRepository.save(record);

        log.info("[{}] {} pre-wave inspection for {} (slip={}, shift={}cm)",
                accessControlService.currentRole(),
                reInspected ? "Updated" : "Created",
                equipment.getEquipmentCode(), slipCount, shiftCm);

        return new PreWaveInspectionUpsertResult(reInspected);
    }

    /** 删除某条点检记录（点错设备时撤回），合计随之减少。 */
    @Transactional
    public void delete(Long id) {
        assertDirector();
        PreWaveInspection record = inspectionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("点检记录不存在"));
        inspectionRepository.delete(record);
        log.info("[{}] Deleted pre-wave inspection record id={} equipment={}",
                accessControlService.currentRole(), id, record.getEquipmentCode());
    }

    /** 今晚两个合计数。 */
    public PreWaveInspectionSummaryDTO summaryToday() {
        assertDirector();
        return buildSummary(LocalDate.now());
    }

    private PreWaveInspectionSummaryDTO buildSummary(LocalDate date) {
        List<PreWaveInspection> records =
                inspectionRepository.findByInspectionDateOrderByUpdatedAtDesc(date);

        long totalSlip = records.stream()
                .mapToLong(r -> r.getSlipCount() == null ? 0L : r.getSlipCount())
                .sum();
        BigDecimal totalShift = records.stream()
                .map(r -> r.getShiftCm() == null ? BigDecimal.ZERO : r.getShiftCm())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return PreWaveInspectionSummaryDTO.builder()
                .inspectionDate(date)
                .inspectedCount(records.size())
                .totalSlipCount(totalSlip)
                .totalShiftCm(totalShift)
                .build();
    }

    /** 现场对账：现场另记的两份小计与页上合计逐项核对。 */
    public PreWaveReconcileResultDTO reconcile(PreWaveReconcileDTO dto) {
        assertDirector();
        PreWaveInspectionSummaryDTO sheet = buildSummary(LocalDate.now());

        long slipDiff = sheet.getTotalSlipCount() - dto.getOnSiteSlipCount();
        BigDecimal shiftDiff = sheet.getTotalShiftCm()
                .subtract(dto.getOnSiteShiftCm() == null ? BigDecimal.ZERO : dto.getOnSiteShiftCm());

        boolean slipMatched = slipDiff == 0;
        boolean shiftMatched = shiftDiff.compareTo(BigDecimal.ZERO) == 0;

        return PreWaveReconcileResultDTO.builder()
                .sheetSlipCount(sheet.getTotalSlipCount())
                .onSiteSlipCount(dto.getOnSiteSlipCount())
                .slipDiff(slipDiff)
                .slipMatched(slipMatched)
                .sheetShiftCm(sheet.getTotalShiftCm())
                .onSiteShiftCm(dto.getOnSiteShiftCm())
                .shiftDiff(shiftDiff)
                .shiftMatched(shiftMatched)
                .balanced(slipMatched && shiftMatched)
                .build();
    }

    private List<PreWaveInspectionDTO> toDTOs(List<PreWaveInspection> records) {
        return records.stream().map(r -> PreWaveInspectionDTO.builder()
                .id(r.getId())
                .inspectionDate(r.getInspectionDate())
                .equipmentId(r.getEquipmentId())
                .equipmentCode(r.getEquipmentCode())
                .equipmentName(r.getEquipmentName())
                .equipmentType(r.getEquipmentType())
                .slipCount(r.getSlipCount())
                .shiftCm(r.getShiftCm())
                .remark(r.getRemark())
                .updatedAt(r.getUpdatedAt())
                .build()
        ).collect(Collectors.toList());
    }

    /** 录入结果附带“是否复点覆盖”标记，供前端提示“同一台只算一次”。 */
    public record PreWaveInspectionUpsertResult(boolean reInspected) {
    }
}
