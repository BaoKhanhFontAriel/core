package vn.vnpay.receiver.thread;


import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ShutdownThread extends Thread{
    public void run() {
        System.out.println("shut down hook task completed..");
    }
}
