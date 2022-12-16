package org.example.connect.oracle;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.example.connect.AbsThread;

import java.util.concurrent.LinkedBlockingQueue;


@Slf4j
@Setter
@Getter
@ToString
public class OracleConnectionPool {
    private LinkedBlockingQueue<OracleConnectionCell> pool = new LinkedBlockingQueue<>();
    protected int numOfConnectionCreated = 0;

    protected String sid;
    protected int max_pool_size;
    protected int init_pool_size;
    protected int min_pool_size;
    protected long time_out = 10000;

    protected String url;
    protected String user;
    protected String password;

    protected Thread thread;
    protected long start_time;
    protected long end_time;
    protected static OracleConnectionPool instancePool;

    public synchronized static OracleConnectionPool getInstancePool() {
        if (instancePool == null) {
            instancePool = new OracleConnectionPool();
            instancePool.init_pool_size = OracleConnectionPoolConfig.INIT_POOL_SIZE;
            instancePool.max_pool_size = OracleConnectionPoolConfig.MAX_POOL_SIZE;
            instancePool.min_pool_size = OracleConnectionPoolConfig.MIN_POOL_SIZE;
            instancePool.url = OracleConnectionPoolConfig.URL;
            instancePool.user = OracleConnectionPoolConfig.USERNAME;
            instancePool.password = OracleConnectionPoolConfig.PASSWORD;
            instancePool.time_out = OracleConnectionPoolConfig.TIME_OUT;
            instancePool.sid = OracleConnectionPoolConfig.SID;
            /*
             * When the number of connection > min connection , close TimeOut Connection
             */
            instancePool.thread = new Thread(() -> {
                while (true) {
                    for (OracleConnectionCell connection : instancePool.pool) {
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
        log.info("Create oracle Connection pool........................ ");
        // Load Connection to Pool
        start_time = System.currentTimeMillis();
        try {
            for (int i = 0; i < init_pool_size; i++) {
                OracleConnectionCell connection = new OracleConnectionCell(url, user, password, time_out);
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

    public synchronized OracleConnectionCell getConnection() {
        OracleConnectionCell connectionWraper = null;
        if (pool.size() == 0 && numOfConnectionCreated < max_pool_size) {
            connectionWraper = new OracleConnectionCell(url, user, password, time_out);
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


    public void releaseConnection(OracleConnectionCell conn) {
        try {
            if (conn.isClosed()) {
                pool.remove(conn);
                OracleConnectionCell connection = new OracleConnectionCell(url, user, password, time_out);
                pool.put(connection);
            } else {
                pool.put(conn);
            }
        } catch (Exception e) {
            log.info("Connection : " + conn.toString(), e);
        }
    }
}
