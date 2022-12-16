package org.example.connect.oracle;

public class OracleConnectionPoolConfig {
    private static final String SERVER_NAME = "10.22.19.192";
    public static final int MAX_POOL_SIZE = 20;
    public static final int MIN_POOL_SIZE = 5;
    public static final int INIT_POOL_SIZE = 10;
    public static final String DB_PORT = "1521";
    public static final String USERNAME = "NHSVBAOFF";
    public static final String PASSWORD = "NHSVBAOFF";
    public static final String SID = "OFFLINE";
    public static final String URL =  "jdbc:oracle:thin:@" + SERVER_NAME + ":" + DB_PORT + ":" + SID;
    public static final long TIME_OUT = 200;
}
