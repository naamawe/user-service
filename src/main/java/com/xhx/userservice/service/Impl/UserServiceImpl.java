package com.xhx.userservice.service.Impl;

import cn.hutool.core.bean.BeanUtil;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.xhx.userservice.client.PermissionClient;
import com.xhx.userservice.common.exception.*;
import com.xhx.userservice.common.util.JwtUtils;
import com.xhx.userservice.common.util.RoleAccessHelper;
import com.xhx.userservice.common.util.SnowflakeIdWorker;
import com.xhx.userservice.config.JwtProperties;
import com.xhx.userservice.entity.dto.LoginDTO;
import com.xhx.userservice.entity.dto.UserDTO;
import com.xhx.userservice.entity.dto.UserUpdateDTO;
import com.xhx.userservice.entity.pojo.User;
import com.xhx.userservice.entity.vo.UserLoginVO;
import com.xhx.userservice.entity.vo.UserUpdateVO;
import com.xhx.userservice.entity.vo.UserVO;
import com.xhx.userservice.mapper.UserMapper;
import com.xhx.userservice.service.UserService;

import entity.dto.OperationLogDTO;
import entity.pojo.Result;
import exception.MessageException;
import io.seata.spring.annotation.GlobalTransactional;
import javax.annotation.Resource;

import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import uitls.UserContext;

import java.sql.Timestamp;
import java.util.*;

import static constant.mqConstant.*;

/**
 * @author master
 */
@Service
public class UserServiceImpl implements UserService {

    @Resource
    private UserMapper userMapper;
    @Resource
    private PasswordEncoder passwordEncoder;
    @Resource
    private PermissionClient permissionClient;
    @Resource
    private RabbitTemplate rabbitTemplate;
    @Resource
    private SnowflakeIdWorker idWorker;
    @Resource
    private JwtUtils jwtUtils;
    @Resource
    private JwtProperties jwtProperties;

    /**
     * 用户注册
     * @param userDTO
     * @param ip
     */
    @Override
    @GlobalTransactional(name = "user-service-tx-group",  rollbackFor = Exception.class)
    public void register(UserDTO userDTO, String ip) {

        User user = BeanUtil.copyProperties(userDTO, User.class);

        long userId = idWorker.nextId();
        user.setUserId(userId);

        String encodedPassword = passwordEncoder.encode(userDTO.getPassword());
        user.setPassword(encodedPassword);
        user.setGmtCreate(new Timestamp(System.currentTimeMillis()));
        userMapper.insertUser(user);

        try {
            permissionClient.bindDefaultRole(user.getUserId());
        } catch (Exception e) {
            throw new BindingException("绑定角色失败");
        }

        // 发送消息到 RabbitMQ
        constructAndSendMessage(userId, ip, "user_register", "用户注册成功");
    }

