package org.example.runnable;

import lombok.extern.slf4j.Slf4j;
import org.example.utils.ExecutorSingleton;

@Slf4j
public class LongRunningTask implements Runnable {
    @Override
    public void run() {
        while (true) {
            if (Thread.interrupted()) {
                log.info("thread is interrupted");
                return;
            }

            if (ExecutorSingleton.getInstance().getIsAllTasksFinished()) {
                log.info("all tasks is finished");
                return;
            }

            if (ExecutorSingleton.getInstance().getIsErrorHappened()) {
                log.info("error happened");
                return;
            }
        }
    }
}
