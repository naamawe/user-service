package com.xhx.userservice.service.Impl;

import cn.hutool.core.bean.BeanUtil;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.xhx.userservice.client.PermissionClient;
import com.xhx.userservice.common.exception.*;
import com.xhx.userservice.common.util.JwtUtils;
import com.xhx.userservice.common.util.LogDetailUtils;
import com.xhx.userservice.common.util.SnowflakeIdWorker;
import com.xhx.userservice.config.JwtProperties;
import com.xhx.userservice.config.RabbitMQConfig;
import com.xhx.userservice.entiey.dto.LoginDTO;
import com.xhx.userservice.entiey.dto.UserDTO;
import com.xhx.userservice.entiey.dto.OperationLogDTO;
import com.xhx.userservice.entiey.pojo.User;
import com.xhx.userservice.entiey.vo.UserLoginVO;
import com.xhx.userservice.entiey.vo.UserVO;
import com.xhx.userservice.mapper.UserMapper;
import com.xhx.userservice.service.UserService;
import io.seata.spring.annotation.GlobalTransactional;
import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import uitls.UserContext;

import java.sql.Timestamp;
import java.util.*;

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
        constructAndSendMessage(user, ip, "user_register", "用户注册成功");
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
        String token = jwtUtils.createToken(user.getUserId(), role, jwtProperties.getTokenTTL());

        UserLoginVO userLoginVO = new UserLoginVO();
        userLoginVO.setUserId(user.getUserId());
        userLoginVO.setUsername(user.getUsername());
        userLoginVO.setToken(token);

        // 发送消息到 RabbitMQ
        constructAndSendMessage(user, ip, "user_login", "用户登录成功");

        return userLoginVO;
    }

    /**
     * 获取用户列表
     * @param page
     * @param size
     * @param ip
     * @return
     */
    @Override
    public PageInfo<UserVO> getUser(int page, int size, String ip) {
        Long userId = UserContext.getUser();
        String roleCode = UserContext.getRole();

        User user = userMapper.getUserById(userId);

        if (user == null) {
            throw new NullUserException("用户不存在");
        }

        try (Page<?> ignored = PageHelper.startPage(page, size)) {
            List<User> users;

            switch (roleCode) {
                case "user":
                    User user1 = userMapper.getUserById(userId);
                    users = List.of(user1);
                    break;
                case "admin":
                    List<Long> userIds = (List<Long>) permissionClient.getUserIdsByRoleCode(2).getData();

                    if (userIds == null || userIds.isEmpty()) {
                        users = List.of();
                    } else {
                        users = userMapper.getUsersByUserIds(userIds);
                    }
                    break;
                case "super_admin":
                    users = userMapper.getAllUser();
                    break;
                default:
                    users = List.of();
            }
            List<UserVO> userVOList = users.stream()
                    .map(u -> BeanUtil.copyProperties(user, UserVO.class))
                    .toList();
            // 发送消息到 RabbitMQ
            constructAndSendMessage(user, ip, "user_check", "查询用户信息");

            return new PageInfo<>(userVOList);
        }
    }

    /**
     * 获取用户信息
     * @param userId
     * @param ip
     * @return
     */
    @Override
    public UserVO getUserById(Long userId, String ip) {
        // 当前用户上下文信息
        Long currentUserId = UserContext.getUser();
        String role = UserContext.getRole();

        if ("user".equals(role)) {
            if (!Objects.equals(currentUserId, userId)) {
                throw new AccessDeniedException("普通用户无权限访问其他用户信息");
            }
        } else if ("admin".equals(role)) {
            String targetRole = (String) permissionClient.getUserRoleCode(userId).getData();
            if ("super_admin".equals(targetRole)) {
                throw new AccessDeniedException("管理员无权限访问超管信息");
            }
        } else if (!"super_admin".equals(role)) {
            throw new AccessDeniedException("未知角色，拒绝访问");
        }

        // 查询目标用户信息（只在通过权限校验后）
        User targetUser = userMapper.getUserById(userId);
        UserVO userVO = BeanUtil.copyProperties(targetUser, UserVO.class);
        if (targetUser == null) {
            throw new NullUserException("目标用户不存在");
        }

        // 日志记录只需要当前用户ID和IP，无需当前用户对象
        constructAndSendMessage(targetUser, ip, "user_check", "查询用户 " + userId + " 信息");

        return userVO;
    }

    /**
     * 更新用户信息
     * @param userId
     * @param userDTO
     * @param ip
     * @return
     */
    @Override
    public UserVO updateUser(Long userId, UserDTO userDTO, String ip) {
        return null;
    }

    /**
     * 重置密码
     * @param userId
     * @param password
     * @param ip
     */
    @Override
    public void resetPassword(Long userId, String password, String ip) {

    }

    /**
     * 构造消息
     * @param user
     * @param ip
     * @param action
     * @param message
     * @return
     */
    private void constructAndSendMessage(User user, String ip, String action, String message){
        OperationLogDTO logDTO = new OperationLogDTO();
        logDTO.setUserId(user.getUserId());
        logDTO.setAction(action);
        logDTO.setIp(ip);
        Map<String, Object> detail = new HashMap<>();
        detail.put("username", user.getUsername());

        String detailJson = LogDetailUtils.buildDetailJson(message, detail);
        logDTO.setDetail(detailJson);

        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE,
                    RabbitMQConfig.ROUTING_KEY,
                    logDTO
            );
        } catch (AmqpException e) {
            throw new MessageException("操作日志发送失败");
        }
    }
}
