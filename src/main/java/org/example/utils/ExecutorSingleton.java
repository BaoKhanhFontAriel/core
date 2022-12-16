package org.example.utils;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.example.error.ExecutorError;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;


@Slf4j
@Getter
@Setter
@ToString
public class ExecutorSingleton {
    private static ExecutorSingleton instance;

    private Boolean isAllTasksFinished;

    private Boolean isErrorHappened ;

    private String errorMessage;

//    private String errorCode;

    private ExecutorError error;

    private ScheduledExecutorService executorService;

    public ExecutorSingleton(){
        log.info("create new ExecuterSingleton...");
        this.executorService = Executors.newScheduledThreadPool(10);
        this.isAllTasksFinished = false;
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
