package com.surf.repository;

import com.surf.entity.Equipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EquipmentRepository extends JpaRepository<Equipment, Long> {
    
    Optional<Equipment> findByEquipmentCode(String equipmentCode);
    
    boolean existsByEquipmentCode(String equipmentCode);
    
    List<Equipment> findByEquipmentType(String equipmentType);
    
    List<Equipment> findByStatus(String status);
    
    @Query("SELECT e FROM Equipment e WHERE e.status = 'ACTIVE'")
    List<Equipment> findAllActive();
}
