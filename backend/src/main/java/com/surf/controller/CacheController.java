package com.surf.controller;

import com.surf.dto.ApiResponse;
import com.surf.service.RedisCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/cache")
@RequiredArgsConstructor
public class CacheController {
    
    private final RedisCacheService redisCacheService;
    
    @PostMapping("/buffer-pad-template")
    public ResponseEntity<ApiResponse<Void>> setBufferPadTemplate(
            @RequestParam String templateName,
            @RequestBody Map<String, String> template) {
        redisCacheService.setBufferPadTemplate(templateName, template);
        return ResponseEntity.ok(ApiResponse.success("模板缓存成功", null));
    }
    
    @GetMapping("/buffer-pad-template/{templateName}")
    public ResponseEntity<ApiResponse<Map<String, String>>> getBufferPadTemplate(@PathVariable String templateName) {
        Map<String, String> template = redisCacheService.getBufferPadTemplate(templateName);
        if (template.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.error(404, "模板不存在"));
        }
        return ResponseEntity.ok(ApiResponse.success(template));
    }
    
    @DeleteMapping("/buffer-pad-template/{templateName}")
    public ResponseEntity<ApiResponse<Void>> deleteBufferPadTemplate(@PathVariable String templateName) {
        redisCacheService.deleteBufferPadTemplate(templateName);
        return ResponseEntity.ok(ApiResponse.success("模板删除成功", null));
    }
    
    @DeleteMapping("/buffer-pad-template")
    public ResponseEntity<ApiResponse<Void>> clearAllTemplates() {
        redisCacheService.clearAllTemplates();
        return ResponseEntity.ok(ApiResponse.success("所有模板已清除", null));
    }
}
