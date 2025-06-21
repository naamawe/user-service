package com.xhx.userservice.client.fallback;

import com.xhx.userservice.client.PermissionClient;
import entity.pojo.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;

import static com.xhx.userservice.common.constant.MessageConstant.*;

/**
 * @author master
 */
@Component
@Slf4j
public class PermissionClientFallbackFactory implements FallbackFactory<PermissionClient> {

    @Override
    public PermissionClient create(Throwable cause) {
        return new PermissionClient() {
            @Override
            public Result bindDefaultRole(Long userId) {
                log.error("bindDefaultRole RPC调用失败，userId={}", userId, cause);
                return Result.fail(BIND_DEFAULT_ROLE_FAIL);
            }

            @Override
            public Result getUserRoleCode(Long userId) {
                log.error("getUserRoleCode RPC调用失败，userId={}", userId, cause);
                return Result.ok(Collections.emptyList());
            }

            @Override
            public Result upgradeToAdmin(Long userId) {
                log.error("upgradeToAdmin RPC调用失败，userId={}", userId, cause);
                return Result.fail(UPGRADE_TO_ADMIN_FAIL);
            }

            @Override
            public Result downgradeToUser(Long userId) {
                log.error("downgradeToUser RPC调用失败，userId={}", userId, cause);
                return Result.fail(DOWNGRADE_TO_USER_FAIL);
            }

            @Override
            public Result getUserIdsByRoleCode(Integer roleCode) {
                log.error("getUserIdsByRoleCode RPC调用失败，roleCode={}", roleCode, cause);
                return Result.ok(Collections.emptyList());
            }
        };
    }
}
