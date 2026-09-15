package com.surf;

import com.surf.entity.Equipment;
import com.surf.entity.EquipmentAdjustRecord;
import com.surf.entity.EquipmentLoan;
import com.surf.repository.EquipmentAdjustRecordRepository;
import com.surf.repository.EquipmentLoanRepository;
import com.surf.repository.EquipmentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 改号接口端到端：保存新编号后，设备名单、领用台账、设备调整流水三处都是新号，
 * 旧号在两本账里查不到这台；新号被在册设备占用时 400 拦截并写明占用者，本台编号维持原样。
 */
@SpringBootTest
@AutoConfigureMockMvc
class EquipmentRenameApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Autowired
    private EquipmentLoanRepository equipmentLoanRepository;

    @Autowired
    private EquipmentAdjustRecordRepository equipmentAdjustRecordRepository;

    private Equipment saveEquipment(String code, String name) {
        return equipmentRepository.save(Equipment.builder()
                .equipmentCode(code)
                .equipmentName(name)
                .equipmentType("防滑扶手")
                .status("ACTIVE")
                .build());
    }

    @Test
    void renameFollowsIntoLoanLedgerAndAdjustRecords() throws Exception {
        Equipment equipment = saveEquipment("API-OLD-01", "低浪区防滑扶手");
        // 一笔未还清的在借行 + 一笔调整流水，都挂着旧号
        equipmentLoanRepository.save(EquipmentLoan.builder()
                .equipmentId(equipment.getId())
                .equipmentCode("API-OLD-01")
                .equipmentName("低浪区防滑扶手")
                .borrowedBy("王教练")
                .build());
        equipmentAdjustRecordRepository.save(EquipmentAdjustRecord.builder()
                .equipmentId(equipment.getId())
                .equipmentCode("API-OLD-01")
                .equipmentName("低浪区防滑扶手")
                .newWaveLevelCode("LOW")
                .newWaveLevelName("低浪")
                .operator("馆长")
                .build());

        mockMvc.perform(put("/api/equipment/{id}", equipment.getId())
                        .header("X-Role", "DIRECTOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"equipmentCode\":\"API-NEW-01\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.equipmentCode", is("API-NEW-01")));

        // 关掉设备管理再打开：名单、领用台账、调整流水三处都是新号
        mockMvc.perform(get("/api/equipment/{id}", equipment.getId()).header("X-Role", "DIRECTOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.equipmentCode", is("API-NEW-01")));
        mockMvc.perform(get("/api/equipment-loan/history/{id}", equipment.getId()).header("X-Role", "DIRECTOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].equipmentCode", everyItem(is("API-NEW-01"))));
        mockMvc.perform(get("/api/binding/adjust-records")
                        .param("equipmentId", String.valueOf(equipment.getId()))
                        .header("X-Role", "DIRECTOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].equipmentCode", everyItem(is("API-NEW-01"))));

        // 用旧号再查：两本账里找不到这台（未还清的在借行也不留旧号）
        assertThat(equipmentRepository.findByEquipmentCode("API-OLD-01")).isEmpty();
        assertThat(equipmentLoanRepository.findAll())
                .noneMatch(l -> "API-OLD-01".equals(l.getEquipmentCode()));
        assertThat(equipmentAdjustRecordRepository.findAll())
                .noneMatch(r -> "API-OLD-01".equals(r.getEquipmentCode()));
    }

    @Test
    void occupiedNewCodeIsRejectedAndNamesOccupant() throws Exception {
        saveEquipment("API-TAKEN", "高浪区防滑扶手");
        Equipment equipment = saveEquipment("API-OLD-02", "中浪区防滑扶手");

        mockMvc.perform(put("/api/equipment/{id}", equipment.getId())
                        .header("X-Role", "DIRECTOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"equipmentCode\":\"API-TAKEN\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("API-TAKEN")))
                .andExpect(jsonPath("$.message", containsString("高浪区防滑扶手")))
                .andExpect(jsonPath("$.message", containsString("占用")));

        // 本台编号维持原样
        assertThat(equipmentRepository.findById(equipment.getId()).orElseThrow().getEquipmentCode())
                .isEqualTo("API-OLD-02");
    }
}
