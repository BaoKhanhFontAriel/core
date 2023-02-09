package vn.vnpay.receiver.utils;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import vn.vnpay.receiver.connect.kafka.KafkaConnectionPoolConfig;
import vn.vnpay.receiver.error.ErrorCode;
import vn.vnpay.receiver.model.ApiRequest;
import vn.vnpay.receiver.model.ApiResponse;
import vn.vnpay.receiver.runnable.PushToOracleCallable;
import vn.vnpay.receiver.runnable.PushToRedisCallable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@Slf4j
public class DataUtils {
    public static ApiResponse uploadData(String stringJson){
        ApiRequest apiRequest = GsonSingleton.getInstance().getGson().fromJson(stringJson, ApiRequest.class);
        ApiResponse apiResponse = new ApiResponse("00", "success", apiRequest.getToken());

        // set up thread pool
        ScheduledExecutorService executor = ExecutorSingleton.getInstance().getExecutorService();
        // add runnable for pushing to redis
        Future redisFuture = executor.submit(new PushToRedisCallable(apiRequest));
        // add runnable for pushing to oracle
        Future oracleFuture = executor.schedule(new PushToOracleCallable(apiRequest),
                KafkaConnectionPoolConfig.REQUEST_TIME_SLEEP,
                TimeUnit.MILLISECONDS);

        List<Future> futureList = new ArrayList<>();
        futureList.add(redisFuture);
        futureList.add(oracleFuture);

//         concurrency for redis and oracle
//         redis -> oracle
        for (Future f : futureList) {
            try {
                ApiResponse response = (ApiResponse) f.get(KafkaConnectionPoolConfig.REQUEST_TIME_OUT,
                        TimeUnit.MILLISECONDS);
                if (response != null) {
                    apiResponse = response;
                    break;
                }
            } catch (InterruptedException e) {
                log.error("{} has InterruptedException: {}", Thread.currentThread().getName(), e.getMessage());
                apiResponse = new ApiResponse(ErrorCode.INTERRUPTED_ERROR, "fail: " + e.getMessage(), apiRequest.getToken());
            } catch (ExecutionException e) {
                log.error("{} has execution error: {}", Thread.currentThread().getName(), e.getMessage());
                apiResponse = new ApiResponse(ErrorCode.EXECUTION_ERROR, "fail: " + e.getMessage(), apiRequest.getToken());
            } catch (TimeoutException e) {
                apiResponse = new ApiResponse(ErrorCode.TIME_OUT_ERROR, "fail: " + e, apiRequest.getToken());
                String token = TokenUtils.generateNewToken();
                MDC.put("token", token);
                log.error("Time execution in core is over 1 minute: ", e);
                MDC.remove("token");
                break;
            }
        }

        return apiResponse;
    }

}
