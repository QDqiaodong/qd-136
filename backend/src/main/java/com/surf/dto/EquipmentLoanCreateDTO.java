package com.surf.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 设备领用登记入参。
 * borrowerName 为当班教练姓名，必填并写在台账上；
 * 未还清（仍在借）时服务层拒绝再次领用。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EquipmentLoanCreateDTO {

    @NotNull(message = "设备ID不能为空")
    private Long equipmentId;

    @NotBlank(message = "领用人（当班教练）不能为空")
    @Size(max = 50, message = "领用人长度不能超过50")
    private String borrowerName;

    @Size(max = 500, message = "备注长度不能超过500")
    private String remark;
}
