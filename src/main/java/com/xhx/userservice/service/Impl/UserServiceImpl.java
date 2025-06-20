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
import exception.MessageException;
import io.seata.spring.annotation.GlobalTransactional;
import javax.annotation.Resource;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import uitls.UserContext;

import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static com.xhx.userservice.common.constant.MessageConstant.*;
import static com.xhx.userservice.common.constant.UserConstant.*;
import static com.xhx.userservice.common.constant.RedisConstant.*;
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
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private RedisTemplate<String, User> userRedisTemplate;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private RedissonClient redissonClient;


    /**
     * 用户注册
     * @param userDTO
     * @param ip
     */
    @Override
    @GlobalTransactional(name = USER_SERVICE_GROUP,  rollbackFor = Exception.class)
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
            throw new BindingException(BIND_ERROR);
        }

        // 发送消息到 RabbitMQ
        constructAndSendMessage(userId, ip, USER_REGISTER, USER_REGISTER_SUCCESS);
        throw new RuntimeException();
    }

    /**
     * 用户登录
     * @param loginDTO
     * @param ip
     * @return
     */
    @Override
    public UserLoginVO login(LoginDTO loginDTO, String ip) {
        Long userId = loginDTO.getUserId();

        String redisKey = USER_LOGIN_KEY + userId;
        User user = userRedisTemplate.opsForValue().get(redisKey);

        if (user != null && NULL.equals(user.getUsername())) {
            throw new NullUserException(USER_NOT_EXIST);
        }

        if (user == null) {
            user = getUserWithLock(redisKey, userId);
        }

        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            throw new PasswordErrorException(PASSWORD_ERROR);
        }

        String role = getRoleFromCacheOrRPC(user.getUserId());
        String token = jwtUtils.createToken(user.getUserId(), role, ip, jwtProperties.getTokenTTL());

        UserLoginVO vo = new UserLoginVO();
        vo.setUserId(user.getUserId());
        vo.setUsername(user.getUsername());
        vo.setToken(token);

        constructAndSendMessage(user.getUserId(), ip, USER_LOGIN, USER_LOGIN_SUCCESS);
        return vo;
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

        try (Page<?> ignored = PageHelper.startPage(page, size)) {

            List<User> users = RoleAccessHelper.getAccessibleUsers(roleCode, userId, userMapper, permissionClient, redisTemplate, redissonClient);

            List<UserVO> userVOList = users.stream().map(user -> {
                UserVO userVO = BeanUtil.copyProperties(user, UserVO.class);

                String role = getRoleFromCacheOrRPC(user.getUserId());
                userVO.setRole(role);

                return userVO;
            }).toList();

            constructAndSendMessage(userId, ip, USER_CHECK, USER_CHECK_SUCCESS);

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

        String redisKey = USER_LOGIN_KEY + userId;
        String targetRole = getRoleFromCacheOrRPC(userId);
        RoleAccessHelper.checkPermission(role, currentUserId, userId, targetRole);

        // 查询目标用户信息（只在通过权限校验后）
        User targetUser = userRedisTemplate.opsForValue().get(redisKey);

        if (targetUser == null) {
            targetUser = getUserWithLock(redisKey, userId);
        } else if (NULL.equals(targetUser.getUsername())) {
            throw new NullUserException(USER_NOT_EXIST);
        }

        UserVO userVO = BeanUtil.copyProperties(targetUser, UserVO.class);
        userVO.setRole(targetRole);
        constructAndSendMessage(currentUserId, ip, USER_CHECK, USER_CHECK_SUCCESS + userId);

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

        String redisKey = USER_LOGIN_KEY + userId;
        User targetUser = userRedisTemplate.opsForValue().get(redisKey);

        if (targetUser == null) {
            targetUser = getUserWithLock(redisKey, userId);
        } else if (NULL.equals(targetUser.getUsername())) {
            throw new NullUserException(USER_NOT_EXIST);
        }

        String targetRole = getRoleFromCacheOrRPC(userId);
        RoleAccessHelper.checkPermission(role, currentUserId, userId, targetRole);

        String requestedRole = userUpdateDTO.getRole();
        if (requestedRole != null && !requestedRole.equals(targetRole)) {
            if (USER_ROLE_ADMIN.equals(requestedRole)) {
                permissionClient.upgradeToAdmin(userId);
            } else if (USER_ROLE_USER.equals(requestedRole)) {
                permissionClient.downgradeToUser(userId);
            }
        }

        User updateUser = new User();
        boolean needUpdate = false;

        List<Map<String, String>> changeLogs = new ArrayList<>();

        if (userUpdateDTO.getUsername() != null && !userUpdateDTO.getUsername().equals(targetUser.getUsername())) {
            updateUser.setUsername(userUpdateDTO.getUsername());
            changeLogs.add(Map.of(
                    "field", "username",
                    "old", targetUser.getUsername(),
                    "new", userUpdateDTO.getUsername()
            ));
            needUpdate = true;
        }
        if (userUpdateDTO.getEmail() != null && !userUpdateDTO.getEmail().equals(targetUser.getEmail())) {
            updateUser.setEmail(userUpdateDTO.getEmail());
            changeLogs.add(Map.of(
                    "field", "email",
                    "old", String.valueOf(targetUser.getEmail()),
                    "new", String.valueOf(userUpdateDTO.getEmail())
            ));
            needUpdate = true;
        }
        if (userUpdateDTO.getPhone() != null && !userUpdateDTO.getPhone().equals(targetUser.getPhone())) {
            updateUser.setPhone(userUpdateDTO.getPhone());
            changeLogs.add(Map.of(
                    "field", "phone",
                    "old", String.valueOf(targetUser.getPhone()),
                    "new", String.valueOf(userUpdateDTO.getPhone())
            ));
            needUpdate = true;
        }

        if (needUpdate) {
            userMapper.updateUser(userId, updateUser);

            redisTemplate.delete(redisKey);
            CompletableFuture.runAsync(() -> {
                try {
                    Thread.sleep(200);
                    redisTemplate.delete(redisKey);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        UserUpdateVO vo = new UserUpdateVO();
        vo.setUserId(userId);
        vo.setUsername(updateUser.getUsername() != null ? updateUser.getUsername() : targetUser.getUsername());
        vo.setEmail(updateUser.getEmail() != null ? updateUser.getEmail() : targetUser.getEmail());
        vo.setPhone(updateUser.getPhone() != null ? updateUser.getPhone() : targetUser.getPhone());
        vo.setGmtCreate(targetUser.getGmtCreate());

        Map<String, Object> extraDetail = new HashMap<>();
        extraDetail.put("changes", changeLogs);

        constructAndSendMessage(currentUserId, ip, USER_UPDATE, USER_UPDATE_SUCCESS, extraDetail);

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

        String redisKey = USER_LOGIN_KEY + userId;
        User targetUser = userRedisTemplate.opsForValue().get(redisKey);

        if (targetUser == null) {
            targetUser = getUserWithLock(redisKey, userId);
        } else if (NULL.equals(targetUser.getUsername())) {
            throw new NullUserException(USER_NOT_EXIST);
        }

        String targetRole = getRoleFromCacheOrRPC(userId);
        RoleAccessHelper.checkPermission(role, currentUserId, userId, targetRole);

        String encryptedPassword = passwordEncoder.encode(password);
        User user = new User();
        user.setPassword(encryptedPassword);
        userMapper.updateUser(userId, user);

        redisTemplate.delete(redisKey);

        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(200);
                redisTemplate.delete(redisKey);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        constructAndSendMessage(currentUserId, UserContext.getIp(), USER_RESET_PASSWORD, USER_RESET_PASSWORD_SUCCESS);
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
        detail.put(MESSAGE,message);
        logDTO.setDetail(detail);

        try {
            rabbitTemplate.convertAndSend(
                    EXCHANGE,
                    USER_ROUTING_KEY,
                    logDTO
            );
        } catch (AmqpException e) {
            throw new MessageException(OPERATION_LOG_SEND_FAILURE);
        }
    }
    private void constructAndSendMessage(Long userId, String ip, String action, String message, Map<String, Object> extraDetail) {
        OperationLogDTO logDTO = new OperationLogDTO();
        logDTO.setUserId(userId);
        logDTO.setAction(action);
        logDTO.setIp(ip);
        logDTO.setGmtCreate(new Timestamp(System.currentTimeMillis()));

        Map<String, Object> detail = new HashMap<>();
        detail.put(MESSAGE, message);
        if (extraDetail != null) {
            detail.putAll(extraDetail);
        }
        logDTO.setDetail(detail);

        try {
            rabbitTemplate.convertAndSend(EXCHANGE, USER_ROUTING_KEY, logDTO);
        } catch (AmqpException e) {
            throw new MessageException(OPERATION_LOG_SEND_FAILURE);
        }
    }


    private User getUserWithLock(String redisKey, Long userId) {
        RLock lock = redissonClient.getLock(LOCK_USER + userId);
        boolean locked = false;

        try {
            locked = lock.tryLock(LOCK_WAIT_TIME_SECONDS, LOCK_LEASE_TIME_SECONDS, TimeUnit.SECONDS);
            if (!locked) {
                Thread.sleep(THREAD_SLEEP_TIME_MILLIS);
                return getUserWithLock(redisKey, userId);
            }

            // 再查一次缓存，防止击穿
            User user = userRedisTemplate.opsForValue().get(redisKey);
            if (user != null) {
                if (NULL.equals(user.getUsername())) {
                    throw new NullUserException(USER_NOT_EXIST);
                }
                return user;
            }

            user = userMapper.getUserById(userId);
            if (user == null) {
                User emptyUser = new User();
                emptyUser.setUsername(NULL);
                userRedisTemplate.opsForValue().set(redisKey, emptyUser, CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
                throw new NullUserException(USER_NOT_EXIST);
            }

            int ttl = TTL_BASE_SECONDS + new Random().nextInt(TTL_RANDOM_BOUND_SECONDS);
            userRedisTemplate.opsForValue().set(redisKey, user, ttl, TimeUnit.MINUTES);
            return user;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(THREAD_INTERRUPTED);
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private String getRoleFromCacheOrRPC(Long userId) {
        String cacheKey = USER_ROLE_KEY + userId;

        // 先查缓存
        String role = stringRedisTemplate.opsForValue().get(cacheKey);
        if (role != null) {
            // 命中缓存
            if (NULL.equals(role)) {
                // 缓存的是空角色
                throw new NullUserException(USER_ROLE_NOT_EXIST);
            }
            return role;
        }

        // 缓存未命中，尝试加锁防止击穿
        RLock lock = redissonClient.getLock(LOCK_USER_ROLE + userId);
        boolean locked = false;
        try {
            locked = lock.tryLock(LOCK_WAIT_TIME_SECONDS, LOCK_LEASE_TIME_SECONDS, TimeUnit.SECONDS);
            if (locked) {
                // 再次检查缓存，防止缓存击穿
                role = stringRedisTemplate.opsForValue().get(cacheKey);
                if (role != null) {
                    if (NULL.equals(role)) {
                        throw new NullUserException(USER_ROLE_NOT_EXIST);
                    }
                    return role;
                }

                // 调用权限服务获取角色
                role = (String) permissionClient.getUserRoleCode(userId).getData();

                if (role == null) {
                    // 防止缓存穿透，缓存空角色
                    stringRedisTemplate.opsForValue().set(cacheKey, NULL, USER_ROLE_NULL_TTL_MINUTES, TimeUnit.MINUTES);
                    throw new NullUserException(USER_ROLE_NOT_EXIST);
                }

                // 缓存角色，防止雪崩加随机 TTL
                int ttl = USER_ROLE_TTL_BASE_SECONDS + new Random().nextInt(USER_ROLE_TTL_RANDOM_SECONDS);
                stringRedisTemplate.opsForValue().set(cacheKey, role, ttl, TimeUnit.MINUTES);
            } else {
                // 没抢到锁，稍等后重试
                Thread.sleep(THREAD_SLEEP_TIME_MILLIS);
                return getRoleFromCacheOrRPC(userId);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(THREAD_INTERRUPTED);
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }

        return role;
    }

}
