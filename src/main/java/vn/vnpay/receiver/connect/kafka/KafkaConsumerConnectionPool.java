package vn.vnpay.receiver.connect.kafka;

import lombok.Getter;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.vnpay.receiver.connect.kafka.runnable.KafkaConsumerCallable;
import vn.vnpay.receiver.connect.kafka.runnable.KafkaProducerRunner;
import vn.vnpay.receiver.utils.ExecutorSingleton;

import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

@Getter
public class KafkaConsumerConnectionPool {
    private static final Logger log = LoggerFactory.getLogger(KafkaConsumerConnectionPool.class);
    private LinkedBlockingQueue<KafkaConsumerConnectionCell> pool = new LinkedBlockingQueue<>();
    private static KafkaConsumerConnectionPool instancePool;
    protected int numOfConnectionCreated = 0;
    protected int maxPoolSize;
    protected int initPoolSize;
    protected int minPoolSize;
    protected long timeOut = 10000;
    protected String url;
    protected Properties consumerProps;
    protected String consumerTopic;
    protected Thread thread;
    protected long startTime;
    protected long endTime;

    public synchronized static KafkaConsumerConnectionPool getInstancePool() {
        if (instancePool == null) {
            instancePool = new KafkaConsumerConnectionPool();
            instancePool.initPoolSize = KafkaConnectionPoolConfig.INIT_POOL_SIZE;
            instancePool.maxPoolSize = KafkaConnectionPoolConfig.MAX_POOL_SIZE;
            instancePool.minPoolSize = KafkaConnectionPoolConfig.MIN_POOL_SIZE;
            instancePool.timeOut = KafkaConnectionPoolConfig.TIME_OUT;
            instancePool.thread = new Thread(() -> {
                while (true) {
                    for (KafkaConsumerConnectionCell connection : instancePool.pool) {
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

            instancePool.consumerTopic = KafkaConnectionPoolConfig.KAFKA_CONSUMER_TOPIC;
            String bootstrapServers = KafkaConnectionPoolConfig.KAFKA_SERVER;
            String grp_id = "core-consumer";

            instancePool.consumerProps = new Properties();
            instancePool.consumerProps.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
            instancePool.consumerProps.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
            instancePool.consumerProps.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
            instancePool.consumerProps.setProperty(ConsumerConfig.GROUP_ID_CONFIG, grp_id);
            instancePool.consumerProps.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        }
        return instancePool;
    }

    public void receive() {
        ExecutorSingleton.getInstance().getExecutorService().submit(new KafkaConsumerCallable());
    }

    public void start() {
        log.info("Create Kafka Consumer Connection pool........................ ");
        // Load Connection to Pool
        startTime = System.currentTimeMillis();
        try {
            for (int i = 0; i < initPoolSize; i++) {
                int partition = numOfConnectionCreated;
                KafkaConsumerConnectionCell connection = new KafkaConsumerConnectionCell(consumerProps, consumerTopic, timeOut, partition);
                pool.put(connection);
                numOfConnectionCreated++;
            }
        } catch (Exception e) {
            log.warn("[Message : can not start connection pool] - [Connection pool : {}] - " + "[Exception : {}]",
                    this.toString(), e);
        }
//        thread.start();
        endTime = System.currentTimeMillis();
        log.info("Start  Kafka Consumer Connection pool in : {} ms", (endTime - startTime));
    }

    public synchronized KafkaConsumerConnectionCell getConnection() {
        log.info("Get kafka consumer connection..................");
        KafkaConsumerConnectionCell connectionWraper = null;
        if (pool.size() == 0 && numOfConnectionCreated < maxPoolSize) {
            int partition = numOfConnectionCreated;
            connectionWraper = new KafkaConsumerConnectionCell(consumerProps, consumerTopic, timeOut, partition);
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


    public void releaseConnection(KafkaConsumerConnectionCell consumer) {
        log.info("begin releasing connection {}", consumer.toString());
        try {
            if (consumer.isClosed()) {
                pool.remove(consumer);
                int partiton = pool.size();
                KafkaConsumerConnectionCell connection = new KafkaConsumerConnectionCell(consumerProps, consumerTopic, timeOut, partiton);
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
