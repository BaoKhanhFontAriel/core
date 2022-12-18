package vn.vnpay.connect.rabbit;

import vn.vnpay.utils.AppConfigSingleton;

public class RabbitConnectionPoolConfig {

    public static final int MAX_POOL_SIZE = AppConfigSingleton.getInstance().getIntProperty("rabbitmq.max_pool_size");
    public static final int MIN_POOL_SIZE = AppConfigSingleton.getInstance().getIntProperty("rabbitmq.min_pool_size");;
    public static final int INIT_POOL_SIZE = AppConfigSingleton.getInstance().getIntProperty("rabbitmq.init_pool_size");
    public static final String QUEUE_NAME = AppConfigSingleton.getInstance().getStringProperty("rabbitmq.queue_name");;
    public static final String EXCHANGE_NAME = AppConfigSingleton.getInstance().getStringProperty("rabbitmq.exchange_name");;
    public static final String EXCHANGE_TYPE =AppConfigSingleton.getInstance().getStringProperty("rabbitmq.exchange_type");;
    public static final String ROUTING_KEY = AppConfigSingleton.getInstance().getStringProperty("rabbitmq.routing_key");;
    public static final String URL = AppConfigSingleton.getInstance().getStringProperty("rabbitmq.url");
    public static final long TIME_OUT = AppConfigSingleton.getInstance().getIntProperty("rabbitmq.time_out");
}
