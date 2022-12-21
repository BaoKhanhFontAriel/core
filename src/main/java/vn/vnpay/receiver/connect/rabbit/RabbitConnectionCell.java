package vn.vnpay.receiver.connect.rabbit;

import com.rabbitmq.client.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import vn.vnpay.receiver.error.ErrorCode;
import vn.vnpay.receiver.model.ApiResponse;
import vn.vnpay.receiver.model.ApiRequest;
import vn.vnpay.receiver.runnable.LongRunningTask;
import vn.vnpay.receiver.runnable.PushToOracleRunnable;
import vn.vnpay.receiver.runnable.PushToRedisRunnale;
import vn.vnpay.receiver.utils.AppConfigSingleton;
import vn.vnpay.receiver.utils.ExecutorSingleton;
import vn.vnpay.receiver.utils.GsonSingleton;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;


@Setter
@Getter
@ToString
//@Slf4j
public class RabbitConnectionCell {
    private static final Logger log = LoggerFactory.getLogger(RabbitConnectionCell.class);

    public static final int TIME_SLEEP = AppConfigSingleton.getInstance().getIntProperty("time.sleep");
    public static final int TIME_OUT = AppConfigSingleton.getInstance().getIntProperty("time.out");
    private String queueName;
    private String exchangeName;
    private String exchangeType;
    private String routingKey;
    private long relaxTime;
    private long timeOut;
    private Connection conn;
    private Channel channel;

//    private volatile ApiResponse apiResponse;

    private AtomicReference<ApiResponse> apiResponse;

    private AtomicBoolean isSuccessful = new AtomicBoolean();

    public RabbitConnectionCell(ConnectionFactory factory, String exchangeName, String exchangeType, String routingKey, long relaxTime) {

        this.routingKey = routingKey;
        this.relaxTime = relaxTime;

        try {
            this.conn = factory.newConnection();
            this.channel = conn.createChannel();
            this.channel.exchangeDeclare(exchangeName, exchangeType);

            this.queueName = channel.queueDeclare().getQueue();
            this.channel.queueBind(queueName, exchangeName, routingKey);
//            channel.queuePurge(exchangeName);

        } catch (IOException | TimeoutException e) {
            log.error("fail connecting to rabbit : {0}", e);
        }
    }

    public void receiveAndSend() {
        // do when server receive request
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String json = new String(delivery.getBody(), "UTF-8");

            log.info("----");
            log.info("rabbit begin receiving data: {}", json);

            ApiRequest apiRequest = GsonSingleton.getInstance().getGson().fromJson(json, ApiRequest.class);
//            apiResponse = new ApiResponse("00", "success", apiRequest.getToken());
            isSuccessful.set(true);
            log.info("isSuccessful: {}", isSuccessful);


            // set up thread pool
            ScheduledExecutorService executor = ExecutorSingleton.getInstance().getExecutorService();

            // add runnable task for counting time
            Future timeOutFuture = executor.submit(new LongRunningTask());

            // add runnable for pushing to redis
            executor.submit(new PushToRedisRunnale(apiRequest));

            // add runnable for pushing to oracle
            executor.schedule(new PushToOracleRunnable(apiRequest, apiResponse), TIME_SLEEP, TimeUnit.MILLISECONDS);

            try {

                // set time to execute task under 1 minute
                // else throw TimeOutException
                timeOutFuture.get(TIME_OUT, TimeUnit.MILLISECONDS);

            } catch (TimeoutException e) {

                // assign response message for time out error
//                apiResponse = new ApiResponse(ErrorCode.TIME_OUT_ERROR, "fail: " + e, apiRequest.getToken());

                // save to receiver.log
                MDC.put("LOG_FILE", "receiver");
                log.error("Time execution in core is over 1 minute ", e);
                MDC.remove("LOG_FILE");

            } catch (ExecutionException | InterruptedException e) {

                log.error("fail to send message to api: ", e);
                apiResponse.set(new ApiResponse(ErrorCode.EXECUTION_ERROR, "fail: " + e, apiRequest.getToken()));

            } finally {

                // cancel all running futures
//                timeOutFuture.cancel(true);
//                redisFuture.cancel(true);
//                oracleFuture.cancel(true);

//                if (ExecutorSingleton.getInstance().getIsRedisFutureDone()
//                        && ExecutorSingleton.getInstance().getIsOracleFutureDone()){
//                    apiResponse = new ApiResponse("00", "success", apiRequest.getToken());
//                }
//
//                if (ExecutorSingleton.getInstance().getIsErrorHappened()) {
//                    apiResponse = new ApiResponse(ExecutorSingleton.getInstance().getError().getResCode(), "fail: " + ExecutorSingleton.getInstance().getError().getErrorMessage(), apiRequest.getToken());
//
//                    ExecutorSingleton.getInstance().setIsErrorHappened(false);
//                    ExecutorSingleton.getInstance().setError(null);
//                }

                // send message
                log.info("api response in rabbit: {}", apiResponse);
                log.info("isSuccessful: {}", isSuccessful.get());
                String message = GsonSingleton.getInstance().getGson().toJson(apiResponse);
                AMQP.BasicProperties replyProps = new AMQP.BasicProperties.Builder().correlationId(delivery.getProperties().getCorrelationId()).build();
                channel.basicPublish("", delivery.getProperties().getReplyTo(), replyProps, message.getBytes(StandardCharsets.UTF_8));
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);

//                ExecutorSingleton.getInstance().setIsRedisFutureDone(false);
//                ExecutorSingleton.getInstance().setIsOracleFutureDone(false);
            }
            log.info("rabbit finish receiving data");
        };

        try {
            channel.basicConsume(queueName, false, deliverCallback, (consumerTag -> {
            }));

        } catch (IOException e) {
            log.error("rabbit fail to receive data: {0}", e);
        }
    }

    public boolean isTimeOut() {
        if (System.currentTimeMillis() - this.relaxTime > this.timeOut) {
            return true;
        }
        return false;
    }

    public void close() {
        try {
            this.conn.close();
        } catch (Exception e) {
            log.warn("connection is closed: {0}", e);
        }
    }

    public boolean isClosed() throws Exception {
        return !conn.isOpen();
    }

}
