package org.example.connect.rabbit;

import com.google.gson.Gson;
import com.rabbitmq.client.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.example.error.ErrorCode;
import org.example.runnable.LongRunningTask;
import org.example.runnable.PushToOracleCallable;
import org.example.runnable.PushToRedisCallable;
import org.example.model.ApiResponse;
import org.example.model.CustomerRequest;
import org.example.utils.AppConfig;
import org.example.utils.ExecutorSingleton;
import org.example.utils.GsonSingleton;

import java.io.IOException;
import java.text.ParseException;
import java.util.concurrent.*;


@Slf4j
@Setter
@Getter
@ToString
public class RabbitConnectionCell {

    private String queueName;
    private String exchangeName;

    private String exchangeType;

    private String routingKey;
    private long relaxTime;
    private long timeOut;
    private Connection conn;

    private Channel channel;

    public RabbitConnectionCell(ConnectionFactory factory, String exchangeName, String exchangeType, String routingKey, long relaxTime) {
//        super();
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
            log.info("fail connecting to rabbit : {0}", e);
        }
    }

    public void receiveAndSend() {
        // do when server receive request
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String json = new String(delivery.getBody(), "UTF-8");

            log.info("rabbit begin receiving data: {}", json);

            ApiResponse apiResponse = null;
            CustomerRequest customerRequest = GsonSingleton.getInstance().getGson().fromJson(json, CustomerRequest.class);
            String message = null;

            // set up thread pool
            ScheduledExecutorService executor = ExecutorSingleton.getInstance().getExecutorService();

            // add runnable task for counting time
            Future timeOutFuture = executor.submit(new LongRunningTask());

            // add runnable for pushing to redis
            Future redisFuture = executor.submit(new PushToRedisCallable(customerRequest));

            // add runnable for pushing to oracle
            Future oracleFuture = executor.schedule(new PushToOracleCallable(customerRequest), AppConfig.getTimeSleep(), TimeUnit.MILLISECONDS);


            try {
                // set time to execute task under 1 minute
                // else throw TimeOutException
                timeOutFuture.get(AppConfig.getTimeOut(), TimeUnit.MILLISECONDS);

                apiResponse = new ApiResponse("00", "success", customerRequest.getToken());
            } catch (TimeoutException e) {
                log.error("Time execution in core is over 1 minute ", e);
                timeOutFuture.cancel(true);
                redisFuture.cancel(true);
                oracleFuture.cancel(true);
                apiResponse = new ApiResponse(ErrorCode.TIME_OUT_ERROR, "fail: " + e, customerRequest.getToken());

            } catch (ParseException | ExecutionException | InterruptedException e) {
                log.error("fail to send message to api: ", e);
                apiResponse = new ApiResponse(ErrorCode.EXECUTION_ERROR, "fail: " + e, customerRequest.getToken());

            } finally {

                if (ExecutorSingleton.getInstance().getIsErrorHappened()) {
                    apiResponse = new ApiResponse(ExecutorSingleton.getInstance().getError().getResCode(),
                            "fail: " + ExecutorSingleton.getInstance().getError().getErrorMessage(),
                            customerRequest.getToken());

                    ExecutorSingleton.getInstance().setIsErrorHappened(false);
                    ExecutorSingleton.getInstance().setError(null);
                }

                if (ExecutorSingleton.getInstance().getIsAllTasksFinished()) {
                    ExecutorSingleton.getInstance().setIsAllTasksFinished(false);
                }

                // send message
                message = GsonSingleton.getInstance().getGson().toJson(apiResponse);
                AMQP.BasicProperties replyProps = new AMQP.BasicProperties
                        .Builder()
                        .correlationId(delivery.getProperties().getCorrelationId())
                        .build();
                channel.basicPublish("", delivery.getProperties().getReplyTo(), replyProps, message.getBytes("UTF-8"));
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
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
