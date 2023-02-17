package vn.vnpay.receiver.connect.kafka;

import lombok.Getter;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;


@Getter
public class KafkaProducerPool extends ObjectPool<KafkaProducerCell> {
    private static final Logger log = LoggerFactory.getLogger(KafkaProducerPool.class);
    private static KafkaProducerPool instancePool;
    protected Properties producerProps;
    protected String producerTopic;
    protected long startTime;
    protected long endTime;
    public synchronized static KafkaProducerPool getInstancePool() {
        log.info("Create Kafka Producer Connection pool........................ ");
        if (instancePool == null) {
            instancePool = new KafkaProducerPool();
        }
        return instancePool;
    }

    public KafkaProducerPool() {
        setInitSize(KafkaPoolConfig.INIT_PRODUCER_POOL_SIZE);
        setExpirationTime(KafkaPoolConfig.TIME_OUT);
        producerTopic = KafkaPoolConfig.KAFKA_PRODUCER_TOPIC;
        producerProps = new Properties();
        producerProps.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaPoolConfig.KAFKA_SERVER);
        producerProps.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
    }

    public synchronized KafkaProducerCell getConnection() {
        log.info("Get kafka production connection.............");
        return super.checkOut();
    }


    public void releaseConnection(KafkaProducerCell consumer) {
        log.info("begin releasing connection {}", consumer.toString());
        super.checkIn(consumer);
    }

    @Override
    protected KafkaProducerCell create() {
        return (new KafkaProducerCell(producerProps, producerTopic));
    }

    @Override
    public boolean validate(KafkaProducerCell o) {
        return (!o.isClosed());
    }

    @Override
    public void expire(KafkaProducerCell o) {
        o.close();
    }
}
