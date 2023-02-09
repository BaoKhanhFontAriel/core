package vn.vnpay.receiver.connect.kafka;

import lombok.Getter;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.concurrent.LinkedBlockingQueue;



@Getter
public class KafkaProducerConnectionPool {
    private static final Logger log = LoggerFactory.getLogger(KafkaProducerConnectionPool.class);
    private LinkedBlockingQueue<KafkaProducerConnectionCell> pool = new LinkedBlockingQueue<>();
    private static KafkaProducerConnectionPool instancePool;
    protected int numOfConnectionCreated = 0;
    protected int maxPoolSize;
    protected int initPoolSize;
    protected int minPoolSize;
    protected long timeOut = 10000;
    protected Properties producerProps;
    protected String producerTopic;
    protected Thread thread;
    protected long startTime;
    protected long endTime;

    public synchronized static KafkaProducerConnectionPool getInstancePool() {
        if (instancePool == null) {
            instancePool = new KafkaProducerConnectionPool();
            instancePool.initPoolSize = KafkaConnectionPoolConfig.INIT_PRODUCER_POOL_SIZE;
            instancePool.maxPoolSize = KafkaConnectionPoolConfig.MIN_PRODUCER_POOL_SIZE;
            instancePool.minPoolSize = KafkaConnectionPoolConfig.MIN_PRODUCER_POOL_SIZE;
            instancePool.timeOut = KafkaConnectionPoolConfig.TIME_OUT;
            instancePool.thread = new Thread(() -> {
                while (true) {
                    for (KafkaProducerConnectionCell connection : instancePool.pool) {
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

            instancePool.producerTopic = KafkaConnectionPoolConfig.KAFKA_PRODUCER_TOPIC;
            String bootstrapServers = KafkaConnectionPoolConfig.KAFKA_SERVER;

            instancePool.producerProps = new Properties();
            instancePool.producerProps.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
            instancePool.producerProps.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
            instancePool.producerProps.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        }
        return instancePool;
    }

    public void start() {
        log.info("Create Kafka Producer Connection pool........................ ");
        // Load Connection to Pool
        startTime = System.currentTimeMillis();
        try {
            for (int i = 0; i < initPoolSize; i++) {
                KafkaProducerConnectionCell connection = new KafkaProducerConnectionCell(producerProps, producerTopic, timeOut);
                pool.put(connection);
                numOfConnectionCreated++;
            }
        } catch (Exception e) {
            log.warn("[Message : can not start connection pool] - [Connection pool : {}] - " + "[Exception : {}]",
                    this.toString(), e);
        }
//        thread.start();
        endTime = System.currentTimeMillis();
        log.info("Start Kafka Producer Connection pool in : {} ms", (endTime - startTime));
    }

    public synchronized KafkaProducerConnectionCell getConnection() {
        log.info("Get kafka production connection.............");
        KafkaProducerConnectionCell connectionWraper = null;
        if (pool.size() == 0 && numOfConnectionCreated < maxPoolSize) {
            connectionWraper = new KafkaProducerConnectionCell(producerProps, producerTopic, timeOut);
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
        return connectionWraper;
    }


    public void releaseConnection(KafkaProducerConnectionCell consumer) {
        log.info("begin releasing connection {}", consumer.toString());
        try {
            if (consumer.isClosed()) {
                pool.remove(consumer);
                KafkaProducerConnectionCell connection = new KafkaProducerConnectionCell(producerProps, producerTopic, timeOut);
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
