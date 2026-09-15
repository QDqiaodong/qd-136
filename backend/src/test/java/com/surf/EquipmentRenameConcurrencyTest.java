package com.surf;

import com.surf.dto.EquipmentUpdateDTO;
import com.surf.entity.Equipment;
import com.surf.repository.EquipmentRepository;
import com.surf.service.EquipmentCodeOccupiedException;
import com.surf.service.EquipmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 并发改号：两人几乎同时把两台空闲设备改成同一个新号，只能成功一台；
 * 另一台必须被拦下（占用提示），不能两本档案共用一个号，落选者编号维持原样。
 */
@SpringBootTest
class EquipmentRenameConcurrencyTest {

    @Autowired
    private EquipmentService equipmentService;

    @Autowired
    private EquipmentRepository equipmentRepository;

    private Equipment saveEquipment(String code, String name) {
        return equipmentRepository.save(Equipment.builder()
                .equipmentCode(code)
                .equipmentName(name)
                .equipmentType("防滑扶手")
                .status("ACTIVE")
                .build());
    }

    @Test
    void onlyOneSucceedsWhenRacingToSameNewCode() throws Exception {
        Equipment first = saveEquipment("RACE-A", "低浪区防滑扶手");
        Equipment second = saveEquipment("RACE-B", "中浪区防滑扶手");

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<String>> futures = new ArrayList<>();
        for (Long id : List.of(first.getId(), second.getId())) {
            futures.add(pool.submit(() -> {
                ready.countDown();
                go.await();
                try {
                    equipmentService.updateEquipment(id,
                            EquipmentUpdateDTO.builder().equipmentCode("RACE-NEW").build());
                    return "OK";
                } catch (IllegalArgumentException | EquipmentCodeOccupiedException e) {
                    // 预检拦下或撞唯一约束回滚，都算被占用拦截
                    return "BLOCKED";
                }
            }));
        }
        ready.await(10, TimeUnit.SECONDS);
        go.countDown();

        List<String> results = new ArrayList<>();
        for (Future<String> future : futures) {
            results.add(future.get(30, TimeUnit.SECONDS));
        }
        pool.shutdown();

        // 只能成功一台，另一台必须看到占用拦截
        assertThat(results).containsExactlyInAnyOrder("OK", "BLOCKED");

        // 不能两本档案共用一个号：新号只挂在一台名下，落选者维持原号
        List<Equipment> holders = equipmentRepository.findAll().stream()
                .filter(e -> "RACE-NEW".equals(e.getEquipmentCode()))
                .toList();
        assertThat(holders).hasSize(1);
        Equipment loser = holders.get(0).getId().equals(first.getId()) ? second : first;
        String loserOldCode = loser.getId().equals(first.getId()) ? "RACE-A" : "RACE-B";
        assertThat(equipmentRepository.findById(loser.getId()).orElseThrow().getEquipmentCode())
                .isEqualTo(loserOldCode);
    }
}
