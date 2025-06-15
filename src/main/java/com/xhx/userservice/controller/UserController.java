package com.xhx.userservice.controller;

import com.github.pagehelper.PageInfo;
import com.xhx.userservice.common.util.IpUtils;
import com.xhx.userservice.entity.dto.LoginDTO;
import com.xhx.userservice.entity.dto.UserDTO;
import com.xhx.userservice.entity.vo.UserLoginVO;
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
    public Result getUser(@RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "10") int size) {
        PageInfo<UserVO> pageInfo = userService.getUser(page, size);
        return Result.ok(pageInfo);
    }

    /**
     * 查询用户信息
     * @param userid
     * @return
     */
    @GetMapping("/{userid}")
    public Result getUserById(@PathVariable Long userid) {
        UserVO user = userService.getUserById(userid);
        return Result.ok(user);
    }

    /**
     * 修改用户信息
     * @param userId
     * @return
     */
    @PutMapping("/{userId}")
    public Result updateUser(@PathVariable Long userId, UserDTO userDTO) {
        UserVO user = userService.updateUser(userId, userDTO);
        return Result.ok(user);
    }

    /**
     * 密码重置
     * @return
     */
    @PostMapping("/reset-password")
    public Result resetPassword(Long userId, String password) {
        userService.resetPassword(userId, password);
        return Result.ok("修改密码成功");
    }
}
