package vn.vnpay.receiver.runnable;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import vn.vnpay.receiver.connect.oracle.OracleConnectionCell;
import vn.vnpay.receiver.connect.oracle.OracleConnectionPool;
import vn.vnpay.receiver.error.ErrorCode;
import vn.vnpay.receiver.exceptions.OracleDataPushException;
import vn.vnpay.receiver.model.ApiRequest;
import vn.vnpay.receiver.model.ApiResponse;
import org.json.JSONException;
import org.json.JSONObject;
import vn.vnpay.receiver.utils.TokenUtils;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Date;
import java.util.concurrent.Callable;

@Slf4j
@Getter
public class PushToOracleCallable implements Callable<ApiResponse> {

    private static OracleConnectionPool oracleConnectionPool = OracleConnectionPool.getInstancePool();

    private ApiRequest apiRequest;
    OracleConnectionCell oracleConnectionCell = oracleConnectionPool.getConnection();

    public PushToOracleCallable(ApiRequest apiRequest) {
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
    public ApiResponse call() throws OracleDataPushException {
        ApiResponse response = null;
//        try {
//            pushToDatabase();
//        } catch (SQLException | ParseException | JSONException | InterruptedException e) {
//            MDC.put("token", TokenUtils.generateNewToken());
//            log.error("fail to push to database: ", e);
//            MDC.remove("token");
//            response = new ApiResponse(ErrorCode.ORACLE_ERROR, "fail: " + e.getMessage(), apiRequest.getToken());
//        }
//        finally {
//            oracleConnectionPool.releaseConnection(oracleConnectionCell);
//        }
        try {
            pushToDatabase();
        } catch (SQLException | ParseException | InterruptedException e) {
            log.error("fail to push to database: ", e);
            throw new OracleDataPushException("fail to push to oracle: " + e.getMessage());
        } finally {
            oracleConnectionPool.releaseConnection(oracleConnectionCell);
        }
        return response;
    }
}
