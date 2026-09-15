package com.surf.service;

import com.surf.dto.BindingStatusDTO;
import com.surf.dto.EquipmentAdjustDTO;
import com.surf.dto.WaveLevelBindingDTO;
import com.surf.entity.Equipment;
import com.surf.entity.EquipmentAdjustRecord;
import com.surf.entity.EquipmentWaveLevel;
import com.surf.entity.WaveLevel;
import com.surf.repository.EquipmentAdjustRecordRepository;
import com.surf.repository.EquipmentRepository;
import com.surf.repository.EquipmentWaveLevelRepository;
import com.surf.repository.WaveLevelRepository;
import com.surf.security.AccessControlService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EquipmentWaveLevelService {

    private final EquipmentWaveLevelRepository equipmentWaveLevelRepository;
    private final EquipmentRepository equipmentRepository;
    private final WaveLevelRepository waveLevelRepository;
    private final EquipmentAdjustRecordRepository equipmentAdjustRecordRepository;
    private final AccessControlService accessControlService;
    private final BufferThicknessRuleService bufferThicknessRuleService;

    @Transactional
    public EquipmentWaveLevel bindWaveLevel(WaveLevelBindingDTO dto) {
        Equipment equipment = equipmentRepository.findById(dto.getEquipmentId())
                .orElseThrow(() -> new IllegalArgumentException("设备不存在"));
        if (!"ACTIVE".equals(equipment.getStatus())) {
            throw new IllegalArgumentException("设备已停用，不能绑定档位");
        }

        WaveLevel waveLevel = waveLevelRepository.findByLevelCode(dto.getWaveLevelCode())
                .orElseThrow(() -> new IllegalArgumentException("浪高档位不存在"));

        // 角色强制鉴权：教练只能把辅助设备绑定到授权档位，越权直接 403
        accessControlService.assertCanBind(equipment, waveLevel.getLevelCode());

        // 适浪厚度校验：缓冲挡垫厚度不达标档位下限时直接拦截，绑定不会落库
        bufferThicknessRuleService.assertThicknessSatisfied(equipment, waveLevel);

        // 同一台设备同一时刻只允许一个生效档位：先取出当前仍生效的旧绑定
        List<EquipmentWaveLevel> activeBindings = equipmentWaveLevelRepository
                .findAllByEquipmentIdAndExpireDateIsNull(equipment.getId());
        boolean alreadyOnTarget = activeBindings.stream()
                .anyMatch(b -> b.getWaveLevelCode().equals(waveLevel.getLevelCode()));
        if (alreadyOnTarget) {
            throw new IllegalArgumentException("设备当前已绑定在该档位，无需重复绑定");
        }

        // 先做新档生效：新绑定落库，设备在新档位立即可用
        EquipmentWaveLevel binding = EquipmentWaveLevel.builder()
                .equipmentId(equipment.getId())
                .waveLevelId(waveLevel.getId())
                .waveLevelCode(waveLevel.getLevelCode())
                .waveLevelName(waveLevel.getLevelName())
                .bindingType(dto.getBindingType())
                .effectiveDate(LocalDateTime.now())
                .build();

        EquipmentWaveLevel saved = equipmentWaveLevelRepository.save(binding);

        // 再做旧档失效：打上失效时间，旧档位名单里不再出现这台设备
        for (EquipmentWaveLevel stale : activeBindings) {
            stale.setExpireDate(LocalDateTime.now());
            equipmentWaveLevelRepository.save(stale);
            log.info("[{}] Expired previous binding of equipment {} on wave level {}",
                    accessControlService.currentRole(), equipment.getEquipmentCode(), stale.getWaveLevelCode());
        }

        log.info("[{}] Bound equipment {} to wave level {}",
                accessControlService.currentRole(), equipment.getEquipmentCode(), waveLevel.getLevelCode());
        return saved;
    }

    @Transactional
    public EquipmentWaveLevel adjustWaveLevel(EquipmentAdjustDTO dto) {
        Equipment equipment = equipmentRepository.findById(dto.getEquipmentId())
                .orElseThrow(() -> new IllegalArgumentException("设备不存在"));
        if (!"ACTIVE".equals(equipment.getStatus())) {
            throw new IllegalArgumentException("设备已停用，不能调整档位");
        }

        WaveLevel newWaveLevel = waveLevelRepository.findByLevelCode(dto.getNewWaveLevelCode())
                .orElseThrow(() -> new IllegalArgumentException("新浪高档位不存在"));

        Optional<EquipmentWaveLevel> currentBinding = equipmentWaveLevelRepository
                .findByEquipmentIdAndExpireDateIsNull(dto.getEquipmentId());

        // 角色强制鉴权：当前档位与目标档位都必须在教练授权范围内，防止借调整改动未授权档位
        accessControlService.assertCanAdjust(equipment, currentBinding.orElse(null), newWaveLevel.getLevelCode());

        // 适浪厚度校验：必须在旧绑定失效之前拦截，厚度不足时既不会绑上目标档位，也不会动原绑定
        bufferThicknessRuleService.assertThicknessSatisfied(equipment, newWaveLevel);

        String previousLevelCode = null;
        String previousLevelName = null;

        if (currentBinding.isPresent()) {
            EquipmentWaveLevel existing = currentBinding.get();
            previousLevelCode = existing.getWaveLevelCode();
            previousLevelName = existing.getWaveLevelName();

            if (previousLevelCode.equals(dto.getNewWaveLevelCode())) {
                throw new IllegalArgumentException("新档位与当前档位相同");
            }

            existing.setExpireDate(LocalDateTime.now());
            equipmentWaveLevelRepository.save(existing);
        }

        EquipmentWaveLevel newBinding = EquipmentWaveLevel.builder()
                .equipmentId(equipment.getId())
                .waveLevelId(newWaveLevel.getId())
                .waveLevelCode(newWaveLevel.getLevelCode())
                .waveLevelName(newWaveLevel.getLevelName())
                .bindingType("ADJUST")
                .effectiveDate(LocalDateTime.now())
                .build();

        EquipmentWaveLevel saved = equipmentWaveLevelRepository.save(newBinding);

        EquipmentAdjustRecord record = EquipmentAdjustRecord.builder()
                .equipmentId(equipment.getId())
                .equipmentCode(equipment.getEquipmentCode())
                .equipmentName(equipment.getEquipmentName())
                .previousWaveLevelCode(previousLevelCode)
                .previousWaveLevelName(previousLevelName)
                .newWaveLevelCode(newWaveLevel.getLevelCode())
                .newWaveLevelName(newWaveLevel.getLevelName())
                .adjustReason(dto.getAdjustReason())
                .operator(dto.getOperator())
                .remark(dto.getRemark())
                .build();

        equipmentAdjustRecordRepository.save(record);

        log.info("[{}] Adjusted equipment {} from {} to {}",
                accessControlService.currentRole(), equipment.getEquipmentCode(),
                previousLevelCode, newWaveLevel.getLevelCode());

        return saved;
    }

    public Optional<EquipmentWaveLevel> getCurrentBinding(Long equipmentId) {
        Optional<Equipment> equipment = equipmentRepository.findById(equipmentId);
        if (equipment.isEmpty()) {
            return Optional.empty();
        }
        // 教练查询非辅助设备 / 未授权档位的绑定一律拒绝，避免按 ID 探测
        accessControlService.assertCanViewEquipment(equipment.get());

        // 已停用设备没有"当前生效绑定"，不再当成在用
        if (!"ACTIVE".equals(equipment.get().getStatus())) {
            return Optional.empty();
        }
        Optional<EquipmentWaveLevel> binding =
                equipmentWaveLevelRepository.findByEquipmentIdAndExpireDateIsNull(equipmentId);
        if (binding.isPresent()
                && !accessControlService.canAccessWaveLevel(binding.get().getWaveLevelCode())) {
            throw new com.surf.security.AccessDeniedException("没有权限查看该浪高档位的绑定");
        }
        return binding;
    }

    public List<EquipmentWaveLevel> getBindingsByWaveLevel(String waveLevelCode) {
        if (!accessControlService.canAccessWaveLevel(waveLevelCode)) {
            throw new com.surf.security.AccessDeniedException("没有权限查看该浪高档位");
        }
        List<EquipmentWaveLevel> bindings = equipmentWaveLevelRepository.findActiveByWaveLevelCode(waveLevelCode);
        Map<Long, Equipment> equipmentMap = loadEquipmentMap();
        // 已停用设备的绑定不再视为该档位上的在用设备
        List<EquipmentWaveLevel> inServiceBindings = bindings.stream()
                .filter(b -> {
                    Equipment equipment = equipmentMap.get(b.getEquipmentId());
                    return equipment != null && "ACTIVE".equals(equipment.getStatus());
                })
                .collect(Collectors.toList());
        return accessControlService.filterBindings(inServiceBindings, equipmentMap);
    }

    public List<EquipmentWaveLevel> getBindingHistory(Long equipmentId) {
        Equipment equipment = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new IllegalArgumentException("设备不存在"));
        accessControlService.assertCanViewEquipment(equipment);

        List<EquipmentWaveLevel> history =
                equipmentWaveLevelRepository.findByEquipmentIdOrderByCreatedAtDesc(equipmentId);
        return accessControlService.filterBindings(history, Collections.singletonMap(equipmentId, equipment));
    }

    public List<EquipmentAdjustRecord> getAdjustRecords(Long equipmentId) {
        List<EquipmentAdjustRecord> records;
        if (equipmentId != null) {
            Equipment equipment = equipmentRepository.findById(equipmentId).orElse(null);
            if (equipment == null) {
                return List.of();
            }
            accessControlService.assertCanViewEquipment(equipment);
            records = equipmentAdjustRecordRepository.findByEquipmentIdOrderByAdjustTimeDesc(equipmentId);
            return accessControlService.filterAdjustRecords(records,
                    Collections.singletonMap(equipmentId, equipment));
        }
        records = equipmentAdjustRecordRepository.findAllByOrderByAdjustTimeDesc();
        return accessControlService.filterAdjustRecords(records, loadEquipmentMap());
    }

    /**
     * 绑定页“绑定状态”列表，服务端按角色收窄：
     * 馆长返回全部有效绑定；教练仅返回授权档位上防滑扶手、缓冲挡垫的绑定。
     * 刷新后重新请求本接口，列表天然只含当前角色被授权的设备。
     */
    public List<BindingStatusDTO> getScopedActiveBindings() {
        List<EquipmentWaveLevel> activeBindings = equipmentWaveLevelRepository.findAll().stream()
                .filter(b -> b.getExpireDate() == null)
                .collect(Collectors.toList());

        Map<Long, Equipment> equipmentMap = loadEquipmentMap();
        List<EquipmentWaveLevel> visibleBindings =
                accessControlService.filterBindings(activeBindings, equipmentMap);

        List<BindingStatusDTO> result = new ArrayList<>();
        for (EquipmentWaveLevel binding : visibleBindings) {
            Equipment equipment = equipmentMap.get(binding.getEquipmentId());
            // 已停用设备不再出现在绑定状态名单里，不再当成在用
            if (equipment == null || !"ACTIVE".equals(equipment.getStatus())) {
                continue;
            }
            result.add(BindingStatusDTO.builder()
                    .equipmentId(equipment.getId())
                    .equipmentCode(equipment.getEquipmentCode())
                    .equipmentName(equipment.getEquipmentName())
                    .equipmentType(equipment.getEquipmentType())
                    .bufferThickness(equipment.getBufferThickness())
                    .location(equipment.getLocation())
                    .bindingId(binding.getId())
                    .waveLevelCode(binding.getWaveLevelCode())
                    .waveLevelName(binding.getWaveLevelName())
                    .bindingType(binding.getBindingType())
                    .effectiveDate(binding.getEffectiveDate())
                    .build());
        }
        return result;
    }

    /**
     * 绑定页“选择设备”下拉数据，按角色收窄：馆长可选全部设备，
     * 教练只能选防滑扶手、缓冲挡垫。
     */
    public List<Equipment> getBindableEquipments() {
        return accessControlService.filterEquipments(equipmentRepository.findAllActive());
    }

    private Map<Long, Equipment> loadEquipmentMap() {
        return equipmentRepository.findAll().stream()
                .collect(Collectors.toMap(Equipment::getId, Function.identity(), (a, b) -> a));
    }

    public List<Object[]> countEquipmentByWaveLevel() {
        return equipmentWaveLevelRepository.countByWaveLevelCode();
    }
}
