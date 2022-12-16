package org.example.connect.rabbit;

public class RabbitConnectionPoolConfig {


    private static final String SERVER_NAME = "localhost";
    public static final int MAX_POOL_SIZE = 20;
    public static final int MIN_POOL_SIZE = 5;
    public static final int INIT_POOL_SIZE = 10;
    public static final String DB_PORT = "1521";
    public static final String QUEUENAME = "sender";
    public static final String EXHCHANGENAME = "sender_exchange";

    public static final String EXHCHANGETYPE = "direct";

    public static final String ROUTING_KEY = "sender_info";
    public static final String SID = "OFFLINE";
    public static final String URL =  "jdbc:oracle:thin:@" + SERVER_NAME + ":" + DB_PORT + ":" + SID;
    public static final long TIME_OUT = 200;
}
