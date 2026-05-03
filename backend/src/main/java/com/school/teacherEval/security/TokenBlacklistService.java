package com.school.teacherEval.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Token 黑名单服务
 * <p>
 * 内部使用场景下采用内存存储（ConcurrentHashMap）。
 * 如需集群部署或多实例水平扩展，请改用 Redis + StringRedisTemplate。
 * </p>
 */
@Service
@Slf4j
public class TokenBlacklistService {

    // token -> 过期时间
    private final Map<String, Date> blacklist = new ConcurrentHashMap<>();

    /**
     * 将 Token 加入黑名单
     */
    public void blacklistToken(String token, Date expiration) {
        if (token != null && !token.isBlank()) {
            blacklist.put(token, expiration);
            log.info("Token 已加入黑名单，过期时间: {}", expiration);
        }
    }

    /**
     * 检查 Token 是否在黑名单中
     */
    public boolean isBlacklisted(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        return blacklist.containsKey(token);
    }

    /**
     * 定时清理已过期的黑名单记录（每 10 分钟执行一次）
     */
    @Scheduled(fixedRate = 600_000)
    public void cleanupExpiredTokens() {
        Date now = new Date();
        int beforeSize = blacklist.size();
        blacklist.entrySet().removeIf(entry -> entry.getValue() != null && entry.getValue().before(now));
        int afterSize = blacklist.size();
        if (beforeSize != afterSize) {
            log.debug("清理过期黑名单 Token: {} -> {}", beforeSize, afterSize);
        }
    }
}
