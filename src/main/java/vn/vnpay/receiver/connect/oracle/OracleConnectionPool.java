package vn.vnpay.receiver.connect.oracle;


import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.LinkedBlockingQueue;


@Setter
@Getter
@ToString
//@Slf4j
public class OracleConnectionPool {
    private static final Logger log  = LoggerFactory.getLogger(OracleConnectionPool.class);

    private LinkedBlockingQueue<OracleConnectionCell> pool = new LinkedBlockingQueue<>();
    protected int numOfConnectionCreated = 0;

    protected String sid;
    protected int maxPoolSize;
    protected int initPoolSize;
    protected int minPoolSize;
    protected long timeOut = 10000;

    protected String url;
    protected String user;
    protected String password;

    protected Thread thread;
    protected long startTime;
    protected long endTime;
    protected static OracleConnectionPool instancePool;

    public synchronized static OracleConnectionPool getInstancePool() {
        if (instancePool == null) {
            instancePool = new OracleConnectionPool();
            instancePool.initPoolSize = OracleConnectionPoolConfig.INIT_POOL_SIZE;
            instancePool.maxPoolSize = OracleConnectionPoolConfig.MAX_POOL_SIZE;
            instancePool.minPoolSize = OracleConnectionPoolConfig.MIN_POOL_SIZE;
            instancePool.url = OracleConnectionPoolConfig.URL;
            instancePool.user = OracleConnectionPoolConfig.USERNAME;
            instancePool.password = OracleConnectionPoolConfig.PASSWORD;
            instancePool.timeOut = OracleConnectionPoolConfig.TIME_OUT;
            /*
             * When the number of connection > min connection , close TimeOut Connection
             */
            instancePool.thread = new Thread(() -> {
                while (true) {
                    for (OracleConnectionCell connection : instancePool.pool) {
                        if (instancePool.numOfConnectionCreated > instancePool.minPoolSize) {
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
        startTime = System.currentTimeMillis();
        try {
            for (int i = 0; i < initPoolSize; i++) {
                OracleConnectionCell connection = new OracleConnectionCell(url, user, password, timeOut);
                pool.put(connection);
                numOfConnectionCreated++;
            }
        } catch (Exception e) {
            log.warn(String.format(
                    "[Message : can not start oracle connection pool] - [Connection pool : %s] - " + "[Exception : %s]",
                    this.toString(), e));
        }
        thread.start();
        endTime = System.currentTimeMillis();
        log.info("Start Rabbit Connection pool in : {} ms", (endTime - startTime));
    }

    public synchronized OracleConnectionCell getConnection() {
        OracleConnectionCell connectionWraper = null;
        if (pool.size() == 0 && numOfConnectionCreated < maxPoolSize) {
            connectionWraper = new OracleConnectionCell(url, user, password, timeOut);
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
                OracleConnectionCell connection = new OracleConnectionCell(url, user, password, timeOut);
                pool.put(connection);
            } else {
                pool.put(conn);
            }
        } catch (Exception e) {
            log.info("Connection : " + conn.toString(), e);
        }
    }
}
