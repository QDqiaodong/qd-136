package com.surf.service;

import com.surf.config.GlobalExceptionHandler;
import com.surf.dto.ApiResponse;
import com.surf.dto.EquipmentLoanCreateDTO;
import com.surf.dto.EquipmentUpdateDTO;
import com.surf.entity.Equipment;
import com.surf.entity.EquipmentAdjustRecord;
import com.surf.entity.EquipmentLoan;
import com.surf.repository.EquipmentAdjustRecordRepository;
import com.surf.repository.EquipmentLoanRepository;
import com.surf.repository.EquipmentRepository;
import com.surf.security.AccessControlService;
import com.surf.security.CurrentUser;
import com.surf.security.Role;
import com.surf.security.SecurityProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 设备改号：保存新编号时领用台账、设备调整流水必须跟着改成新号（含未还清的在借行），
 * 旧号在两本账里再查不到这台；新号被在册设备占用时拦截并写明占用者，本台编号维持原样；
 * 两台设备同时改同一个新号时只放行一台。
 */
@DataJpaTest
@Import({EquipmentService.class, EquipmentLoanService.class, AccessControlService.class})
@EnableConfigurationProperties(SecurityProperties.class)
class EquipmentRenameTest {

    @Autowired
    private EquipmentService equipmentService;

    @Autowired
    private EquipmentLoanService equipmentLoanService;

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Autowired
    private EquipmentLoanRepository equipmentLoanRepository;

    @Autowired
    private EquipmentAdjustRecordRepository equipmentAdjustRecordRepository;

    @Autowired
    private TestEntityManager entityManager;

    @BeforeEach
    void useDirectorRole() {
        CurrentUser.setRole(Role.DIRECTOR);
    }

    @AfterEach
    void clearRole() {
        CurrentUser.clear();
    }

    private Equipment saveEquipment(String code, String name) {
        return equipmentRepository.save(Equipment.builder()
                .equipmentCode(code)
                .equipmentName(name)
                .equipmentType("防滑扶手")
                .status("ACTIVE")
                .build());
    }

    private void borrow(Long equipmentId, String coach) {
        equipmentLoanService.borrow(EquipmentLoanCreateDTO.builder()
                .equipmentId(equipmentId)
                .borrowerName(coach)
                .build());
    }

    private void saveAdjustRecord(Equipment equipment) {
        equipmentAdjustRecordRepository.save(EquipmentAdjustRecord.builder()
                .equipmentId(equipment.getId())
                .equipmentCode(equipment.getEquipmentCode())
                .equipmentName(equipment.getEquipmentName())
                .previousWaveLevelCode("LOW")
                .previousWaveLevelName("低浪")
                .newWaveLevelCode("MEDIUM")
                .newWaveLevelName("中浪")
                .adjustReason("浪高调整")
                .operator("馆长")
                .build());
    }

    private EquipmentUpdateDTO renameDto(String newCode) {
        return EquipmentUpdateDTO.builder().equipmentCode(newCode).build();
    }

    @Test
    void renameCascadesToLoanLedgerAndAdjustRecords() {
        Equipment equipment = saveEquipment("FS-OLD-01", "低浪区防滑扶手");
        // 一笔已还清的领用 + 一笔仍在借的领用 + 一笔调整流水，都挂着旧号
        borrow(equipment.getId(), "王教练");
        equipmentLoanService.returnEquipment(equipment.getId(), "王教练");
        borrow(equipment.getId(), "李教练");
        saveAdjustRecord(equipment);

        equipmentService.updateEquipment(equipment.getId(), renameDto("FS-NEW-01"));

        // 设备名单是新号
        assertThat(equipmentRepository.findById(equipment.getId()).orElseThrow().getEquipmentCode())
                .isEqualTo("FS-NEW-01");

        // 领用台账全部换成新号：已还清的和未还清的在借行都不留旧号
        List<EquipmentLoan> loans =
                equipmentLoanRepository.findByEquipmentIdOrderByBorrowedAtDesc(equipment.getId());
        assertThat(loans).hasSize(2);
        assertThat(loans).allMatch(l -> "FS-NEW-01".equals(l.getEquipmentCode()));
        assertThat(loans.stream().filter(l -> l.getReturnedAt() == null)).hasSize(1);

        // 调整流水也换成新号
        List<EquipmentAdjustRecord> records =
                equipmentAdjustRecordRepository.findByEquipmentIdOrderByAdjustTimeDesc(equipment.getId());
        assertThat(records).hasSize(1);
        assertThat(records.get(0).getEquipmentCode()).isEqualTo("FS-NEW-01");

        // 用旧号再查：名单、台账、流水三处都找不到这台
        assertThat(equipmentRepository.findByEquipmentCode("FS-OLD-01")).isEmpty();
        assertThat(equipmentLoanRepository.findAll())
                .noneMatch(l -> "FS-OLD-01".equals(l.getEquipmentCode()));
        assertThat(equipmentAdjustRecordRepository.findAll())
                .noneMatch(r -> "FS-OLD-01".equals(r.getEquipmentCode()));
    }

