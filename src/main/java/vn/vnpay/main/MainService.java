package vn.vnpay.main;

import lombok.extern.slf4j.Slf4j;
import vn.vnpay.connect.oracle.OracleConnectionPool;
import vn.vnpay.connect.rabbit.RabbitConnectionCell;
import vn.vnpay.connect.rabbit.RabbitConnectionPool;
import vn.vnpay.connect.reddis.RedisConnectionPool;
import vn.vnpay.thread.ShutdownThread;
import vn.vnpay.utils.ExecutorSingleton;
import vn.vnpay.utils.GsonSingleton;

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

//        oracleConnectionPool.start();
        rabbitConnectionPool.start();
        redisConnectionPool.start();

        RabbitConnectionCell rabbitConnectionCell = rabbitConnectionPool.getConnection();
        rabbitConnectionCell.receiveAndSend();
        rabbitConnectionPool.releaseConnection(rabbitConnectionCell);

        Runtime.getRuntime().addShutdownHook(new ShutdownThread());
    }
}
