package vn.vnpay.receiver.runnable;

import lombok.extern.slf4j.Slf4j;
import vn.vnpay.receiver.connect.reddis.RedisConnectionCell;
import vn.vnpay.receiver.connect.reddis.RedisConnectionPool;
import vn.vnpay.receiver.error.ErrorCode;
import vn.vnpay.receiver.error.ExecutorError;
import vn.vnpay.receiver.model.CustomerRequest;
import vn.vnpay.receiver.utils.ExecutorSingleton;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisException;

import java.util.concurrent.Callable;


@Slf4j
public class PushToRedisCallable implements Callable {

    private static final int EXPIRE_TIME_SECONDS = 120;
    private static RedisConnectionPool reddisConnectionPool = RedisConnectionPool.getInstancePool();

    private CustomerRequest customerRequest;

    private boolean isSuccessful;
    private boolean isRunning = true;
    RedisConnectionCell redisConnectionCell = RedisConnectionPool.getInstancePool().getConnection();
    public PushToRedisCallable(CustomerRequest customerRequest) {
        this.customerRequest = customerRequest;
    }

    public void pushToRedis() throws JedisException, InterruptedException {
        log.info("begin push to jedis: {}", customerRequest.toString());

        long start = System.currentTimeMillis();

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
            ExecutorSingleton.getInstance().setIsRedisFutureDone(true);
        } catch (JedisException | InterruptedException e) {
            log.error("fail to push to redis: ", e);
            ExecutorSingleton.getInstance().setIsErrorHappened(true);
            ExecutorSingleton.getInstance().setError(new ExecutorError(ErrorCode.REDIS_ERROR, e.getMessage()));
            reddisConnectionPool.releaseConnection(redisConnectionCell);
        }
        return null;
    }
}
