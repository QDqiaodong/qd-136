package com.surf.repository;

import com.surf.entity.WaveLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WaveLevelRepository extends JpaRepository<WaveLevel, Long> {

    Optional<WaveLevel> findByLevelCode(String levelCode);

    /** 不区分大小写按代号查询，用于新增档位查重（MySQL 唯一索引本身也不区分大小写）。 */
    Optional<WaveLevel> findByLevelCodeIgnoreCase(String levelCode);

    boolean existsByLevelCode(String levelCode);

    /** 档位列表固定按显示顺序升序；顺序相同按 id 兜底，保证排序稳定。 */
    List<WaveLevel> findAllByOrderBySortOrderAscIdAsc();
}
