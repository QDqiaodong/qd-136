package com.surf.service;

import com.surf.dto.EquipmentLoanCreateDTO;
import com.surf.entity.Equipment;
import com.surf.entity.EquipmentLoan;
import com.surf.repository.EquipmentLoanRepository;
import com.surf.repository.EquipmentRepository;
import com.surf.security.AccessControlService;
import com.surf.security.CurrentUser;
import com.surf.security.Role;
import com.surf.security.SecurityProperties;
import org.junit.jupiter.api.AfterEach;
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
 * 领用 / 归还台账对应的服务层行为：
 * 未还清不许再借并写明借用人；归还后可再借；关页再开（清持久化上下文重读）
 * 在借仍标在借、已还清不再标记。
 */
@DataJpaTest
@Import({EquipmentLoanService.class, AccessControlService.class})
@EnableConfigurationProperties(SecurityProperties.class)
class EquipmentLoanServiceTest {

    @Autowired
    private EquipmentLoanService equipmentLoanService;

    @Autowired
    private EquipmentLoanRepository equipmentLoanRepository;

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Autowired
    private TestEntityManager entityManager;

    @AfterEach
    void clearRole() {
        CurrentUser.clear();
    }

    private Equipment saveEquipment(String code) {
        return equipmentRepository.save(Equipment.builder()
                .equipmentCode(code)
                .equipmentName("防滑扶手")
                .equipmentType("防滑扶手")
                .status("ACTIVE")
                .build());
    }

    private EquipmentLoanCreateDTO borrowDto(Long id, String coach) {
        return EquipmentLoanCreateDTO.builder()
                .equipmentId(id)
                .borrowerName(coach)
                .build();
    }

    @Test
    void cannotBorrowAgainBeforeReturnedAndNamesCurrentBorrower() {
        CurrentUser.setRole(Role.DIRECTOR);
        Equipment equipment = saveEquipment("FS-01");

        equipmentLoanService.borrow(borrowDto(equipment.getId(), "王教练"));

        // 第二人再登记领用：拦截，并写明已经借给哪位教练
        assertThatThrownBy(() -> equipmentLoanService.borrow(borrowDto(equipment.getId(), "李教练")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("王教练")
                .hasMessageContaining("尚未归还");

        // 第二条领用没有落库，台账只有一笔且仍在借
        List<EquipmentLoan> loans =
                equipmentLoanRepository.findByEquipmentIdOrderByBorrowedAtDesc(equipment.getId());
        assertThat(loans).hasSize(1);
        assertThat(loans.get(0).getBorrowedBy()).isEqualTo("王教练");
        assertThat(loans.get(0).getReturnedAt()).isNull();
    }

    @Test
    void canBorrowAgainAfterReturnedAndMarkerDoesNotPersist() {
        CurrentUser.setRole(Role.DIRECTOR);
        Equipment equipment = saveEquipment("FS-02");

        EquipmentLoan first = equipmentLoanService.borrow(borrowDto(equipment.getId(), "王教练"));
        equipmentLoanService.returnEquipment(equipment.getId(), "王教练");

        // 模拟“关页再开”：清掉持久化上下文，强制从库重读
        entityManager.flush();
        entityManager.clear();

        // 已还清：旧记录有归还时间，查不到在借记录，不再标在借
        assertThat(equipmentLoanService.getActiveLoan(equipment.getId())).isEmpty();
        EquipmentLoan reloadedFirst = equipmentLoanRepository.findById(first.getId()).orElseThrow();
        assertThat(reloadedFirst.getReturnedAt()).isNotNull();

        // 还清之后才能再借给另一位教练
        EquipmentLoan second = equipmentLoanService.borrow(borrowDto(equipment.getId(), "李教练"));
        assertThat(second.getBorrowedBy()).isEqualTo("李教练");
        assertThat(equipmentLoanService.getActiveLoan(equipment.getId()))
                .get()
                .extracting(EquipmentLoan::getBorrowedBy)
                .isEqualTo("李教练");
    }

    @Test
    void activeLoanSurvivesReload() {
        CurrentUser.setRole(Role.DIRECTOR);
        Equipment equipment = saveEquipment("FS-03");

        equipmentLoanService.borrow(borrowDto(equipment.getId(), "当班教练"));

        entityManager.flush();
        entityManager.clear();

        // 关页再开，仍在借的要标在借并保留借用人
        EquipmentLoan active = equipmentLoanService.getActiveLoan(equipment.getId()).orElseThrow();
        assertThat(active.getBorrowedBy()).isEqualTo("当班教练");
        assertThat(active.getReturnedAt()).isNull();
    }

    @Test
    void returnWhenNotOnLoanIsRejected() {
        CurrentUser.setRole(Role.DIRECTOR);
        Equipment equipment = saveEquipment("FS-04");

        assertThatThrownBy(() -> equipmentLoanService.returnEquipment(equipment.getId(), "王教练"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不在借");
    }
}
