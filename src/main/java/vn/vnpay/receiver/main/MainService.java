package vn.vnpay.receiver.main;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewPartitions;
import vn.vnpay.receiver.connect.kafka.*;
import vn.vnpay.receiver.connect.kafka.runnable.KafkaProducerRunner;
import vn.vnpay.receiver.connect.kafka.runnable.KafkaReceiveAndSendRunnable;
import vn.vnpay.receiver.connect.oracle.OracleConnectionPool;
import vn.vnpay.receiver.connect.rabbit.RabbitConnectionPool;
import vn.vnpay.receiver.connect.redis.RedisConnectionPool;
import vn.vnpay.receiver.model.ApiResponse;
import vn.vnpay.receiver.thread.ShutdownThread;
import vn.vnpay.receiver.utils.ExecutorSingleton;
import vn.vnpay.receiver.utils.GsonSingleton;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;


@Slf4j
public class MainService {
    private static final RabbitConnectionPool rabbitConnectionPool = RabbitConnectionPool.getInstancePool();
    private static final OracleConnectionPool oracleConnectionPool = OracleConnectionPool.getInstancePool();
    private static final RedisConnectionPool redisConnectionPool = RedisConnectionPool.getInstancePool();
//    private static final KafkaConnectionPool kafkaConnectionPool = KafkaConnectionPool.getInstancePool();

    private static final ExecutorSingleton executorSingleton = new ExecutorSingleton();
    private static final GsonSingleton gsonSingleton = new GsonSingleton();

    public static void main(String[] args) throws IOException, TimeoutException {
        Runtime.getRuntime().addShutdownHook(new ShutdownThread());

//        oracleConnectionPool.start();
//        rabbitConnectionPool.start();
        redisConnectionPool.start();
//        kafkaConnectionPool.start();
        KafkaProducerConnectionPool.getInstancePool().start();
        KafkaConsumerConnectionPool.getInstancePool().start();

//        RabbitConnectionCell rabbitConnectionCell = rabbitConnectionPool.getConnection();
//        rabbitConnectionCell.receiveAndSend();
//        KafkaConnectionCell kafkaConnectionCell = kafkaConnectionPool.getConnection();
//        KafkaConnectionCell kafkaConnectionCell = new KafkaConnectionCell();
//        kafkaConnectionCell.receiveAndSend();

//        //create partition
        Properties props = new Properties();
        props.put("bootstrap.servers","localhost:29092");
        AdminClient adminClient = AdminClient.create(props);
        Map<String, NewPartitions> newPartitionSet = new HashMap<>();
        newPartitionSet.put(KafkaConnectionPoolConfig.KAFKA_PRODUCER_TOPIC, NewPartitions.increaseTo(10));
        adminClient.createPartitions(newPartitionSet);
        adminClient.close();

        // receive message
            receiveAndSend();

    }

    public static void receiveAndSend() {
        ExecutorSingleton.getInstance().getExecutorService().submit(new KafkaReceiveAndSendRunnable());
    }
}
