package org.example.runnable;

import lombok.extern.slf4j.Slf4j;
import org.example.connect.reddis.RedisConnectionCell;
import org.example.connect.reddis.RedisConnectionPool;
import org.example.error.ErrorCode;
import org.example.error.ExecutorError;
import org.example.model.CustomerRequest;
import org.example.utils.ExecutorSingleton;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisException;

import java.util.concurrent.Callable;


@Slf4j
public class PushToRedisCallable implements Callable {

    private static final int EXPIRE_TIME_SECONDS = 120;
    private static RedisConnectionPool reddisConnectionPool = RedisConnectionPool.getInstancePool();

    private CustomerRequest customerRequest;
    private boolean isRunning = true;

    public PushToRedisCallable(CustomerRequest customerRequest) {
        this.customerRequest = customerRequest;
    }

    public void pushToRedis() throws JedisException {
        log.info("begin push to jedis: {}", customerRequest.toString());

        long start = System.currentTimeMillis();

        RedisConnectionCell redisConnectionCell = RedisConnectionPool.getInstancePool().getConnection();
        Jedis jedis = redisConnectionCell.getJedis();
        jedis.lpush(customerRequest.getToken(), customerRequest.getData());
        jedis.expire(customerRequest.getToken(), EXPIRE_TIME_SECONDS);
        reddisConnectionPool.releaseConnection(redisConnectionCell);

        long end = System.currentTimeMillis();

        log.info("successfully push to jedis in {} ms", end - start);

    }

    @Override
    public Object call() throws Exception {
        try {
            pushToRedis();
        } catch (JedisException e) {
            log.error("fail to push to redis: ", e);
            ExecutorSingleton.getInstance().setIsErrorHappened(true);
            ExecutorSingleton.getInstance().setError(new ExecutorError(ErrorCode.REDIS_ERROR, e.getMessage()));

//            ExecutorSingleton.getInstance().setErrorCode(ErrorCode.REDIS_ERROR);
//            ExecutorSingleton.getInstance().setErrorMessage(e.getMessage());
        }
        return null;
    }
}
