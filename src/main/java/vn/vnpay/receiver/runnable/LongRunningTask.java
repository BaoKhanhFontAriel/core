package vn.vnpay.receiver.runnable;

import lombok.extern.slf4j.Slf4j;
import vn.vnpay.receiver.utils.ExecutorSingleton;

@Slf4j
public class LongRunningTask implements Runnable {
    @Override
    public void run() {
        while (true) {
            if (Thread.interrupted()) {
                log.info("thread is interrupted");
                return;
            }

            if (ExecutorSingleton.getInstance().getIsOracleFutureDone()
                    && ExecutorSingleton.getInstance().getIsRedisFutureDone()) {
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
