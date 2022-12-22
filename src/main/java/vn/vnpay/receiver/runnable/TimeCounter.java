package vn.vnpay.receiver.runnable;

import lombok.extern.slf4j.Slf4j;
import vn.vnpay.receiver.utils.AppConfigSingleton;
import vn.vnpay.receiver.utils.ExecutorSingleton;

@Slf4j
public class TimeCounter implements Runnable {

    private int timeCounter = AppConfigSingleton.getInstance().getIntProperty("time.out");

    private boolean isTimeOut = false;

    @Override
    public void run() {
        while (true) {
            if (Thread.interrupted()) {
                log.info("thread is interrupted");
                return;
            }
        }
    }
}
