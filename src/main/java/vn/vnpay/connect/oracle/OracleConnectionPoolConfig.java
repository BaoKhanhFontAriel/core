package vn.vnpay.connect.oracle;

import vn.vnpay.utils.AppConfigSingleton;

public class OracleConnectionPoolConfig {
    public static final int MAX_POOL_SIZE = AppConfigSingleton.getInstance().getIntProperty("oracle.max_pool_size");;
    public static final int MIN_POOL_SIZE = AppConfigSingleton.getInstance().getIntProperty("oracle.min_pool_size");;
    public static final int INIT_POOL_SIZE = AppConfigSingleton.getInstance().getIntProperty("oracle.init_pool_size");
    public static final String USERNAME = AppConfigSingleton.getInstance().getStringProperty("oracle.username");
    public static final String PASSWORD = AppConfigSingleton.getInstance().getStringProperty("oracle.password");
    public static final String URL = AppConfigSingleton.getInstance().getStringProperty("oracle.url");
    public static final long TIME_OUT = AppConfigSingleton.getInstance().getIntProperty("oracle.timeout");;
}
