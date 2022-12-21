package vn.vnpay.receiver.runnable;

import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisException;
import vn.vnpay.receiver.connect.redis.RedisConnectionCell;
import vn.vnpay.receiver.connect.redis.RedisConnectionPool;
import vn.vnpay.receiver.error.ErrorCode;
import vn.vnpay.receiver.error.ExecutorError;
import vn.vnpay.receiver.model.ApiRequest;
import vn.vnpay.receiver.model.ApiResponse;


@Slf4j
public class PushToRedisRunnale implements Runnable {

    private static final int EXPIRE_TIME_SECONDS = 120;
    private static RedisConnectionPool redisConnectionPool = RedisConnectionPool.getInstancePool();
    private ApiRequest apiRequest;
    private volatile boolean isErrorHappened;
    private volatile ExecutorError error;
    private volatile ApiResponse apiResponse;

    private volatile String response;

    RedisConnectionCell redisConnectionCell = RedisConnectionPool.getInstancePool().getConnection();

    public PushToRedisRunnale(ApiRequest apiRequest) {
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
    public synchronized void run() {
        log.info("api response in push to redis: {}", apiResponse);

        try {
            pushToRedis();
//            ExecutorSingleton.getInstance().setIsRedisFutureDone(true);
        } catch (JedisException | InterruptedException e) {
            log.error("fail to push to redis: ", e);

            synchronized (this ){
                apiResponse = new ApiResponse(ErrorCode.ORACLE_ERROR, "fail: " + e.getMessage(), apiRequest.getToken());
                log.info("api response error in push to redis: {}", apiResponse);
            }


//            isErrorHappened = true;
//            error = new ExecutorError(ErrorCode.REDIS_ERROR, e.getMessage());

//            ExecutorSingleton.getInstance().setIsErrorHappened(true);
//            ExecutorSingleton.getInstance().setError(new ExecutorError(ErrorCode.REDIS_ERROR, e.getMessage()));
            try {
                redisConnectionPool.releaseConnection(redisConnectionCell);
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
        }
    }
}
