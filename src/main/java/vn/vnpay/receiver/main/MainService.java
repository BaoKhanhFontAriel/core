package vn.vnpay.receiver.main;

import lombok.extern.slf4j.Slf4j;
import vn.vnpay.receiver.connect.oracle.OracleConnectionPool;
import vn.vnpay.receiver.connect.rabbit.RabbitConnectionCell;
import vn.vnpay.receiver.connect.rabbit.RabbitConnectionPool;
import vn.vnpay.receiver.connect.redis.RedisConnectionPool;
import vn.vnpay.receiver.thread.ShutdownThread;
import vn.vnpay.receiver.utils.ExecutorSingleton;
import vn.vnpay.receiver.utils.GsonSingleton;

import java.io.IOException;
import java.util.concurrent.*;


@Slf4j
public class MainService {
    private static final RabbitConnectionPool rabbitConnectionPool = RabbitConnectionPool.getInstancePool();
    private static final OracleConnectionPool oracleConnectionPool = OracleConnectionPool.getInstancePool();

    private static final RedisConnectionPool redisConnectionPool = RedisConnectionPool.getInstancePool();

    private static final ExecutorSingleton executorSingleton = new ExecutorSingleton();

    private static final GsonSingleton gsonSingleton = new GsonSingleton();

    public static void main(String[] args) throws IOException, TimeoutException {

        oracleConnectionPool.start();
        rabbitConnectionPool.start();
        redisConnectionPool.start();

        RabbitConnectionCell rabbitConnectionCell = rabbitConnectionPool.getConnection();
        rabbitConnectionCell.receiveAndSend();

        Runtime.getRuntime().addShutdownHook(new ShutdownThread());
    }
}
