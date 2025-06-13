package com.xhx.userservice.service;

import com.xhx.userservice.entiey.dto.LoginDTO;
import com.xhx.userservice.entiey.dto.UserDTO;
import com.xhx.userservice.entiey.pojo.User;
import com.xhx.userservice.entiey.vo.UserLoginVO;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author master
 */
public interface UserService {

    /**
     * 用户注册
     */
    void register(UserDTO userDTO, String ip);

    /**
     * 用户登录
     */
    UserLoginVO login(LoginDTO loginDTO, String ip);

    /**
     * 获取用户信息
     * @return
     */
    List<User> getUser();

    /**
     * 根据id获取用户信息
     * @param userid
     * @return
     */
    User getUserById(Long userid);

    /**
     * 修改用户信息
     * @param userId
     * @return
     */
    User updateUser(Long userId);

    /**
     * 重置密码
     */
    void resetPassword();

}
