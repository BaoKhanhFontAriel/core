package org.example.thread;

public class ShutdownThread extends Thread{
    public void run() {
        System.out.println("shut down hook task completed..");
    }
}
