package com.surf.repository;

import com.surf.entity.WaveLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WaveLevelRepository extends JpaRepository<WaveLevel, Long> {
    
    Optional<WaveLevel> findByLevelCode(String levelCode);
    
    boolean existsByLevelCode(String levelCode);
}
