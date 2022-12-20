package vn.vnpay.receiver.connect.redis;

import vn.vnpay.receiver.utils.AppConfigSingleton;

public class RedisConnectionPoolConfig {
    public static final int MAX_POOL_SIZE = AppConfigSingleton.getInstance().getIntProperty("redis.max_pool_size");;
    public static final int MIN_POOL_SIZE = AppConfigSingleton.getInstance().getIntProperty("redis.min_pool_size");;
    public static final int INIT_POOL_SIZE = AppConfigSingleton.getInstance().getIntProperty("redis.init_pool_size");;
    public static final String URL =  AppConfigSingleton.getInstance().getStringProperty("redis.url");;
    public static final long TIME_OUT = AppConfigSingleton.getInstance().getIntProperty("redis.time_out");;
}
