package com.surf.security;

import com.surf.entity.Equipment;
import com.surf.entity.EquipmentAdjustRecord;
import com.surf.entity.EquipmentWaveLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 角色访问控制：所有与“辅助设备-浪高档位绑定”相关的可见范围与写操作都必须经过本服务。
 *
 * 馆长（DIRECTOR）：全部可见、全部可操作。
 * 教练（COACH）：
 *   - 只能看到授权档位；
 *   - 只能看到/操作防滑扶手、缓冲挡垫两类辅助设备；
 *   - 对未授权档位发起绑定/调整一律拒绝（抛出 {@link AccessDeniedException}，由全局异常处理转 403）。
 */
@Service
@RequiredArgsConstructor
public class AccessControlService {

    private final SecurityProperties properties;

    /** 当前请求角色；请求头未带角色时回退到配置的默认角色。 */
    public Role currentRole() {
        Role role = CurrentUser.getRole();
        return role != null ? role : properties.getDefaultRole();
    }

    public boolean isCoach() {
        return currentRole() == Role.COACH;
    }

    public boolean isDirector() {
        return currentRole() == Role.DIRECTOR;
    }

    /** 教练被授权的档位编码集合（大写）。 */
    public Set<String> getCoachAuthorizedWaveLevels() {
        return properties.getCoachAuthorizedWaveLevels().stream()
                .map(String::toUpperCase)
                .collect(Collectors.toSet());
    }

    private Set<String> getAuxiliaryTypes() {
        return Set.copyOf(properties.getAuxiliaryEquipmentTypes());
    }

    /** 辅助设备类型列表（顺序取配置顺序）。 */
    public List<String> getAuxiliaryEquipmentTypes() {
        return List.copyOf(properties.getAuxiliaryEquipmentTypes());
    }

    /**
     * 当前角色是否能看到某个浪高档位。
     * 馆长可见全部；教练仅可见授权档位。
     */
    public boolean canAccessWaveLevel(String waveLevelCode) {
        if (waveLevelCode == null) {
            return false;
        }
        if (isDirector()) {
            return true;
        }
        return getCoachAuthorizedWaveLevels().contains(waveLevelCode.toUpperCase());
    }

    /**
     * 当前角色是否能看到/操作某台设备。
     * 馆长可见全部；教练仅能接触防滑扶手、缓冲挡垫两类辅助设备。
     */
    public boolean canAccessEquipment(Equipment equipment) {
        if (equipment == null) {
            return false;
        }
        if (isDirector()) {
            return true;
        }
        return getAuxiliaryTypes().contains(equipment.getEquipmentType());
    }

    /** 初始绑定鉴权：教练只能把辅助设备绑定到授权档位。 */
    public void assertCanBind(Equipment equipment, String waveLevelCode) {
        if (isDirector()) {
            return;
        }
        if (!canAccessEquipment(equipment)) {
            throw new AccessDeniedException("没有权限：教练只能调整防滑扶手和缓冲挡垫");
        }
        if (!canAccessWaveLevel(waveLevelCode)) {
            throw new AccessDeniedException("没有权限：该浪高档位未授权给当前教练");
        }
    }

    /**
     * 档位调整鉴权。
     * 教练调整时，设备当前绑定的档位与目标新档位都必须在授权范围内——
     * 即不允许借“调整”之名去改未授权档位上的绑定。
     */
    public void assertCanAdjust(Equipment equipment, EquipmentWaveLevel currentBinding, String newWaveLevelCode) {
        if (isDirector()) {
            return;
        }
        if (!canAccessEquipment(equipment)) {
            throw new AccessDeniedException("没有权限：教练只能调整防滑扶手和缓冲挡垫");
        }
        if (currentBinding != null && !canAccessWaveLevel(currentBinding.getWaveLevelCode())) {
            throw new AccessDeniedException("没有权限：该设备绑定在未授权档位上，禁止调整");
        }
        if (!canAccessWaveLevel(newWaveLevelCode)) {
            throw new AccessDeniedException("没有权限：目标浪高档位未授权给当前教练");
        }
    }

    /** 查看单台设备绑定/历史前的鉴权，防止教练通过指定 ID 探测未授权设备。 */
    public void assertCanViewEquipment(Equipment equipment) {
        if (isDirector()) {
            return;
        }
        if (!canAccessEquipment(equipment)) {
            throw new AccessDeniedException("没有权限查看该设备");
        }
    }

    /** 列表收窄：教练只保留辅助设备；馆长保留全部。 */
    public List<Equipment> filterEquipments(List<Equipment> equipments) {
        if (isDirector()) {
            return equipments;
        }
        Set<String> auxTypes = getAuxiliaryTypes();
        return equipments.stream()
                .filter(e -> auxTypes.contains(e.getEquipmentType()))
                .collect(Collectors.toList());
    }

    /**
     * 绑定列表收窄：教练只保留“辅助设备 + 授权档位”的绑定；馆长保留全部。
     *
     * @param bindings     绑定记录
     * @param equipmentMap equipmentId -> 设备，用于判断设备类型
     */
    public List<EquipmentWaveLevel> filterBindings(List<EquipmentWaveLevel> bindings,
                                                   Map<Long, Equipment> equipmentMap) {
        if (isDirector()) {
            return bindings;
        }
        Set<String> authorizedLevels = getCoachAuthorizedWaveLevels();
        Set<String> auxTypes = getAuxiliaryTypes();
        return bindings.stream()
                .filter(b -> authorizedLevels.contains(b.getWaveLevelCode().toUpperCase()))
                .filter(b -> {
                    Equipment equipment = equipmentMap.get(b.getEquipmentId());
                    return equipment != null && auxTypes.contains(equipment.getEquipmentType());
                })
                .collect(Collectors.toList());
    }

    /** 调整记录收窄：教练只看辅助设备在授权档位上的记录；馆长看全部。 */
    public List<EquipmentAdjustRecord> filterAdjustRecords(List<EquipmentAdjustRecord> records,
                                                           Map<Long, Equipment> equipmentMap) {
        if (isDirector()) {
            return records;
        }
        Set<String> authorizedLevels = getCoachAuthorizedWaveLevels();
        Set<String> auxTypes = getAuxiliaryTypes();
        return records.stream()
                .filter(r -> authorizedLevels.contains(r.getNewWaveLevelCode().toUpperCase()))
                .filter(r -> r.getPreviousWaveLevelCode() == null
                        || authorizedLevels.contains(r.getPreviousWaveLevelCode().toUpperCase()))
                .filter(r -> {
                    Equipment equipment = equipmentMap.get(r.getEquipmentId());
                    return equipment != null && auxTypes.contains(equipment.getEquipmentType());
                })
                .collect(Collectors.toList());
    }
}
