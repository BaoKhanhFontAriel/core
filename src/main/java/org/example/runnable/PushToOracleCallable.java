package org.example.runnable;

import lombok.extern.slf4j.Slf4j;
import org.example.connect.oracle.OracleConnectionCell;
import org.example.connect.oracle.OracleConnectionPool;
import org.example.error.ErrorCode;
import org.example.error.ExecutorError;
import org.example.model.CustomerRequest;
import org.example.utils.ExecutorSingleton;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Date;
import java.util.concurrent.Callable;

@Slf4j
public class PushToOracleCallable implements Callable {

    private static OracleConnectionPool oracleConnectionPool = OracleConnectionPool.getInstancePool();

    private CustomerRequest customerRequest;
    private boolean isRunning = true;

    public PushToOracleCallable(CustomerRequest customerRequest) {
        this.customerRequest = customerRequest;
    }

    public void pushToDatabase() throws SQLException, ParseException, JSONException, InterruptedException {


        log.info("begin assigning json to variable");
        String data = customerRequest.getData();
        JSONObject jsonObject = new JSONObject(data);
        String customerName = jsonObject.getString("customerName");
        int rescode = jsonObject.getInt("rescode");
        int amount = jsonObject.getInt("amount");
        double debitAmount = jsonObject.getDouble("debitAmount");
        double realAmount = jsonObject.getDouble("realAmount");
        String payDate = jsonObject.getString("payDate");


        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        Date parsedDate = dateFormat.parse(payDate);
        Timestamp timestamp = new java.sql.Timestamp(parsedDate.getTime());

        log.info("successful assign json to variable");

        log.info("begin pushing to database");
        String sql = "CALL PKG_CUSTOMER.ADD_NEW_CUSTOMER(?,?,?,?,?,?,?,?)";

        OracleConnectionCell oracleConnectionCell = oracleConnectionPool.getConnection();
        PreparedStatement st = oracleConnectionCell.getConn().prepareStatement(sql);
        st.setDate(1, java.sql.Date.valueOf(LocalDate.now()));
        st.setTimestamp(2, timestamp);
        st.setDouble(3, realAmount);
        st.setDouble(4, debitAmount);
        st.setDouble(5, amount);
        st.setString(6, data);
        st.setLong(7, rescode);
        st.setString(8, customerName);
        st.execute();
        st.close();
        oracleConnectionPool.releaseConnection(oracleConnectionCell);

        log.info("push to database successfully!");

        ExecutorSingleton.getInstance().setIsAllTasksFinished(true);
    }


    @Override
    public Object call() throws RuntimeException {

        try {
            pushToDatabase();
        } catch (SQLException | ParseException | JSONException | InterruptedException e) {
            log.error("fail to push to database: ", e);
            ExecutorSingleton.getInstance().setIsErrorHappened(true);
            ExecutorSingleton.getInstance().setError(new ExecutorError(ErrorCode.ORACLE_ERROR, e.getMessage()));
        }
        return null;
    }
}
