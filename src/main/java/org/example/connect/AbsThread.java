package org.example.connect;


import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbsThread extends Thread {

    @Override
    public void run() {
        while(true) {
            synchronized (this) {
                try {
                    sleep(10);
                } catch (Exception e) {
                    log.error("absthread not sleep: {0}", e);
                }
            }
        }
    }

    public abstract void execute();
}
