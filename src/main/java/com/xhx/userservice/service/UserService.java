package com.xhx.userservice.service;

import com.github.pagehelper.PageInfo;
import com.xhx.userservice.entiey.dto.LoginDTO;
import com.xhx.userservice.entiey.dto.UserDTO;
import com.xhx.userservice.entiey.pojo.User;
import com.xhx.userservice.entiey.vo.UserLoginVO;
import com.xhx.userservice.entiey.vo.UserVO;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
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
    PageInfo<UserVO> getUser(int page, int size, String ip);

    /**
     * 根据id获取用户信息
     * @param userid
     * @return
     */
    UserVO getUserById(Long userid, String ip);

    /**
     * 修改用户信息
     * @param userId
     * @return
     */
    UserVO updateUser(Long userId, UserDTO userDTO, String ip);

    /**
     * 重置密码
     */
    void resetPassword(Long userId, String password, String ip);

}
