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
    private Boolean isOracleFutureDone;
    private Boolean isRedisFutureDone;
    private Boolean isErrorHappened ;
    private ExecutorError error;
    private ScheduledExecutorService executorService;

    public ExecutorSingleton(){
        log.info("create new ExecutorServiceSingleton...");
        this.executorService = Executors.newScheduledThreadPool(10);
        this.isOracleFutureDone = false;
        this.isRedisFutureDone = false;
        this.isErrorHappened = false;
        this.error = null;
    }

    public static ExecutorSingleton getInstance(){
        if(instance == null){
            instance = new ExecutorSingleton();
        }
        return instance;
    }


    public ScheduledExecutorService getExecutorService() {
        return executorService;
    }
}