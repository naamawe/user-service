package com.xhx.userservice.service;

import cn.hutool.core.bean.BeanUtil;
import com.xhx.userservice.client.PermissionClient;
import com.xhx.userservice.common.exception.BindingException;
import com.xhx.userservice.common.exception.MessageException;
import com.xhx.userservice.common.exception.NullUserException;
import com.xhx.userservice.common.exception.PasswordErrorException;
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
import com.xhx.userservice.mapper.UserMapper;
import io.seata.spring.annotation.GlobalTransactional;
import javax.annotation.Resource;

import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author master
 */
@Service
public class UserServiceImpl implements UserService{

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

        OperationLogDTO logDTO = ConstructMessage(user, ip, "user_register", "用户注册成功");

        // 发送消息到 RabbitMQ
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

    @Override
    public UserLoginVO login(LoginDTO loginDTO, String ip) {
        User user = userMapper.findByUsername(loginDTO.getUsername());

        if (user == null){
            throw new NullUserException("用户不存在");
        }
        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())){
            throw new PasswordErrorException("密码错误");
        }

        String token = jwtUtils.createToken(user.getUserId(), jwtProperties.getTokenTTL());

        UserLoginVO userLoginVO = new UserLoginVO();
        userLoginVO.setUserId(user.getUserId());
        userLoginVO.setUsername(user.getUsername());
        userLoginVO.setToken(token);

        OperationLogDTO logDTO = ConstructMessage(user, ip, "user_login", "用户登录成功");

        // 发送消息到 RabbitMQ
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE,
                    RabbitMQConfig.ROUTING_KEY,
                    logDTO
            );
        } catch (AmqpException e) {
            throw new MessageException("操作日志发送失败");
        }
        return userLoginVO;
    }

    @Override
    public List<User> getUser() {
        return List.of();
    }

    @Override
    public User getUserById(Long userid) {
        return null;
    }

    @Override
    public User updateUser(Long userId) {
        return null;
    }

    @Override
    public void resetPassword() {

    }

    /**
     * 构造消息
     * @param user
     * @param ip
     * @param action
     * @param message
     * @return
     */
    private OperationLogDTO ConstructMessage(User user, String ip, String action, String message){
        OperationLogDTO logDTO = new OperationLogDTO();
        logDTO.setUserId(user.getUserId());
        logDTO.setAction(action);
        logDTO.setIp(ip);
        Map<String, Object> detail = new HashMap<>();
        detail.put("username", user.getUsername());

        String detailJson = LogDetailUtils.buildDetailJson(message, detail);
        logDTO.setDetail(detailJson);
        return logDTO;
    }
}
