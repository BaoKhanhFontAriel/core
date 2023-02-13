package vn.vnpay.receiver.connect.kafka;

import lombok.Getter;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.vnpay.receiver.error.ErrorCode;
import vn.vnpay.receiver.model.ApiResponse;
import vn.vnpay.receiver.utils.ExecutorSingleton;
import vn.vnpay.receiver.utils.GsonSingleton;

import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

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
    private static AtomicReference<String> res;
    private static CountDownLatch latch;

    public synchronized static KafkaConsumerConnectionPool getInstancePool() {
        if (instancePool == null) {
            instancePool = new KafkaConsumerConnectionPool();
            instancePool.initPoolSize = KafkaConnectionPoolConfig.INIT_CONSUMER_POOL_SIZE;
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
            String grp_id = KafkaConnectionPoolConfig.KAFKA_CONSUMER_GROUP_ID;

            instancePool.consumerProps = new Properties();
            instancePool.consumerProps.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
            instancePool.consumerProps.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
            instancePool.consumerProps.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
            instancePool.consumerProps.setProperty(ConsumerConfig.GROUP_ID_CONFIG, grp_id);
            instancePool.consumerProps.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        }
        return instancePool;
    }


    public void start() {
        log.info("Create Kafka Consumer Connection pool........................ ");
        // Load Connection to Pool
        startTime = System.currentTimeMillis();
        try {
            for (int i = 0; i < initPoolSize; i++) {
                KafkaConsumerConnectionCell connection = new KafkaConsumerConnectionCell(consumerProps, consumerTopic, timeOut, i);
                pool.put(connection);
                numOfConnectionCreated++;
            }
        } catch (Exception e) {
            log.warn("[Message : can not start connection pool] - [Connection pool : {}] - " + "[Exception : {}]",
                    this.toString(), e);
        }
//        thread.start();

        for (KafkaConsumerConnectionCell c: pool
             ) {
            c.getConsumer().poll(Duration.ofMillis(100));
        }

        endTime = System.currentTimeMillis();
        log.info("consumer pool size {}", pool.size() );
        log.info("Start  Kafka Consumer Connection pool in : {} ms", (endTime - startTime));
    }

    public static void startPoolPolling() {
        log.info("Start Kafka consumer pool polling.........");
        for (KafkaConsumerConnectionCell consumerCell : instancePool.pool) {
            log.info("consumer {} start polling", consumerCell.getConsumer().groupMetadata().groupInstanceId());
            ExecutorSingleton.submit((Runnable) () ->
            {
                while (true) {
                    ConsumerRecords<String, String> records = consumerCell.poll(Duration.ofMillis(100));
                    for (ConsumerRecord<String, String> r : records) {
                        log.info("----");
                        log.info("kafka consumer id {} receive data: partition = {}, offset = {}, key = {}, value = {}",
                                consumerCell.getConsumer().groupMetadata().groupInstanceId(),
                                r.partition(),
                                r.offset(), r.key(), r.value());
                        res.set(r.value());
                        latch.countDown();
                        log.info("latch count after set res {}", latch.getCount());
                    }
                }//
            });
        }
    }

    public static String getRecord() throws Exception {
        log.info("Get Kafka Consumer pool record.......");
        latch = new CountDownLatch(1);
        res = new AtomicReference<>();

        try {
            latch.await(2000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            log.info("Kafka consumes is time out", e);
            throw new Exception("Kafka consumes is time out");
        }

        return res.get();
    }
}
