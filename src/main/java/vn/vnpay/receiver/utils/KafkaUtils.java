package vn.vnpay.receiver.utils;

import com.google.gson.JsonSyntaxException;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.ConsumerGroupListing;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.MDC;
import vn.vnpay.receiver.connect.kafka.*;
import vn.vnpay.receiver.error.ErrorCode;
import vn.vnpay.receiver.model.ApiRequest;
import vn.vnpay.receiver.model.ApiResponse;
import vn.vnpay.receiver.model.PaymentRequest;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class KafkaUtils {
    public static void receiveAndSend() throws Exception {
        log.info("Kafka receive and send.........");

        while (true) {
            String res = receive();
            if (res != null) {
                ApiResponse response = null;
                ApiRequest apiRequest = null;
                MDC.put("token", TokenUtils.generateNewToken());

                try {
                    apiRequest = GsonSingleton.getInstance().getGson().fromJson(res, ApiRequest.class);
                    response = new ApiResponse("00", "success", apiRequest.getToken());
                    DataUtils.processData(apiRequest);
                }
                catch (JsonSyntaxException e){
                    response = new ApiResponse(ErrorCode.EXECUTION_ERROR, e.getMessage(), null);
                }
                catch (ExecutionException e) {
                    log.error(e.getMessage());
                    response = new ApiResponse(ErrorCode.EXECUTION_ERROR, "fail:" + e.getMessage(), apiRequest.getToken());
                } catch (TimeoutException e) {
                    log.error(e.getMessage());
                    response = new ApiResponse(ErrorCode.TIME_OUT_ERROR, "fail:" + e.getMessage(), apiRequest.getToken());
                } catch (InterruptedException e) {
                    log.error(e.getMessage());
                    response = new ApiResponse(ErrorCode.INTERRUPTED_ERROR, "fail:" + e.getMessage(), apiRequest.getToken());
                } finally {
                    MDC.remove("token");
                }

                // send message
                String message = GsonSingleton.toJson(response);
                log.info("send data is: {} ", message);
                send(message);
            }
        }
    }

    public static void receiveAndSendPayment() throws Exception {
        log.info("Kafka receive and send.........");

        while (true) {
            String res = receive();
            if (res != null) {
                ApiResponse response = null;
                try {
                    PaymentRequest paymentRequest = GsonSingleton.getInstance().getGson().fromJson(res, PaymentRequest.class);
                    response = new ApiResponse("00", "success", paymentRequest.getRequestid());
                }
                catch (JsonSyntaxException e){
                    response = new ApiResponse(ErrorCode.EXECUTION_ERROR, e.getMessage(), null);
                }
                // send message
                String message = GsonSingleton.toJson(response);
                log.info("send data is: {} ", message);
                send(message);
            }
        }
    }

    public static String receive() throws Exception {
        log.info("Kafka start receiving.........");
        return KafkaConsumerPool.getRecord();
    }

    public static void send(String message) throws Exception {
        log.info("Kafka send {}.........", message);
        KafkaProducerCell producerCell = KafkaProducerPool.getInstancePool().getConnection();
        org.apache.kafka.clients.producer.KafkaProducer<String, String> producer = producerCell.getProducer();
        // send message

        ProducerRecord<String, String> record = new ProducerRecord<>(producerCell.getProducerTopic(), message);

        try {
            producer.send(record, (recordMetadata, e) -> {
                if (e == null) {
                    log.info("Kafka producer successfully send record as: Topic = {}, partition = {}, Offset = {}",
                            recordMetadata.topic(), recordMetadata.partition(), recordMetadata.offset());

                } else {
                    log.error("Can't produce,getting error", e);
                }
            });
        } catch (Exception e) {
            throw new Exception("Kafka can not produce message");
        }


        KafkaProducerPool.getInstancePool().releaseConnection(producerCell);
    }

    private static AdminClient adminClient;

    public static void createNewTopic(String topic, int partition, short replica) {
        //        //create partition
        if (adminClient == null){
            Properties props = new Properties();
            props.put("bootstrap.servers", "localhost:29092");
            adminClient = AdminClient.create(props);
        }
        NewTopic newTopic = new NewTopic(topic, partition, replica);
        adminClient.createTopics(Arrays.asList(newTopic));
        adminClient.close();
    }
}
