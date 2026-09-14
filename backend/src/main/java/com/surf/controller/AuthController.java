package com.surf.controller;

import com.surf.dto.ApiResponse;
import com.surf.dto.AuthMeDTO;
import com.surf.entity.WaveLevel;
import com.surf.repository.WaveLevelRepository;
import com.surf.security.AccessControlService;
import com.surf.security.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AccessControlService accessControlService;
    private final WaveLevelRepository waveLevelRepository;

    /**
     * 返回当前操作者角色与可见范围。前端据此收窄界面；真正的越权拦截由后端写操作鉴权保证。
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthMeDTO>> me() {
        Role role = accessControlService.currentRole();

        Set<String> authorizedCodes;
        if (role == Role.DIRECTOR) {
            // 馆长可见全部已配置档位
            authorizedCodes = waveLevelRepository.findAll().stream()
                    .map(WaveLevel::getLevelCode)
                    .collect(Collectors.toSet());
        } else {
            authorizedCodes = accessControlService.getCoachAuthorizedWaveLevels();
        }

        List<String> orderedCodes = waveLevelRepository.findAll().stream()
                .map(WaveLevel::getLevelCode)
                .filter(authorizedCodes::contains)
                .collect(Collectors.toList());

        AuthMeDTO dto = AuthMeDTO.builder()
                .role(role.name())
                .roleName(role == Role.DIRECTOR ? "馆长" : "浪道教练")
                .authorizedWaveLevelCodes(orderedCodes)
                .auxiliaryEquipmentTypes(
                        role == Role.COACH ? accessControlService.getAuxiliaryEquipmentTypes() : List.of())
                .build();

        return ResponseEntity.ok(ApiResponse.success(dto));
    }
}