    @Test
    void renameSurvivesReload() {
        Equipment equipment = saveEquipment("FS-OLD-02", "中浪区防滑扶手");
        borrow(equipment.getId(), "当班教练");
        saveAdjustRecord(equipment);

        equipmentService.updateEquipment(equipment.getId(), renameDto("FS-NEW-02"));

        // 模拟“关掉设备管理再打开”：清掉持久化上下文，强制从库重读
        entityManager.flush();
        entityManager.clear();

        assertThat(equipmentRepository.findById(equipment.getId()).orElseThrow().getEquipmentCode())
                .isEqualTo("FS-NEW-02");
        assertThat(equipmentLoanRepository.findByEquipmentIdOrderByBorrowedAtDesc(equipment.getId()))
                .allMatch(l -> "FS-NEW-02".equals(l.getEquipmentCode()));
        assertThat(equipmentAdjustRecordRepository.findByEquipmentIdOrderByAdjustTimeDesc(equipment.getId()))
                .allMatch(r -> "FS-NEW-02".equals(r.getEquipmentCode()));
    }

    @Test
    void renameToOccupiedCodeIsBlockedAndNamesOccupant() {
        Equipment occupant = saveEquipment("FS-TAKEN", "高浪区防滑扶手");
        Equipment equipment = saveEquipment("FS-OLD-03", "低浪区防滑扶手");
        borrow(equipment.getId(), "王教练");
        saveAdjustRecord(equipment);

        // 新号已被另一台在册设备占用：拦截，报错写明被哪一台占着
        assertThatThrownBy(() -> equipmentService.updateEquipment(equipment.getId(), renameDto("FS-TAKEN")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("FS-TAKEN")
                .hasMessageContaining("高浪区防滑扶手")
                .hasMessageContaining("占用");

        entityManager.clear();

        // 本台编号维持原样，两本账也仍是旧号
        assertThat(equipmentRepository.findById(equipment.getId()).orElseThrow().getEquipmentCode())
                .isEqualTo("FS-OLD-03");
        assertThat(equipmentLoanRepository.findByEquipmentIdOrderByBorrowedAtDesc(equipment.getId()))
                .allMatch(l -> "FS-OLD-03".equals(l.getEquipmentCode()));
        assertThat(equipmentAdjustRecordRepository.findByEquipmentIdOrderByAdjustTimeDesc(equipment.getId()))
                .allMatch(r -> "FS-OLD-03".equals(r.getEquipmentCode()));
        // 占用者不受影响
        assertThat(equipmentRepository.findById(occupant.getId()).orElseThrow().getEquipmentCode())
                .isEqualTo("FS-TAKEN");
    }

    @Test
    void onlyOneSucceedsWhenTwoEquipmentsRaceToSameNewCode() {
        Equipment first = saveEquipment("FS-OLD-04", "低浪区防滑扶手");
        Equipment second = saveEquipment("FS-OLD-05", "中浪区防滑扶手");

        // 第一台改号成功并落库（唯一约束放行）
        equipmentService.updateEquipment(first.getId(), renameDto("FS-COMMON"));
        entityManager.flush();
        entityManager.clear();

        // 第二台几乎同时改同一个新号：必须被拦下，占用提示写明被第一台占着
        assertThatThrownBy(() -> equipmentService.updateEquipment(second.getId(), renameDto("FS-COMMON")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("FS-COMMON")
                .hasMessageContaining("低浪区防滑扶手")
                .hasMessageContaining("占用");

        entityManager.clear();

        // 不能两本档案共用一个号：新号只在第一台名下，第二台维持原号
        assertThat(equipmentRepository.findByEquipmentCode("FS-COMMON").orElseThrow().getId())
                .isEqualTo(first.getId());
        assertThat(equipmentRepository.findById(second.getId()).orElseThrow().getEquipmentCode())
                .isEqualTo("FS-OLD-05");
    }

    @Test
    void blankCodeIsRejected() {
        Equipment equipment = saveEquipment("FS-OLD-06", "低浪区防滑扶手");

        assertThatThrownBy(() -> equipmentService.updateEquipment(equipment.getId(), renameDto("   ")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("设备编号不能为空");

        assertThat(equipmentRepository.findById(equipment.getId()).orElseThrow().getEquipmentCode())
                .isEqualTo("FS-OLD-06");
    }

    @Test
    void occupiedExceptionHandlerNamesOccupant() {
        // 并发改号落选者撞唯一约束后，全局异常处理另起查询，占用提示写明被哪一台占着
        saveEquipment("FS-TAKEN-2", "高浪区防滑扶手");
        entityManager.flush();
        entityManager.clear();

        GlobalExceptionHandler handler = new GlobalExceptionHandler(equipmentRepository);
        ApiResponse<Void> response = handler.handleEquipmentCodeOccupied(
                new EquipmentCodeOccupiedException("FS-TAKEN-2", "FS-OLD-07"));

        assertThat(response.getCode()).isEqualTo(400);
        assertThat(response.getMessage())
                .contains("FS-TAKEN-2")
                .contains("高浪区防滑扶手")
                .contains("占用")
                .contains("FS-OLD-07");
    }
}
