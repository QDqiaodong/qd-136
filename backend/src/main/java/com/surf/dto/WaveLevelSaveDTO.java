package com.surf.dto;

import lombok.Data;

/**
 * 档位管理页新增/编辑浪高档位的入参。
 * 档位代号仅新增时生效；编辑时代号是绑定关系的稳定标识，不允许修改。
 * 字段不设默认值，缺项保持 null，便于后端逐项报出缺失的必填项。
 */
@Data
public class WaveLevelSaveDTO {

    private String levelCode;

    private String levelName;

    private Integer sortOrder;

    private String description;
}
