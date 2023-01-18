package vn.vnpay.receiver.connect.kafka;

import lombok.Getter;
import lombok.Setter;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.vnpay.receiver.connect.oracle.OracleConnectionPool;

import java.util.Properties;
import java.util.concurrent.LinkedBlockingQueue;

@Getter
@Setter
public class KafkaConnectionPool {
    private static final Logger log = LoggerFactory.getLogger(OracleConnectionPool.class);

    private LinkedBlockingQueue<KafkaConnectionCell> pool = new LinkedBlockingQueue<>();

    private static KafkaConnectionPool instancePool;

    protected int numOfConnectionCreated = 0;
    protected int maxPoolSize;
    protected int initPoolSize;
    protected int minPoolSize;
    protected long timeOut = 10000;

    protected String url;

    protected Properties consumerProps;
    protected Properties producerConfig;
    protected String topic;
    protected Thread thread;
    protected long startTime;
    protected long endTime;

    public synchronized static KafkaConnectionPool getInstancePool() {
        if (instancePool == null) {
            instancePool = new KafkaConnectionPool();
            instancePool.initPoolSize = KafkaConnectionPoolConfig.INIT_POOL_SIZE;
            instancePool.maxPoolSize = KafkaConnectionPoolConfig.MAX_POOL_SIZE;
            instancePool.minPoolSize = KafkaConnectionPoolConfig.MIN_POOL_SIZE;
            instancePool.timeOut = KafkaConnectionPoolConfig.TIME_OUT;
            instancePool.thread = new Thread(() -> {
                while (true) {
                    for (KafkaConnectionCell connection : instancePool.pool) {
                        if (instancePool.numOfConnectionCreated > instancePool.minPoolSize) {
                            if (connection.isTimeOut()) {
                                try {
                                    connection.close();
                                    instancePool.pool.remove(connection);
                                    instancePool.numOfConnectionCreated--;
                                } catch (Exception e) {
                                    log.warn("Waring : Connection can not close in timeOut !");
                                }
                            }
                        }
                    }
                }
            });

            instancePool.topic = "test-topic";
            String bootstrapServers="127.0.0.1:9092";
            String grp_id="kafka";

            instancePool.consumerProps = new Properties();
            instancePool.consumerProps.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,bootstrapServers);
            instancePool.consumerProps.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,   StringDeserializer.class.getName());
            instancePool.consumerProps.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,StringDeserializer.class.getName());
            instancePool.consumerProps.setProperty(ConsumerConfig.GROUP_ID_CONFIG,grp_id);
            instancePool.consumerProps.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,"earliest");

            instancePool.producerConfig = new Properties();
            instancePool.producerConfig.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
            instancePool.producerConfig.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
            instancePool.producerConfig.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        }
        return instancePool;
    }

    public void start() {
        log.info("Create Kafka Connection pool........................ ");
        // Load Connection to Pool
        startTime = System.currentTimeMillis();
        try {
            for (int i = 0; i < initPoolSize; i++) {
                KafkaConnectionCell connection = new KafkaConnectionCell(consumerProps, producerConfig, topic, timeOut);
                pool.put(connection);
                numOfConnectionCreated++;
            }
        } catch (Exception e) {
            log.warn("[Message : can not start connection pool] - [Connection pool : {}] - " + "[Exception : {}]",
                    this.toString(), e);
        }
        thread.start();
        endTime = System.currentTimeMillis();
        log.info("Start Kafka Connection pool in : {} ms", (endTime - startTime));
    }

    public synchronized KafkaConnectionCell getConnection() {
        log.info("begin getting kafka connection!");
        KafkaConnectionCell connectionWraper = null;
        if (pool.size() == 0 && numOfConnectionCreated < maxPoolSize) {
            connectionWraper = new KafkaConnectionCell(consumerProps, producerConfig, topic, timeOut);
            try {
                pool.put(connectionWraper);
            } catch (InterruptedException e) {
                log.warn("Can not PUT Connection to Pool, Current Poll size = " + pool.size()
                        + " , Number Connection : " + numOfConnectionCreated, e);
                e.printStackTrace();
            }
            numOfConnectionCreated++;
        }

        try {
            connectionWraper = pool.take();
        } catch (InterruptedException e) {
            log.warn("Can not GET Connection from Pool, Current Poll size = " + pool.size()
                    + " , Number Connection : " + numOfConnectionCreated);
            e.printStackTrace();
        }
        connectionWraper.setRelaxTime(System.currentTimeMillis());
        log.info("finish getting rabbit connection, ");
        return connectionWraper;
    }


    public void releaseConnection(KafkaConnectionCell consumer) {
        log.info("begin releasing connection {}", consumer.toString());
        try {
            if (consumer.isClosed()) {
                pool.remove(consumer);
                KafkaConnectionCell connection = new KafkaConnectionCell(consumerProps, producerConfig, topic, timeOut);
                pool.put(connection);
            } else {
                pool.put(consumer);
            }
            log.info("successfully release connection {}", consumer.toString());
        } catch (Exception e) {
            log.error("Connection : " + consumer.toString(), e);
        }
    }
}
