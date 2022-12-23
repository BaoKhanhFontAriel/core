package vn.vnpay.receiver.connect.rabbit;

import com.rabbitmq.client.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.slf4j.MDC;
import vn.vnpay.receiver.error.ErrorCode;
import vn.vnpay.receiver.model.ApiResponse;
import vn.vnpay.receiver.model.ApiRequest;
import vn.vnpay.receiver.runnable.PushToOracleCallable;
import vn.vnpay.receiver.runnable.PushToRedisCallable;
import vn.vnpay.receiver.utils.AppConfigSingleton;
import vn.vnpay.receiver.utils.ExecutorSingleton;
import vn.vnpay.receiver.utils.GsonSingleton;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


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
    private volatile Channel channel;
    private AtomicReference<ApiResponse> apiResponse = new AtomicReference<>();

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
            apiResponse.set(new ApiResponse("00", "success", apiRequest.getToken()));

            // set up thread pool
            ScheduledExecutorService executor = ExecutorSingleton.getInstance().getExecutorService();
            // add runnable for pushing to redis
            Future redisFuture = executor.submit(new PushToRedisCallable(apiRequest, apiResponse));
            // add runnable for pushing to oracle
            Future oracleFuture = executor.schedule(new PushToOracleCallable(apiRequest, apiResponse), TIME_SLEEP, TimeUnit.MILLISECONDS);

            List<Future> futureList = new ArrayList<>();
            futureList.add(redisFuture);
            futureList.add(oracleFuture);

            // concurrency for redis and oracle
            // redis -> oracle
            for (Future f : futureList) {
                try {
                    ApiResponse response = (ApiResponse) f.get(TIME_OUT, TimeUnit.MILLISECONDS);
                    if (response != null) {
                        apiResponse.set(response);
                        break;
                    }
                } catch (InterruptedException e) {
                    log.info("{} has InterruptedException: {}", Thread.currentThread().getName(), e.getMessage());
                    apiResponse.set(new ApiResponse(ErrorCode.INTERRUPTED_ERROR, "fail: " + e.getMessage(), apiRequest.getToken()));
                } catch (ExecutionException e) {
                    log.info("{} has execution error: {}", Thread.currentThread().getName(), e.getMessage());
                    apiResponse.set(new ApiResponse(ErrorCode.EXECUTION_ERROR, "fail: " + e.getMessage(), apiRequest.getToken()));
                } catch (TimeoutException e) {
                    log.info("Time execution in core is over 1 minute: ", e);
                    apiResponse.set(new ApiResponse(ErrorCode.TIME_OUT_ERROR, "fail: " + e, apiRequest.getToken()));
                }
            }

            // send message
            String message = GsonSingleton.getInstance().getGson().toJson(apiResponse.get());
            AMQP.BasicProperties replyProps = new AMQP.BasicProperties.Builder()
                    .correlationId(delivery.getProperties().getCorrelationId())
                    .build();

            try {
                channel.basicPublish(
                        "",
                        delivery.getProperties().getReplyTo(),
                        replyProps,
                        message.getBytes(StandardCharsets.UTF_8));

                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };

        try {
            channel.basicConsume(queueName, false, deliverCallback, (consumerTag -> {
            }));

        } catch (IOException e) {
            log.error("rabbit fail to receive data: {0}", e);
        }

        log.info("rabbit finish receiving data");
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
