package com.xhx.userservice.client;

import entiey.pojo.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @author master
 */
@FeignClient("permission-service")
public interface PermissionClient {

    /**
     * 调用权限管理接口绑定默认角色
     * @return
     */
    @PostMapping("/permission/bindDefaultRole")
    Result bindDefaultRole(@RequestParam("userId") Long userId);

    /**
     * 查询用户角色编码
     */
    @GetMapping("/permission/getUserRoleCode")
    Result getUserRoleCode(@RequestParam("userId") Long userId);

    /**
     * 将用户升级为管理员
     */
    @PostMapping("/permission/upgradeToAdmin")
    Result upgradeToAdmin(@RequestParam("userId") Long userId);

    /**
     * 将用户降级为普通用户
     */
    @PostMapping("/permission/downgradeToUser")
    Result downgradeToUser(@RequestParam("userId") Long userId);

    /**
     * 根据用户等级查询用户信息
     */
    @GetMapping("/permission/getUserIdsByRoleCode")
    Result getUserIdsByRoleCode(@RequestParam Integer roleCode);
}
