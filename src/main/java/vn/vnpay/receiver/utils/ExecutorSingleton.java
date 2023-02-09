package vn.vnpay.receiver.utils;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import vn.vnpay.receiver.error.ExecutorError;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;


@Slf4j
@Getter
@Setter
@ToString
public class ExecutorSingleton {
    private static ExecutorSingleton instance;
    private ScheduledExecutorService executorService;

    public ExecutorSingleton(){
        log.info("create new ExecutorServiceSingleton...");
        this.executorService = Executors.newScheduledThreadPool(1000);
    }

    public static ExecutorSingleton getInstance(){
        if(instance == null){
            instance = new ExecutorSingleton();
        }
        return instance;
    }

    public static void shutdownNow() {
        instance.executorService.shutdownNow();
    }

    public static void wakeup() {
        instance.executorService = Executors.newScheduledThreadPool(10);
    }


    public ScheduledExecutorService getExecutorService() {
        return executorService;
    }
}
