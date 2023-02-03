package vn.vnpay.receiver.thread;


import lombok.extern.slf4j.Slf4j;
import vn.vnpay.receiver.connect.kafka.*;
import vn.vnpay.receiver.connect.oracle.OracleConnectionCell;
import vn.vnpay.receiver.connect.oracle.OracleConnectionPool;
import vn.vnpay.receiver.connect.rabbit.RabbitConnectionCell;
import vn.vnpay.receiver.connect.rabbit.RabbitConnectionPool;
import vn.vnpay.receiver.connect.redis.RedisConnectionCell;
import vn.vnpay.receiver.connect.redis.RedisConnectionPool;

@Slf4j
public class ShutdownThread extends Thread {
    RabbitConnectionPool rabbitConnectionPool = RabbitConnectionPool.getInstancePool();
    OracleConnectionPool oracleConnectionPool = OracleConnectionPool.getInstancePool();
    RedisConnectionPool redisConnectionPool = RedisConnectionPool.getInstancePool();
    private static final KafkaConnectionPool kafkaConnectionPool = KafkaConnectionPool.getInstancePool();
    private static final KafkaConsumerConnectionPool kafkaConsumerConnectionPool = KafkaConsumerConnectionPool.getInstancePool();
    private static final KafkaProducerConnectionPool kafkaProducerConnectionPool = KafkaProducerConnectionPool.getInstancePool();
    public void run() {
        log.info("start shut down hook");
        rabbitConnectionPool.getPool().forEach(RabbitConnectionCell::close);
        oracleConnectionPool.getPool().forEach(OracleConnectionCell::close);
        redisConnectionPool.getPool().forEach(RedisConnectionCell::close);
        kafkaConnectionPool.getPool().forEach(KafkaConnectionCell::close);
        kafkaProducerConnectionPool.getPool().forEach(KafkaProducerConnectionCell::close);
        kafkaConsumerConnectionPool.getPool().forEach(KafkaConsumerConnectionCell::close);
        redisConnectionPool.getPool().clear();
        rabbitConnectionPool.getPool().clear();
        oracleConnectionPool.getPool().clear();
        kafkaConnectionPool.getPool().clear();
        kafkaProducerConnectionPool.getPool().clear();
        kafkaConsumerConnectionPool.getPool().clear();
        log.info("shut down hook task completed..");
    }
}
