package com.surf.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisCacheService {
    
    private static final String BUFFER_PAD_TEMPLATE_KEY = "buffer_pad:template";
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    public void setBufferPadTemplate(String templateName, Map<String, String> template) {
        HashOperations<String, String, String> hashOps = redisTemplate.opsForHash();
        hashOps.putAll(BUFFER_PAD_TEMPLATE_KEY + ":" + templateName, template);
        redisTemplate.expire(BUFFER_PAD_TEMPLATE_KEY + ":" + templateName, 24, TimeUnit.HOURS);
        log.info("Cached buffer pad template: {}", templateName);
    }
    
    public Map<String, String> getBufferPadTemplate(String templateName) {
        HashOperations<String, String, String> hashOps = redisTemplate.opsForHash();
        return hashOps.entries(BUFFER_PAD_TEMPLATE_KEY + ":" + templateName);
    }
    
    public void deleteBufferPadTemplate(String templateName) {
        redisTemplate.delete(BUFFER_PAD_TEMPLATE_KEY + ":" + templateName);
        log.info("Deleted buffer pad template: {}", templateName);
    }
    
    public void clearAllTemplates() {
        var keys = redisTemplate.keys(BUFFER_PAD_TEMPLATE_KEY + ":*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.info("Cleared all buffer pad templates");
        }
    }
    
    public void setCache(String key, Object value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
        log.debug("Cached key: {} with timeout: {} {}", key, timeout, unit);
    }
    
    public Object getCache(String key) {
        return redisTemplate.opsForValue().get(key);
    }
    
    public void deleteCache(String key) {
        redisTemplate.delete(key);
        log.debug("Deleted cache key: {}", key);
    }
}
