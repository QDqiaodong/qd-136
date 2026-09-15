package com.surf.service;

import com.surf.entity.Equipment;
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

/**
 * 设备名单与领用台账口径一致：在借即标在借并写明教练，归还后不再标记；
 * 关页再开（清持久化上下文重读）状态仍与台账一致。
 */
@DataJpaTest
@Import({EquipmentService.class, EquipmentLoanService.class, AccessControlService.class})
@EnableConfigurationProperties(SecurityProperties.class)
class EquipmentLoanStatusTest {

    @Autowired
    private EquipmentService equipmentService;

    @Autowired
    private EquipmentLoanService equipmentLoanService;

    @Autowired
    private EquipmentRepository equipmentRepository;

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

    private Equipment saveEquipment(String code) {
        return equipmentRepository.save(Equipment.builder()
                .equipmentCode(code)
                .equipmentName("防滑扶手")
                .equipmentType("防滑扶手")
                .status("ACTIVE")
                .build());
    }

    private Equipment findInList(Long id) {
        List<Equipment> all = equipmentService.getAllEquipments();
        return all.stream().filter(e -> e.getId().equals(id)).findFirst().orElseThrow();
    }

    @Test
    void listMarksBorrowedThenClearsAfterReturn() {
        Equipment equipment = saveEquipment("FS-10");

        // 初始空闲
        assertThat(findInList(equipment.getId()).getBorrowed()).isEqualTo(false);

        equipmentLoanService.borrow(
                com.surf.dto.EquipmentLoanCreateDTO.builder()
                        .equipmentId(equipment.getId())
                        .borrowerName("当班教练")
                        .build());

        // 借给当班教练后，名单必须同步标在借并写明借用人（两本账对得上）
        Equipment onLoan = findInList(equipment.getId());
        assertThat(onLoan.getBorrowed()).isTrue();
        assertThat(onLoan.getBorrowedBy()).isEqualTo("当班教练");
        assertThat(onLoan.getActiveLoanId()).isNotNull();

        // 模拟关页再开
        entityManager.flush();
        entityManager.clear();
        assertThat(findInList(equipment.getId()).getBorrowed()).isTrue();

        equipmentLoanService.returnEquipment(equipment.getId(), "当班教练");

        // 还清后名单回到空闲，旧的在借标记不再残留
        entityManager.flush();
        entityManager.clear();
        Equipment returned = findInList(equipment.getId());
        assertThat(returned.getBorrowed()).isEqualTo(false);
        assertThat(returned.getBorrowedBy()).isNull();
        assertThat(returned.getActiveLoanId()).isNull();
    }
}
