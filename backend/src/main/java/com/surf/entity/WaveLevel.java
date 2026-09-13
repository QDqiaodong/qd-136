package com.surf.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "wave_level", indexes = {
    @Index(name = "idx_wave_level_code", columnList = "level_code")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WaveLevel {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "level_code", nullable = false, unique = true, length = 20)
    private String levelCode;
    
    @Column(name = "level_name", nullable = false, length = 50)
    private String levelName;
    
    @Column(name = "description", length = 200)
    private String description;
    
    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;
    
    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
