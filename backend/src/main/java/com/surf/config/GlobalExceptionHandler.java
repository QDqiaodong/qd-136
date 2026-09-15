package com.surf.config;

import com.surf.dto.ApiResponse;
import com.surf.repository.EquipmentRepository;
import com.surf.security.AccessDeniedException;
import com.surf.service.EquipmentCodeOccupiedException;
import com.surf.service.EquipmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final EquipmentRepository equipmentRepository;
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        log.warn("Validation error: {}", errors);
        return ApiResponse.error(400, "参数校验失败");
    }
    
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponse<Void> handleAccessDeniedException(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return ApiResponse.error(403, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("Illegal argument: {}", ex.getMessage());
        return ApiResponse.error(400, ex.getMessage());
    }

    /**
     * 并发改号的落选者：唯一约束撞号时原事务已回滚（本台编号维持原样），
     * 此处另起查询找到占着新号的那台设备，占用提示写明被哪一台占着。
     */
    @ExceptionHandler(EquipmentCodeOccupiedException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleEquipmentCodeOccupied(EquipmentCodeOccupiedException ex) {
        String message = equipmentRepository.findByEquipmentCode(ex.getNewCode())
                .map(occupant -> EquipmentService.occupiedMessage(ex.getNewCode(), ex.getOldCode(), occupant))
                .orElse("设备编号 " + ex.getNewCode() + " 已被其他设备占用，本台编号维持 " + ex.getOldCode());
        log.warn("Equipment code occupied: {}", message);
        return ApiResponse.error(400, message);
    }
    
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleGenericException(Exception ex) {
        log.error("Unexpected error: ", ex);
        return ApiResponse.error("服务器内部错误");
    }
}
