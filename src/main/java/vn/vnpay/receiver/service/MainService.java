package vn.vnpay.receiver.service;

import lombok.extern.slf4j.Slf4j;
import vn.vnpay.receiver.connect.kafka.*;
import vn.vnpay.receiver.connect.oracle.OracleConnectionPool;
import vn.vnpay.receiver.connect.rabbit.RabbitConnectionPool;
import vn.vnpay.receiver.connect.redis.RedisConnectionPool;
import vn.vnpay.receiver.thread.ShutdownThread;
import vn.vnpay.receiver.utils.ExecutorSingleton;
import vn.vnpay.receiver.utils.GsonSingleton;
import vn.vnpay.receiver.utils.KafkaUtils;


@Slf4j
public class MainService {
    private static final RabbitConnectionPool rabbitConnectionPool = RabbitConnectionPool.getInstancePool();
    private static final OracleConnectionPool oracleConnectionPool = OracleConnectionPool.getInstancePool();
//    private static final KafkaConnectionPool kafkaConnectionPool = KafkaConnectionPool.getInstancePool();

    private static final ExecutorSingleton executorSingleton = new ExecutorSingleton();
    private static final GsonSingleton gsonSingleton = new GsonSingleton();

    public static void main(String[] args) throws Exception {
        Runtime.getRuntime().addShutdownHook(new ShutdownThread());
        ExecutorSingleton.getInstance();

//        oracleConnectionPool.start();
//        rabbitConnectionPool.start();

        RedisConnectionPool.getInstancePool().start();
//        KafkaUtils.createNewTopic(KafkaPoolConfig.KAFKA_PRODUCER_TOPIC, 10, (short) 1);
        KafkaProducerPool.getInstancePool();
        KafkaConsumerPool.getInstancePool().init();
        KafkaConsumerPool.getInstancePool().startPoolPolling();

//        RabbitConnectionCell rabbitConnectionCell = rabbitConnectionPool.getConnection();
//        rabbitConnectionCell.receiveAndSend();
//        KafkaConnectionCell kafkaConnectionCell = kafkaConnectionPool.getConnection();
//        KafkaConnectionCell kafkaConnectionCell = new KafkaConnectionCell();
//        kafkaConnectionCell.receiveAndSend();

//        //create partition
//        KafkaUtils.createNewTopic(KafkaPoolConfig.KAFKA_CONSUMER_TOPIC, 10, (short) 1);

        // receive message
        KafkaUtils.receiveAndSend();
//        KafkaUtils.receiveAndSendPayment();
    }
}
