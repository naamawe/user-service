package com.xhx.userservice.mapper;

import com.xhx.userservice.entiey.pojo.User;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * @author master
 */
@Mapper
public interface UserMapper {

    void insertUser(User user);

    User findByUsername(String username);

    User getUserById(Long userId);

//    List<User> getUserById(Long userId);

    List<User> getAllUser();

    List<User> getUsersByUserIds(List<Long> userIds);
}
