package vn.vnpay.receiver.runnable;

import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisException;
import vn.vnpay.receiver.connect.redis.RedisConnectionCell;
import vn.vnpay.receiver.connect.redis.RedisConnectionPool;
import vn.vnpay.receiver.error.ErrorCode;
import vn.vnpay.receiver.exceptions.RedisDataProcessingException;
import vn.vnpay.receiver.model.ApiRequest;
import vn.vnpay.receiver.model.ApiResponse;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;


@Slf4j
public class PushToRedisCallable implements Callable<ApiResponse> {

    private static final int EXPIRE_TIME_SECONDS = 120;
    private static RedisConnectionPool redisConnectionPool = RedisConnectionPool.getInstancePool();
    private ApiRequest apiRequest;
    RedisConnectionCell redisConnectionCell = RedisConnectionPool.getInstancePool().getConnection();

    public PushToRedisCallable(ApiRequest apiRequest) {
        this.apiRequest = apiRequest;
    }

    public void pushToRedis() throws JedisException, InterruptedException {
        log.info("begin push to jedis");

        long start = System.currentTimeMillis();

        Jedis jedis = redisConnectionCell.getJedis();
        jedis.lpush(apiRequest.getToken(), apiRequest.getData());
        jedis.expire(apiRequest.getToken(), EXPIRE_TIME_SECONDS);
        redisConnectionPool.releaseConnection(redisConnectionCell);

        long end = System.currentTimeMillis();

        log.info("successfully push to jedis in {} ms", end - start);
    }

    @Override
    public ApiResponse call() throws RedisDataProcessingException, InterruptedException {
        log.info("push to redis");
        ApiResponse apiResponse = null;
//
//        try {
//            pushToRedis();
//        } catch (JedisException | InterruptedException e) {
//            log.error("fail to push to redis: ", e);
//             apiResponse = new ApiResponse(ErrorCode.ORACLE_ERROR, "fail: " + e.getMessage(), apiRequest.getToken());
//        }
//        finally {
//            redisConnectionPool.releaseConnection(redisConnectionCell);
//        }


        try {
            pushToRedis();
        } catch (JedisException | InterruptedException e) {
            throw new RedisDataProcessingException("fail to push to redis: " + e.getMessage());
        }
        finally {
            redisConnectionPool.releaseConnection(redisConnectionCell);
        }

        return apiResponse;
    }
}
