package vn.vnpay.receiver.main;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import vn.vnpay.receiver.connect.kafka.KafkaConnectionCell;
import vn.vnpay.receiver.connect.kafka.KafkaConnectionPool;
import vn.vnpay.receiver.connect.oracle.OracleConnectionPool;
import vn.vnpay.receiver.connect.rabbit.RabbitConnectionCell;
import vn.vnpay.receiver.connect.rabbit.RabbitConnectionPool;
import vn.vnpay.receiver.connect.redis.RedisConnectionPool;
import vn.vnpay.receiver.thread.ShutdownThread;
import vn.vnpay.receiver.utils.ExecutorSingleton;
import vn.vnpay.receiver.utils.GsonSingleton;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.*;


@Slf4j
public class MainService {
    private static final RabbitConnectionPool rabbitConnectionPool = RabbitConnectionPool.getInstancePool();
    private static final OracleConnectionPool oracleConnectionPool = OracleConnectionPool.getInstancePool();
    private static final RedisConnectionPool redisConnectionPool = RedisConnectionPool.getInstancePool();
    private static final KafkaConnectionPool kafkaConnectionPool = KafkaConnectionPool.getInstancePool();
    private static final ExecutorSingleton executorSingleton = new ExecutorSingleton();
    private static final GsonSingleton gsonSingleton = new GsonSingleton();

    public static void main(String[] args) throws IOException, TimeoutException {
        oracleConnectionPool.start();
        rabbitConnectionPool.start();
        redisConnectionPool.start();
//        kafkaConnectionPool.start();

//        RabbitConnectionCell rabbitConnectionCell = rabbitConnectionPool.getConnection();
//        rabbitConnectionCell.receiveAndSend();
//        KafkaConnectionCell kafkaConnectionCell = kafkaConnectionPool.getConnection();
//        kafkaConnectionCell.receiveAndSend();

        String topic = "test-topic";
        String bootstrapServers="127.0.0.1:9092";
        String grp_id="kafka";

        Properties consumerProps = new Properties();
        consumerProps.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,bootstrapServers);
        consumerProps.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,   StringDeserializer.class.getName());
        consumerProps.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,StringDeserializer.class.getName());
        consumerProps.setProperty(ConsumerConfig.GROUP_ID_CONFIG,grp_id);
        consumerProps.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,"earliest");

        Properties producerConfig = new Properties();
        producerConfig.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        producerConfig.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerConfig.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        
        KafkaConnectionCell kafkaConnectionCell = new KafkaConnectionCell(consumerProps, producerConfig, topic, 1000);
        kafkaConnectionCell.receiveAndSend();

        Runtime.getRuntime().addShutdownHook(new ShutdownThread());
    }
}
