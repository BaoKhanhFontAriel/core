package vn.vnpay.receiver.connect.rabbit;

import com.rabbitmq.client.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.vnpay.receiver.model.ApiResponse;
import vn.vnpay.receiver.utils.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;


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
    private ApiResponse apiResponse;

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

//    public void receiveAndSend() {
//        // do when server receive request
//        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
//            String json = new String(delivery.getBody(), "UTF-8");
//
//            log.info("----");
//            log.info("rabbit begin receiving data: {}", json);
//
//            apiResponse = DataUtils.processData(json);
//
//            // send message
//            log.info("rabbit start publishing data");
//            String message = GsonSingleton.getInstance().getGson().toJson(apiResponse);
//            AMQP.BasicProperties replyProps = new AMQP.BasicProperties.Builder().correlationId(delivery.getProperties().getCorrelationId()).build();
//
//            try {
//                channel.basicPublish("", delivery.getProperties().getReplyTo(), replyProps, message.getBytes(StandardCharsets.UTF_8));
//
//                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
//            } catch (IOException e) {
//                log.error("rabbit fail to publish data: ", e);
//            }
//        };
//
//        try {
//            channel.basicConsume(queueName, false, deliverCallback, (consumerTag -> {
//            }));
//
//        } catch (IOException e) {
//            log.error("rabbit fail to consume data: ", e);
//        }
//
//        log.info("rabbit finish consuming data");
//    }

    public boolean isTimeOut() {
        if (System.currentTimeMillis() - this.relaxTime > this.timeOut) {
            return true;
        }
        return false;
    }

    public void close() {
        try {
            this.channel.close();
            this.conn.close();
        } catch (Exception e) {
            log.warn("connection is closed: {0}", e);
        }
    }

    public boolean isClosed() throws Exception {
        return !conn.isOpen();
    }
}
