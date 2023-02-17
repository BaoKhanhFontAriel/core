package vn.vnpay.receiver.connect.kafka;

import lombok.Getter;
import org.apache.kafka.clients.consumer.CommitFailedException;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.vnpay.receiver.utils.ExecutorSingleton;
import vn.vnpay.receiver.utils.KafkaUtils;

import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Getter
public class KafkaConsumerPool extends ObjectPool<KafkaConsumerCell>{
    private static final Logger log = LoggerFactory.getLogger(KafkaConsumerPool.class);
    private static KafkaConsumerPool instancePool;
    protected Properties consumerProps;
    protected String consumerTopic;
    private final int maxPoolSize;
    private static AtomicReference<LinkedBlockingQueue<String>> recordQueue = new AtomicReference<>(new LinkedBlockingQueue<>());
    private int index;
    private static volatile boolean isReceived ;


    public synchronized static KafkaConsumerPool getInstancePool() {
        if (instancePool == null) {
            instancePool = new KafkaConsumerPool();
        }
        return instancePool;
    }

    public KafkaConsumerPool() {
        setExpirationTime(Integer.MAX_VALUE);
        setInitSize(KafkaPoolConfig.INIT_CONSUMER_POOL_SIZE);
        index = 0;
        maxPoolSize = KafkaPoolConfig.INIT_CONSUMER_POOL_SIZE;
        consumerTopic = KafkaPoolConfig.KAFKA_CONSUMER_TOPIC;
        consumerProps = new Properties();
        consumerProps.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaPoolConfig.KAFKA_SERVER);
        consumerProps.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.setProperty(ConsumerConfig.GROUP_ID_CONFIG, KafkaPoolConfig.KAFKA_CONSUMER_GROUP_ID);
        consumerProps.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
    }

    public void startPoolPolling() {
        log.info("Start Kafka consumer pool polling.........");
        while (getIdle() > 0) {
            KafkaConsumerCell consumerCell = getConnection();
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
                        if (isReceived){
                            log.info("add to record queue");
                            recordQueue.get().add(r.value());
                        }                    }
                    try {
                        consumerCell.getConsumer().commitSync();
                    } catch (CommitFailedException e) {
                        log.error("commit failed", e) ;
                    }
                }//
            });
        }
    }

    public KafkaConsumerCell getConnection(){
        return super.checkOut();
    }

    public static synchronized String getRecord() throws Exception {
        log.info("Get Kafka Consumer pool record size = {}.......", recordQueue.get().size());
        isReceived = true;
        String record = recordQueue.get().take();
        isReceived = false;
        return record;
    }

    @Override
    protected KafkaConsumerCell create() {
        int temp = index;
        index++;
        return new KafkaConsumerCell(consumerProps, consumerTopic, temp);
    }

    @Override
    public boolean validate(KafkaConsumerCell o) {
        return (!o.isClosed());
    }

    @Override
    public void expire(KafkaConsumerCell o) {
        o.close();
    }
}
