package vn.vnpay.receiver.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.MDC;
import vn.vnpay.receiver.connect.kafka.*;
import vn.vnpay.receiver.error.ErrorCode;
import vn.vnpay.receiver.exceptions.OracleDataPushException;
import vn.vnpay.receiver.model.ApiRequest;
import vn.vnpay.receiver.model.ApiResponse;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class KafkaUtils {
    private static AtomicReference<LinkedList<String>> responses;
    static KafkaConsumerConnectionPool consumerPool = KafkaConsumerConnectionPool.getInstancePool();

    public static void receiveAndSend() throws Exception {
        log.info("Kafka receive and send.........");

        while (true) {
            String res = receive();
            if (res != null){
                ApiRequest apiRequest = GsonSingleton.getInstance().getGson().fromJson(res, ApiRequest.class);
                ApiResponse response = new ApiResponse("00", "success", apiRequest.getToken());

                // oracle, redis
                MDC.put("token", TokenUtils.generateNewToken());
                try {
                    DataUtils.processData(apiRequest);
                }
                catch (ExecutionException e){
                    log.error(e.getMessage());
                    response = new ApiResponse(ErrorCode.EXECUTION_ERROR, "fail:" + e.getMessage(), apiRequest.getToken());
                }
                catch (TimeoutException e){
                    log.error(e.getMessage());
                    response = new ApiResponse(ErrorCode.TIME_OUT_ERROR, "fail:" + e.getMessage(), apiRequest.getToken());
                }
                catch (InterruptedException e){
                    log.error(e.getMessage());
                    response = new ApiResponse(ErrorCode.INTERRUPTED_ERROR, "fail:" + e.getMessage(), apiRequest.getToken());
                }
                finally {
                    MDC.remove("token");
                }

                // send message
                String message = GsonSingleton.toJson(response);
                log.info("send data is: {} ", message);

                if (message != null) {
                    send(message);
                }
//                else {
//                    send(
//                            GsonSingleton.toJson(
//                                    new ApiResponse(ErrorCode.TIME_OUT_ERROR,
//                                            "Kafka of Core send empty message",
//                                            apiRequest.getToken())));
//                }

            }
        }
    }

    public static String receive() throws Exception {
        log.info("Kafka start receiving.........");
        return KafkaConsumerConnectionPool.getRecord();
    }

    public static void send(String message) throws Exception {
        log.info("Kafka send {}.........", message);
        KafkaProducerConnectionCell producerCell = KafkaProducerConnectionPool.getInstancePool().getConnection();
        KafkaProducer<String, String> producer = producerCell.getProducer();
        // send message

        ProducerRecord<String, String> record = new ProducerRecord<>(KafkaConnectionPoolConfig.KAFKA_PRODUCER_TOPIC, message);

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


        KafkaProducerConnectionPool.getInstancePool().releaseConnection(producerCell);
    }

    public static void createNewTopic(String topic, int partition, short replica) {
        //        //create partition
        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:29092");
        AdminClient adminClient = AdminClient.create(props);

        NewTopic newTopic = new NewTopic(topic, partition, replica);
        adminClient.createTopics(Arrays.asList(newTopic));

        adminClient.close();
    }
}
