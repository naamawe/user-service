package com.xhx.userservice.common.util;

import com.xhx.userservice.client.PermissionClient;
import com.xhx.userservice.common.exception.AccessDeniedException;
import com.xhx.userservice.entity.pojo.User;
import com.xhx.userservice.mapper.UserMapper;

import java.util.List;
import java.util.Objects;

/**
 * 此工具类由AI生成
 * @author ChatGPT
 */
public class RoleAccessHelper {
    
    public static void checkPermission(String currentRole, Long currentUserId, Long targetUserId, PermissionClient permissionClient) {
        switch (currentRole){
            case "user":
                if (!Objects.equals(currentUserId, targetUserId)){
                    throw new AccessDeniedException("普通用户无权限修改其他用户密码");
                }
                break;
            case "admin":
                String targetRole = (String) permissionClient.getUserRoleCode(targetUserId).getData();
                if ("super_admin".equals(targetRole)){
                    throw new AccessDeniedException("管理员无权限修改超管密码");
                }
                break;
            case "super_admin":
                break;
            default:
                throw new AccessDeniedException("未知角色，拒绝访问");
        }
    }

    public static List<User> getAccessibleUsers(String currentRole, Long currentUserId, UserMapper userMapper, PermissionClient permissionClient) {
        return switch (currentRole) {
            case "user" -> {
                User user = userMapper.getUserById(currentUserId);
                yield user == null ? List.of() : List.of(user);
            }
            case "admin" -> {
                List<Long> userIds = (List<Long>) permissionClient.getUserIdsByRoleCode(2).getData();
                yield (userIds == null || userIds.isEmpty()) ? List.of() : userMapper.getUsersByUserIds(userIds);
            }
            case "super_admin" -> userMapper.getAllUser();
            default -> List.of();
        };
    }
}
