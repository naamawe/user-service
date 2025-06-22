package com.xhx.userservice.controller;

import com.github.pagehelper.PageInfo;
import com.xhx.userservice.common.util.IpUtils;
import com.xhx.userservice.entity.dto.LoginDTO;
import com.xhx.userservice.entity.dto.UserDTO;
import com.xhx.userservice.entity.dto.UserUpdateDTO;
import com.xhx.userservice.entity.vo.UserLoginVO;
import com.xhx.userservice.entity.vo.UserUpdateVO;
import com.xhx.userservice.entity.vo.UserVO;
import com.xhx.userservice.service.UserService;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;


import entity.pojo.Result;
import org.springframework.web.bind.annotation.*;

/**
 * @author master
 */
@RestController
public class UserController {

    @Resource
    private UserService userService;

    /**
     * 用户注册
     * @return 返回码
     */
    @PostMapping("/user/register")
    public Result register(@RequestBody UserDTO userDTO, HttpServletRequest request) {
        String ip = IpUtils.getClientIp(request);
        userService.register(userDTO, ip);
        return Result.ok("注册成功");
    }

    /**
     * 用户登录
     * @return 返回码
     */
    @PostMapping("/user/login")
    public Result login(@RequestBody LoginDTO loginDTO, HttpServletRequest request) {
        String ip = IpUtils.getClientIp(request);
        UserLoginVO login = userService.login(loginDTO, ip);
        return Result.ok(login);
    }

    /**
     * 分页用户列表
     * @return 返回码
     */
    @GetMapping("/users")
    public Result getUser(@RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "10") int size) {
        PageInfo<UserVO> pageInfo = userService.getUser(page, size);
        return Result.ok(pageInfo);
    }

    /**
     * 查询用户信息
     * @param userid 用户id
     * @return       返回码
     */
    @GetMapping("/user/{userid}")
    public Result getUserById(@PathVariable Long userid) {
        UserVO user = userService.getUserById(userid);
        return Result.ok(user);
    }

    /**
     * 修改用户信息
     * @param userId 用户id
     * @return       返回码
     */
    @PutMapping("/user/{userId}")
    public Result updateUser(@PathVariable Long userId, @RequestBody UserUpdateDTO userUpdateDTO) {
        UserUpdateVO user = userService.updateUser(userId, userUpdateDTO);
        return Result.ok(user);
    }

    /**
     * 密码重置
     * @return 返回码
     */
    @PostMapping("/user/reset-password")
    public Result resetPassword(@RequestParam Long userId, @RequestParam String password) {
        userService.resetPassword(userId, password);
        return Result.ok("修改密码成功");
    }
}
