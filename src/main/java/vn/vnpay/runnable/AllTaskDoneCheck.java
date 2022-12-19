package vn.vnpay.runnable;

import lombok.extern.slf4j.Slf4j;
import vn.vnpay.utils.ExecutorSingleton;

import java.util.concurrent.Future;

@Slf4j
public class AllTaskDoneCheck implements Runnable {

    private Future[] futureList;

    public AllTaskDoneCheck(Future... futureList) {
        this.futureList = futureList;
    }

    private Boolean isRunning = true;

    @Override
    public void run() {
        log.info("begin check all task done");
        while (isRunning) {
            for (Future r :
                    futureList) {
                if (r.isDone()) {
                    isRunning = false;
                }
                else {
                    isRunning = true;
                    break;
                }
            }

            if (!isRunning){
                log.info("all Task is done");
                ExecutorSingleton.getInstance().setIsAllTasksFinished(true);
                isRunning = true;
                return;
            }
        }
    }
}
