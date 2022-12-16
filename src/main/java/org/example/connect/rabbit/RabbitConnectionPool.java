package org.example.connect.rabbit;


import com.rabbitmq.client.ConnectionFactory;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.example.connect.AbsThread;
import org.example.connect.oracle.OracleConnectionCell;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
@Setter
@Getter
@ToString
public class RabbitConnectionPool {
    private LinkedBlockingQueue<RabbitConnectionCell> pool = new LinkedBlockingQueue<>();

    private ConnectionFactory factory = new ConnectionFactory();

    protected int numOfConnectionCreated = 0;
    protected int max_pool_size;
    protected int init_pool_size;
    protected int min_pool_size;
    protected long time_out = 10000;

    protected String url;
    protected String queueName;
    protected String exchangeName;
    protected String exchangeType;
    protected String routingKey;

    protected Thread thread;
    protected long start_time;
    protected long end_time;
    protected static RabbitConnectionPool instancePool;

    ExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    public synchronized static RabbitConnectionPool getInstancePool() {
        if (instancePool == null) {
            instancePool = new RabbitConnectionPool();
            instancePool.init_pool_size = RabbitConnectionPoolConfig.INIT_POOL_SIZE;
            instancePool.max_pool_size = RabbitConnectionPoolConfig.MAX_POOL_SIZE;
            instancePool.min_pool_size = RabbitConnectionPoolConfig.MIN_POOL_SIZE;
            instancePool.url = RabbitConnectionPoolConfig.URL;
            instancePool.queueName = RabbitConnectionPoolConfig.QUEUENAME;
            instancePool.exchangeName = RabbitConnectionPoolConfig.EXHCHANGENAME;
            instancePool.exchangeType = RabbitConnectionPoolConfig.EXHCHANGETYPE;
            instancePool.routingKey = RabbitConnectionPoolConfig.ROUTING_KEY;
            instancePool.time_out = RabbitConnectionPoolConfig.TIME_OUT;
            instancePool.thread = new Thread(() -> {
                while(true){
                    for (RabbitConnectionCell connection : instancePool.pool) {
                        if (instancePool.numOfConnectionCreated > instancePool.min_pool_size) {
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

        }
        return instancePool;
    }

    public void start() {
        log.info("Create Rabbit Connection pool........................ ");
        // Load Connection to Pool
        start_time = System.currentTimeMillis();
        try {
            for (int i = 0; i < init_pool_size; i++) {
                RabbitConnectionCell connection = new RabbitConnectionCell(factory, exchangeName, exchangeType, routingKey, time_out);
                pool.put(connection);
                numOfConnectionCreated++;
            }
        } catch (Exception e) {
            log.warn("[Message : can not start connection pool] - [Connection pool : {0}] - " + "[Exception : {1}]",
                    this.toString(), e);
        }
        thread.start();
        end_time = System.currentTimeMillis();
        log.info("Start Rabbit Connection pool in : {} ms", (end_time - start_time));
    }

    public synchronized RabbitConnectionCell getConnection() {
        log.info("begin getting rabbit connection!");
        RabbitConnectionCell connectionWraper = null;
        if (pool.size() == 0 && numOfConnectionCreated < max_pool_size) {
            connectionWraper = new RabbitConnectionCell(factory, exchangeName, exchangeType, routingKey, time_out);
            try {
                pool.put(connectionWraper);
            } catch (InterruptedException e) {
                log.warn("Can not PUT Connection to Pool, Current Poll size = " + pool.size()
                        + " , Number Connection : " + numOfConnectionCreated, e);
                e.printStackTrace();
            }
            numOfConnectionCreated++;
        }

        try {
            connectionWraper = pool.take();
        } catch (InterruptedException e) {
            log.warn("Can not GET Connection from Pool, Current Poll size = " + pool.size()
                    + " , Number Connection : " + numOfConnectionCreated);
            e.printStackTrace();
        }
        connectionWraper.setRelaxTime(System.currentTimeMillis());
        log.info("finish getting rabbit connection, ");
        log.info("set relax time: {0}", connectionWraper.getRelaxTime());
        return connectionWraper;
    }


    public void releaseConnection(RabbitConnectionCell conn) {
        log.info("begin releasing connection {}", conn.toString());
        try {
            if (conn.isClosed()) {
                pool.remove(conn);
                RabbitConnectionCell connection = new RabbitConnectionCell(factory, exchangeName, exchangeType, routingKey, time_out);
                pool.put(connection);
            } else {
                pool.put(conn);
            }
            log.info("successfully release connection {}", conn.toString());
        } catch (Exception e) {
            log.info("Connection : " + conn.toString(), e);
        }
    }

}
