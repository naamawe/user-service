package com.xhx.userservice.service;

import com.github.pagehelper.PageInfo;
import com.xhx.userservice.entity.dto.LoginDTO;
import com.xhx.userservice.entity.dto.UserDTO;
import com.xhx.userservice.entity.vo.UserLoginVO;
import com.xhx.userservice.entity.vo.UserVO;


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
    PageInfo<UserVO> getUser(int page, int size);

    /**
     * 根据id获取用户信息
     * @param userid
     * @return
     */
    UserVO getUserById(Long userid);

    /**
     * 修改用户信息
     * @param userId
     * @return
     */
    UserVO updateUser(Long userId, UserDTO userDTO);

    /**
     * 重置密码
     */
    void resetPassword(Long userId, String password);

}
