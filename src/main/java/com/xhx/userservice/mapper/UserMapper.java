package com.xhx.userservice.mapper;

import com.xhx.userservice.entiey.pojo.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author master
 */
@Mapper
public interface UserMapper {

    void insertUser(User user);

    User findByUsername(String username);
}
