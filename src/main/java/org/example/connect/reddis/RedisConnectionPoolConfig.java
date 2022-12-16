package org.example.connect.reddis;

public class RedisConnectionPoolConfig {
    private static final String SERVER_NAME = "localhost";
    public static final int MAX_POOL_SIZE = 20;
    public static final int MIN_POOL_SIZE = 5;
    public static final int INIT_POOL_SIZE = 10;
    public static final String DB_PORT = "6379";

    public static final String URL =  "http://" + SERVER_NAME + ":" + DB_PORT;
    public static final long TIME_OUT = 200;
}
