package vn.vnpay.receiver.thread;


import lombok.extern.slf4j.Slf4j;
import vn.vnpay.receiver.connect.oracle.OracleConnectionCell;
import vn.vnpay.receiver.connect.oracle.OracleConnectionPool;
import vn.vnpay.receiver.connect.rabbit.RabbitConnectionCell;
import vn.vnpay.receiver.connect.rabbit.RabbitConnectionPool;
import vn.vnpay.receiver.connect.redis.RedisConnectionCell;
import vn.vnpay.receiver.connect.redis.RedisConnectionPool;

@Slf4j
public class ShutdownThread extends Thread{
    RabbitConnectionPool rabbitConnectionPool = RabbitConnectionPool.getInstancePool();
    OracleConnectionPool oracleConnectionPool = OracleConnectionPool.getInstancePool();
    RedisConnectionPool redisConnectionPool = RedisConnectionPool.getInstancePool();
    public void run() {
        rabbitConnectionPool.getPool().forEach(RabbitConnectionCell::close);
        oracleConnectionPool.getPool().forEach(OracleConnectionCell::close);
        redisConnectionPool.getPool().forEach(RedisConnectionCell::close);
        redisConnectionPool.getPool().clear();
        rabbitConnectionPool.getPool().clear();
        oracleConnectionPool.getPool().clear();

        System.out.println("shut down hook task completed..");
    }
}