    /**
     * 用户登录
     * @param loginDTO
     * @param ip
     * @return
     */
    @Override
    public UserLoginVO login(LoginDTO loginDTO, String ip) {
        User user = userMapper.findByUsername(loginDTO.getUsername());

        if (user == null){
            throw new NullUserException("用户不存在");
        }
        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())){
            throw new PasswordErrorException("密码错误");
        }
        String role = (String) permissionClient.getUserRoleCode(user.getUserId()).getData();
        String token = jwtUtils.createToken(user.getUserId(), role, ip, jwtProperties.getTokenTTL());

        UserLoginVO userLoginVO = new UserLoginVO();
        userLoginVO.setUserId(user.getUserId());
        userLoginVO.setUsername(user.getUsername());
        userLoginVO.setToken(token);

        // 发送消息到 RabbitMQ
        constructAndSendMessage(user.getUserId(), ip, "user_login", "用户登录成功");

        return userLoginVO;
    }

    /**
     * 获取用户列表
     * @param page
     * @param size
     * @return
     */
    @Override
    public PageInfo<UserVO> getUser(int page, int size) {
        String ip = UserContext.getIp();
        Long userId = UserContext.getUser();
        String roleCode = UserContext.getRole();

        User user = userMapper.getUserById(userId);

        if (user == null) {
            throw new NullUserException("用户不存在");
        }

        try (Page<?> ignored = PageHelper.startPage(page, size)) {

            List<User> users = RoleAccessHelper.getAccessibleUsers(roleCode, userId, userMapper, permissionClient);

            List<UserVO> userVOList = users.stream()
                    .map(u -> BeanUtil.copyProperties(user, UserVO.class))
                    .toList();
            // 发送消息到 RabbitMQ
            constructAndSendMessage(userId, ip, "user_check", "查询用户信息");

            return new PageInfo<>(userVOList);
        }
    }

    /**
     * 获取用户信息
     * @param userId
     * @return
     */
    @Override
    public UserVO getUserById(Long userId) {

        // 当前用户上下文信息
        String ip = UserContext.getIp();
        Long currentUserId = UserContext.getUser();
        String role = UserContext.getRole();

        String targetRole = (String) permissionClient.getUserRoleCode(userId).getData();
        RoleAccessHelper.checkPermission(role, currentUserId, userId, targetRole);

        // 查询目标用户信息（只在通过权限校验后）
        User targetUser = userMapper.getUserById(userId);
        UserVO userVO = BeanUtil.copyProperties(targetUser, UserVO.class);
        if (targetUser == null) {
            throw new NullUserException("目标用户不存在");
        }

        // 日志记录只需要当前用户ID和IP，无需当前用户对象
        constructAndSendMessage(currentUserId, ip, "user_check", "查询用户 " + userId + " 信息");

        return userVO;
    }

    /**
     * 更新用户信息
     * @param userId
     * @param userUpdateDTO
     * @return
     */
    @Override
    public UserUpdateVO updateUser(Long userId, UserUpdateDTO userUpdateDTO) {
        String ip = UserContext.getIp();
        Long currentUserId = UserContext.getUser();
        String role = UserContext.getRole();

        User targetUser = userMapper.getUserById(userId);
        if (targetUser == null){
            throw new NullUserException("目标用户不存在");
        }
        String targetRole = (String) permissionClient.getUserRoleCode(userId).getData();
        RoleAccessHelper.checkPermission(role, currentUserId, userId, targetRole);

        String requestedRole = userUpdateDTO.getRole();
        if (requestedRole != null && !requestedRole.equals(targetRole)) {
            if ("admin".equals(requestedRole)) {
                permissionClient.upgradeToAdmin(userId);
            } else if ("user".equals(requestedRole)) {
                permissionClient.downgradeToUser(userId);
            }
        }

        User updateUser = new User();
        boolean needUpdate = false;

        if (userUpdateDTO.getUsername() != null) {
            updateUser.setUsername(userUpdateDTO.getUsername());
            needUpdate = true;
        }
        if (userUpdateDTO.getEmail() != null) {
            updateUser.setEmail(userUpdateDTO.getEmail());
            needUpdate = true;
        }
        if (userUpdateDTO.getPhone() != null) {
            updateUser.setPhone(userUpdateDTO.getPhone());
            needUpdate = true;
        }

        if (needUpdate) {
            userMapper.updateUser(userId, updateUser);
        }

        // 构建返回对象
        UserUpdateVO vo = new UserUpdateVO();
        vo.setUserId(userId);
        vo.setUsername(updateUser.getUsername() != null ? updateUser.getUsername() : targetUser.getUsername());
        vo.setEmail(updateUser.getEmail() != null ? updateUser.getEmail() : targetUser.getEmail());
        vo.setPhone(updateUser.getPhone() != null ? updateUser.getPhone() : targetUser.getPhone());
        vo.setGmtCreate(targetUser.getGmtCreate());

        constructAndSendMessage(currentUserId, ip, "user_update", "更新用户信息");
        return vo;
    }

    /**
     * 重置密码
     * @param userId
     * @param password
     */
    @Override
    public void resetPassword(Long userId, String password) {
        Long currentUserId = UserContext.getUser();
        String role = UserContext.getRole();
        User targetUser = userMapper.getUserById(userId);

        if (targetUser == null){
            throw new NullUserException("目标用户不存在");
        }
        String targetRole = (String) permissionClient.getUserRoleCode(userId).getData();
        RoleAccessHelper.checkPermission(role, currentUserId, userId, targetRole);

        String encryptedPassword = passwordEncoder.encode(password);
        User user = new User();
        user.setPassword(encryptedPassword);
        userMapper.updateUser(userId, user);

        constructAndSendMessage(currentUserId, UserContext.getIp(), "user_reset_password", "重置用户密码");
    }

    /**
     * 构造消息
     * @param userId
     * @param ip
     * @param action
     * @param message
     * @return
     */
    private void constructAndSendMessage(Long userId, String ip, String action, String message){
        OperationLogDTO logDTO = new OperationLogDTO();
        logDTO.setUserId(userId);
        logDTO.setAction(action);
        logDTO.setIp(ip);
        logDTO.setGmtCreate(new Timestamp(System.currentTimeMillis()));
        Map<String, Object> detail = new HashMap<>();
        detail.put("message",message);
        logDTO.setDetail(detail);

        try {
            rabbitTemplate.convertAndSend(
                    EXCHANGE,
                    USER_ROUTING_KEY,
                    logDTO
            );
        } catch (AmqpException e) {
            throw new MessageException("操作日志发送失败");
        }
    }

}
