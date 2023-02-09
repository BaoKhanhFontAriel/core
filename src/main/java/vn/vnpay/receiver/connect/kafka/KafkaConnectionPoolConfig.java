package vn.vnpay.receiver.connect.kafka;

import vn.vnpay.receiver.utils.AppConfigSingleton;

public class KafkaConnectionPoolConfig {
    public static final int MAX_POOL_SIZE = AppConfigSingleton.getInstance().getIntProperty("kafka.max_pool_size");;
    public static final int MIN_POOL_SIZE = AppConfigSingleton.getInstance().getIntProperty("kafka.min_pool_size");;
    public static final int INIT_POOL_SIZE = AppConfigSingleton.getInstance().getIntProperty("kafka.init_pool_size");
    public static final long TIME_OUT = AppConfigSingleton.getInstance().getIntProperty("kafka.timeout");;
    public static final int REQUEST_TIME_SLEEP = AppConfigSingleton.getInstance().getIntProperty("time.sleep");
    public static final int REQUEST_TIME_OUT = AppConfigSingleton.getInstance().getIntProperty("time.out");
    public static final String KAFKA_CONSUMER_TOPIC = AppConfigSingleton.getInstance().getStringProperty("kafka.topic.consumer");
    public static final String KAFKA_CONSUMER_GROUP_ID = AppConfigSingleton.getInstance().getStringProperty("kafka.consumer.group_id");
    public static final String KAFKA_PRODUCER_TOPIC = AppConfigSingleton.getInstance().getStringProperty("kafka.topic.producer");
    public static final String KAFKA_SERVER = AppConfigSingleton.getInstance().getStringProperty("kafka.server");
}
