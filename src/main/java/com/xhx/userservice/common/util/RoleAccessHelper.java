package com.xhx.userservice.common.util;

import com.xhx.userservice.client.PermissionClient;
import com.xhx.userservice.common.exception.AccessDeniedException;
import com.xhx.userservice.entity.pojo.User;
import com.xhx.userservice.mapper.UserMapper;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static com.xhx.userservice.common.constant.MessageConstant.*;
import static com.xhx.userservice.common.constant.UserConstant.*;

/**
 * 此工具类由AI生成
 * @author ChatGPT
 */
public class RoleAccessHelper {

    public static void checkPermission(String currentRole, Long currentUserId, Long targetUserId, String targetRole) {
        switch (currentRole){
            case USER_ROLE_USER:
                if (!Objects.equals(currentUserId, targetUserId)){
                    throw new AccessDeniedException(USER_NO_PERMISSION);
                }
                break;
            case USER_ROLE_ADMIN:
                if (USER_ROLE_SUPER_ADMIN.equals(targetRole)){
                    throw new AccessDeniedException(ADMIN_NO_PERMISSION);
                }
                break;
            case USER_ROLE_SUPER_ADMIN:
                break;
            default:
                throw new AccessDeniedException(UNKNOWN_ROLE);
        }
    }

    public static List<User> getAccessibleUsers(String currentRole, Long currentUserId, UserMapper userMapper, PermissionClient permissionClient, RedisTemplate<String, Object> redisTemplate, RedissonClient redissonClient
    ) {
        return switch (currentRole) {
            case USER_ROLE_USER -> {
                User user = userMapper.getUserById(currentUserId);
                yield user == null ? List.of() : List.of(user);
            }

            case USER_ROLE_ADMIN -> {
                List<Long> userIds = getAdminUserIdsWithCache(permissionClient, redisTemplate, redissonClient);
                yield (userIds == null || userIds.isEmpty())
                        ? List.of()
                        : userMapper.getUsersByUserIds(userIds);
            }

            case USER_ROLE_SUPER_ADMIN -> userMapper.getAllUser();

            default -> List.of();
        };
    }

    private static List<Long> getAdminUserIdsWithCache(PermissionClient permissionClient, RedisTemplate<String, Object> redisTemplate, RedissonClient redissonClient) {
        String cacheKey = "user:ids:admin";
        List<Long> userIds = null;

        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return (List<Long>) cached;
            }

            RLock lock = redissonClient.getLock("lock:" + cacheKey);
            boolean locked = lock.tryLock(5, 20, TimeUnit.SECONDS);

            if (locked) {
                try {
                    cached = redisTemplate.opsForValue().get(cacheKey);
                    if (cached != null) {
                        return (List<Long>) cached;
                    }

                    userIds = (List<Long>) permissionClient.getUserIdsByRoleCode(2).getData();
                    if (userIds != null && !userIds.isEmpty()) {
                        int ttl = 600 + new Random().nextInt(300);
                        redisTemplate.opsForValue().set(cacheKey, userIds, ttl, TimeUnit.SECONDS);
                    }
                    return userIds;
                } finally {
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }
                }
            } else {
                Thread.sleep(100);
                return getAdminUserIdsWithCache(permissionClient, redisTemplate, redissonClient);
            }
        } catch (Exception e) {
            return (List<Long>) permissionClient.getUserIdsByRoleCode(2).getData();
        }
    }
}
