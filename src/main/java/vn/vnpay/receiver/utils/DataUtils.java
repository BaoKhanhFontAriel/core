package vn.vnpay.receiver.utils;

import lombok.extern.slf4j.Slf4j;
import vn.vnpay.receiver.connect.kafka.KafkaConsumerPool;
import vn.vnpay.receiver.connect.kafka.KafkaPoolConfig;
import vn.vnpay.receiver.model.ApiRequest;
import vn.vnpay.receiver.runnable.PushToOracleCallable;
import vn.vnpay.receiver.runnable.PushToRedisCallable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@Slf4j
public class DataUtils {
    public static void processData(ApiRequest apiRequest) throws Exception {
        // set up thread pool
        ScheduledExecutorService executor = ExecutorSingleton.getInstance().getExecutorService();
        // add runnable for pushing to redis
        Future redisFuture = executor.submit(new PushToRedisCallable(apiRequest));
        // add runnable for pushing to oracle
        Future oracleFuture = executor.schedule(new PushToOracleCallable(apiRequest),200, TimeUnit.MILLISECONDS);

        List<Future> futureList = new ArrayList<>();
        futureList.add(redisFuture);
        futureList.add(oracleFuture);

        // concurrency for redis and oracle
        // redis -> oracle

        for (Future f : futureList) {
            try {
                f.get(60000, TimeUnit.MILLISECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                throw new Exception(e.getMessage());
            }
        }
    }
}
