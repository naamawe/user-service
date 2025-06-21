package com.xhx.userservice.common.constant;

/**
 * @author master
 */
public class RedisConstant {
    public static final String NULL = "null";

    public static final String USER_LOGIN_KEY = "user:login:";
    public static final String USER_ROLE_KEY = "user:role:";

    public static final String LOCK_USER = "lock:user:";
    public static final String LOCK_USER_ROLE = "lock:user:role:";

    public static final long CACHE_EXPIRE_MINUTES = 2;
    public static final int TTL_BASE_SECONDS = 6 * 60;
    public static final long LOCK_WAIT_TIME_SECONDS = 5;
    public static final long LOCK_LEASE_TIME_SECONDS = 10;
    public static final int TTL_RANDOM_BOUND_SECONDS = 60;
    public static final int THREAD_SLEEP_TIME_MILLIS = 50;

    public static final long USER_ROLE_NULL_TTL_MINUTES = 2;
    public static final int USER_ROLE_TTL_RANDOM_SECONDS = 60;
    public static final int USER_ROLE_TTL_BASE_SECONDS = 6 * 60;

    public static final String USER_ROLE_NOT_EXIST = "用户角色不存在";

}
