package vn.vnpay.receiver.runnable;

import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisException;
import vn.vnpay.receiver.connect.redis.RedisConnectionCell;
import vn.vnpay.receiver.connect.redis.RedisConnectionPool;
import vn.vnpay.receiver.error.ErrorCode;
import vn.vnpay.receiver.error.ExecutorError;
import vn.vnpay.receiver.model.ApiRequest;
import vn.vnpay.receiver.utils.ExecutorSingleton;

import java.util.concurrent.Callable;


@Slf4j
public class PushToRedisCallable implements Callable {

    private static final int EXPIRE_TIME_SECONDS = 120;
    private static RedisConnectionPool redisConnectionPool = RedisConnectionPool.getInstancePool();
    private ApiRequest apiRequest;
    RedisConnectionCell redisConnectionCell = RedisConnectionPool.getInstancePool().getConnection();
    public PushToRedisCallable(ApiRequest apiRequest) {
        this.apiRequest = apiRequest;
    }

    public void pushToRedis() throws JedisException, InterruptedException {
        log.info("begin push to jedis: {}", apiRequest.toString());

        long start = System.currentTimeMillis();

        Jedis jedis = redisConnectionCell.getJedis();
        jedis.lpush(apiRequest.getToken(), apiRequest.getData());
        jedis.expire(apiRequest.getToken(), EXPIRE_TIME_SECONDS);

        redisConnectionPool.releaseConnection(redisConnectionCell);

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
            redisConnectionPool.releaseConnection(redisConnectionCell);
        }
        return null;
    }
}
