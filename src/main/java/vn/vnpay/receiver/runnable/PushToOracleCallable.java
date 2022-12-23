package vn.vnpay.receiver.runnable;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import vn.vnpay.receiver.connect.oracle.OracleConnectionCell;
import vn.vnpay.receiver.connect.oracle.OracleConnectionPool;
import vn.vnpay.receiver.error.ErrorCode;
import vn.vnpay.receiver.model.ApiRequest;
import vn.vnpay.receiver.model.ApiResponse;
import vn.vnpay.receiver.utils.ExecutorSingleton;
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
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Getter
public class PushToOracleCallable implements Callable<ApiResponse> {

    private static OracleConnectionPool oracleConnectionPool = OracleConnectionPool.getInstancePool();

    private ApiRequest apiRequest;
    OracleConnectionCell oracleConnectionCell = oracleConnectionPool.getConnection();

    public PushToOracleCallable(ApiRequest apiRequest, AtomicReference<ApiResponse> apiResponse) {
        this.apiRequest = apiRequest;
    }

    public void pushToDatabase() throws SQLException, ParseException, JSONException, InterruptedException {

        log.info("begin assigning json to variable");
        long start = System.currentTimeMillis();
        String data = apiRequest.getData();
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

        long end = System.currentTimeMillis();

        log.info("push to database successfully in {} ms", end - start);
    }

    @Override
    public ApiResponse call() throws SQLException, ParseException, JSONException, InterruptedException {
        ApiResponse response = null;
        try {
            pushToDatabase();
        } catch (Exception e) {
            log.error("fail to push to database: ", e);
            response = new ApiResponse(ErrorCode.ORACLE_ERROR, "fail: " + e.getMessage(), apiRequest.getToken());
            oracleConnectionPool.releaseConnection(oracleConnectionCell);
        }
        return response;
    }
}
