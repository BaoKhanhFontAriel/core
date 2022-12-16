package org.example.connect.reddis;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.example.connect.AbsThread;
import org.example.connect.oracle.OracleConnectionCell;

import java.util.concurrent.LinkedBlockingQueue;


@Slf4j
@Getter
@Setter
@ToString
public class RedisConnectionPool {
    private LinkedBlockingQueue<RedisConnectionCell> pool = new LinkedBlockingQueue<>();
    protected int numOfConnectionCreated = 0;

    protected int max_pool_size;
    protected int init_pool_size;
    protected int min_pool_size;
    protected long time_out = 10000;

    protected String url;

    protected Thread thread;
    protected long start_time;
    protected long end_time;
    protected static RedisConnectionPool instancePool;

    public synchronized static RedisConnectionPool getInstancePool(){
        if (instancePool == null) {
            instancePool = new RedisConnectionPool();
            instancePool.init_pool_size = RedisConnectionPoolConfig.INIT_POOL_SIZE;
            instancePool.max_pool_size = RedisConnectionPoolConfig.MAX_POOL_SIZE;
            instancePool.min_pool_size = RedisConnectionPoolConfig.MIN_POOL_SIZE;
            instancePool.url = RedisConnectionPoolConfig.URL;
            instancePool.time_out = RedisConnectionPoolConfig.TIME_OUT;
            instancePool.thread = new Thread(() -> {
                while(true){
                    for (RedisConnectionCell connection : instancePool.pool) {
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
        log.info("Create Reddis Connection pool........................ ");
        // Load Connection to Pool
        start_time = System.currentTimeMillis();
        try {
            for (int i = 0; i < init_pool_size; i++) {
                RedisConnectionCell connection = new RedisConnectionCell(url, time_out);
                pool.put(connection);
                numOfConnectionCreated++;
            }
        } catch (Exception e) {
            log.warn(String.format(
                    "[Message : can not start oracle connection pool] - [Connection pool : %s] - " + "[Exception : %s]",
                    this.toString(), e));
        }
        thread.start();
        end_time = System.currentTimeMillis();
        log.info("Start Rabbit Connection pool in : {} ms", (end_time - start_time));
    }

    public synchronized RedisConnectionCell getConnection() {
        RedisConnectionCell connectionWraper = null;
        if (pool.size() == 0 && numOfConnectionCreated < max_pool_size) {
            connectionWraper = new RedisConnectionCell(url, time_out);
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
        return connectionWraper;
    }


    public void releaseConnection(RedisConnectionCell conn) {
        try {
            if (conn.isClosed()) {
                pool.remove(conn);
                RedisConnectionCell connection = new RedisConnectionCell(url, time_out);
                pool.put(connection);
            } else {
                pool.put(conn);
            }
        } catch (Exception e) {
            log.info("Connection : " + conn.toString(), e);
        }
    }
}
