package com.xhx.userservice.controller;

import com.xhx.userservice.common.util.IpUtils;
import com.xhx.userservice.entiey.dto.LoginDTO;
import com.xhx.userservice.entiey.dto.UserDTO;
import com.xhx.userservice.entiey.pojo.Result;
import com.xhx.userservice.entiey.pojo.User;
import com.xhx.userservice.entiey.vo.UserLoginVO;
import com.xhx.userservice.service.UserService;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author master
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    /**
     * 用户注册
     * @return
     */
    @PostMapping("/register")
    public Result register(@RequestBody UserDTO userDTO, HttpServletRequest request) {
        String ip = IpUtils.getClientIp(request);
        userService.register(userDTO, ip);
        return Result.ok("注册成功");
    }

    /**
     * 用户登录
     * @return
     */
    @PostMapping("/login")
    public Result login(@RequestBody LoginDTO loginDTO, HttpServletRequest request) {
        String ip = IpUtils.getClientIp(request);
        UserLoginVO login = userService.login(loginDTO, ip);
        return Result.ok(login);
    }

    /**
     * 分页用户列表
     * @return
     */
    @GetMapping
    public Result getUser() {
        List<User> user = userService.getUser();
        return Result.ok(user);
    }

    /**
     * 查询用户信息
     * @param userid
     * @return
     */
    @GetMapping("/{userid}")
    public Result getUserById(@PathVariable Long userid) {
        User user = userService.getUserById(userid);
        return Result.ok(user);
    }

    /**
     * 修改用户信息
     * @param userId
     * @return
     */
    @PutMapping("/userId")
    public Result updateUser(@PathVariable Long userId) {
        User user = userService.updateUser(userId);
        return Result.ok(user);
    }

    /**
     * 密码重置
     * @return
     */
    @PostMapping("/reset-password")
    public Result resetPassword() {
        userService.resetPassword();
        return Result.ok();
    }
}
