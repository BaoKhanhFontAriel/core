package org.example.main;

import lombok.extern.slf4j.Slf4j;
import org.example.connect.oracle.OracleConnectionPool;
import org.example.connect.rabbit.RabbitConnectionCell;
import org.example.connect.rabbit.RabbitConnectionPool;
import org.example.connect.reddis.RedisConnectionPool;
import org.example.thread.ShutdownThread;
import org.example.utils.ExecutorSingleton;
import org.example.utils.GsonSingleton;

import java.io.IOException;
import java.util.concurrent.*;


@Slf4j
public class MainService {
    private static final String EXCHANGE_NAME = "sender_exchange";
    private static final String QUEUE_NAME = "sender";
    private static final String ROUTING_KEY = "sender_info";

    private static RabbitConnectionPool rabbitConnectionPool = RabbitConnectionPool.getInstancePool();
    private static OracleConnectionPool oracleConnectionPool = OracleConnectionPool.getInstancePool();

    private static RedisConnectionPool reddisConnectionPool = RedisConnectionPool.getInstancePool();

    private static ExecutorSingleton executerSingleton = new ExecutorSingleton();

    private static GsonSingleton gsonSingleton = new GsonSingleton();

    public static void main(String[] args) throws IOException, TimeoutException {

        oracleConnectionPool.start();
        rabbitConnectionPool.start();
        reddisConnectionPool.start();

        RabbitConnectionCell rabbitConnectionCell = rabbitConnectionPool.getConnection();
        rabbitConnectionCell.receiveAndSend();
        rabbitConnectionPool.releaseConnection(rabbitConnectionCell);

        Runtime.getRuntime().addShutdownHook(new ShutdownThread());
    }
}
