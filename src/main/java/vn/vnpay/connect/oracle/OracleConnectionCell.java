package vn.vnpay.connect.oracle;


import com.sun.org.slf4j.internal.Logger;
import com.sun.org.slf4j.internal.LoggerFactory;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;


@Getter
@Setter
@ToString
public class OracleConnectionCell {

    private static final Logger log  = LoggerFactory.getLogger(OracleConnectionCell.class);

    private String username;
    private String password;
    private String url;
    private long relaxTime;
    private long timeOut;
    private Connection conn;

    public OracleConnectionCell(String username, String password, String url, long relaxTime) {
        super();
        this.username = username;
        this.password = password;
        this.url = url;
        this.relaxTime = relaxTime;
        try {
            conn = DriverManager.getConnection(OracleConnectionPoolConfig.URL, OracleConnectionPoolConfig.USERNAME, OracleConnectionPoolConfig.PASSWORD);
        } catch (SQLException e) {
            log.error("fail connecting to database: {0} ", e);
        }
    }

    public boolean isTimeOut() {
        if(System.currentTimeMillis() - relaxTime > timeOut) {
            return true;
        }
        return false;
    }

    public void close() throws Exception{
        try {
            conn.close();
        } catch (Exception e) {
            log.warn("connection is closed: {0}", e);
        }
    }
    public boolean isClosed() throws Exception{
        return conn.isClosed();
    }

}
