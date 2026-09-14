package com.surf.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * 角色与授权范围配置。
 *
 * surf.security.default-role                 未携带角色头时的兜底角色
 * surf.security.coach-authorized-wave-levels 教练被授权可操作的浪高档位编码
 * surf.security.auxiliary-equipment-types    辅助设备类型（教练仅能接触这些设备）
 */
@Data
@Component
@ConfigurationProperties(prefix = "surf.security")
public class SecurityProperties {

    /** 未携带 X-Role 头时采用的默认角色。 */
    private Role defaultRole = Role.DIRECTOR;

    /** 教练被授权的浪高档位编码，默认低浪、中浪（高浪不在授权范围内）。 */
    private List<String> coachAuthorizedWaveLevels = Arrays.asList("LOW", "MEDIUM");

    /** 允许教练操作的辅助设备类型。 */
    private List<String> auxiliaryEquipmentTypes = Arrays.asList("防滑扶手", "缓冲挡垫");
}
