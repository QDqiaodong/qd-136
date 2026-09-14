package com.surf.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 当前操作者及其可见范围，供前端按角色收窄界面。
 * 真正的越权拦截以后端为准，前端信息仅用于展示收窄。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthMeDTO {

    /** DIRECTOR 馆长 / COACH 教练 */
    private String role;

    private String roleName;

    /** 当前角色可见的浪高档位编码（馆长为全部档位） */
    private List<String> authorizedWaveLevelCodes;

    /** 教练可操作的辅助设备类型；馆长为空表示不限 */
    private List<String> auxiliaryEquipmentTypes;
}
