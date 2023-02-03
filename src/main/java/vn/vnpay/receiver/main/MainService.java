package vn.vnpay.receiver.main;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import vn.vnpay.receiver.connect.kafka.*;
import vn.vnpay.receiver.connect.kafka.runnable.KafkaRunnable;
import vn.vnpay.receiver.connect.oracle.OracleConnectionPool;
import vn.vnpay.receiver.connect.rabbit.RabbitConnectionPool;
import vn.vnpay.receiver.connect.redis.RedisConnectionPool;
import vn.vnpay.receiver.thread.ShutdownThread;
import vn.vnpay.receiver.utils.ExecutorSingleton;
import vn.vnpay.receiver.utils.GsonSingleton;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.TimeoutException;


@Slf4j
public class MainService {
    private static final RabbitConnectionPool rabbitConnectionPool = RabbitConnectionPool.getInstancePool();
    private static final OracleConnectionPool oracleConnectionPool = OracleConnectionPool.getInstancePool();
    private static final RedisConnectionPool redisConnectionPool = RedisConnectionPool.getInstancePool();
//    private static final KafkaConnectionPool kafkaConnectionPool = KafkaConnectionPool.getInstancePool();

    private static final KafkaConsumerConnectionPool kafkaConsumerConnectionPool = KafkaConsumerConnectionPool.getInstancePool();
    private static final KafkaProducerConnectionPool kafkaProducerConnectionPool = KafkaProducerConnectionPool.getInstancePool();

    private static final ExecutorSingleton executorSingleton = new ExecutorSingleton();
    private static final GsonSingleton gsonSingleton = new GsonSingleton();

    public static void main(String[] args) throws IOException, TimeoutException {
        Runtime.getRuntime().addShutdownHook(new ShutdownThread());

        oracleConnectionPool.start();
//        rabbitConnectionPool.start();
        redisConnectionPool.start();
//        kafkaConnectionPool.start();
        kafkaProducerConnectionPool.start();
        kafkaProducerConnectionPool.start();

//        RabbitConnectionCell rabbitConnectionCell = rabbitConnectionPool.getConnection();
//        rabbitConnectionCell.receiveAndSend();
//        KafkaConnectionCell kafkaConnectionCell = kafkaConnectionPool.getConnection();
//        KafkaConnectionCell kafkaConnectionCell = new KafkaConnectionCell();
//        kafkaConnectionCell.receiveAndSend();

        // receive message
        executorSingleton.getExecutorService().submit(new KafkaRunnable());
    }
}
