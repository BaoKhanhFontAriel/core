package vn.vnpay.receiver.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewPartitions;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import vn.vnpay.receiver.connect.kafka.*;
import vn.vnpay.receiver.model.ApiResponse;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class KafkaUtils {
    private static AtomicReference<LinkedList<String>> responses;
    static KafkaConsumerConnectionPool consumerPool = KafkaConsumerConnectionPool.getInstancePool();

    public static String sendAndReceive(String message) {
        send(message);
        String res = receive();
        return receive();
    }

    public static void receiveAndSend() {
        log.info("Kafka receive.........");
        for (KafkaConsumerConnectionCell consumerCell : consumerPool.getPool()) {
            ExecutorSingleton.getInstance().getExecutorService().submit((Runnable) () ->
            {
                while (true) {
//                    consumerCell.getConsumer().seekToEnd(consumerCell.getConsumer().assignment());
                    ConsumerRecords<String, String> records = consumerCell.getConsumer().poll(Duration.ofMillis(100));
                    for (ConsumerRecord<String, String> r : records) {
                        log.info("----");
                        log.info("kafka consumer id {} receive data: partition = {}, offset = {}, key = {}, value = {}",
                                consumerCell.getConsumer().groupMetadata().groupInstanceId(),
                                r.partition(),
                                r.offset(), r.key(), r.value());

                        ApiResponse apiResponse = DataUtils.uploadData(r.value());
                        res = GsonSingleton.toJson(apiResponse);
                        send(res);
                    }
                }
            });
        }
    }

    private static volatile String res = null;
    public static String receive() {
        log.info("Kafka receive.........");
        for (KafkaConsumerConnectionCell consumerCell : consumerPool.getPool()) {
            ExecutorSingleton.getInstance().getExecutorService().submit((Runnable) () ->
            {
                while (true) {
//                    consumerCell.getConsumer().seekToEnd(consumerCell.getConsumer().assignment());
                    ConsumerRecords<String, String> records = consumerCell.getConsumer().poll(Duration.ofMillis(100));
                    for (ConsumerRecord<String, String> r : records) {
                        log.info("----");
                        log.info("kafka consumer id {} receive data: partition = {}, offset = {}, key = {}, value = {}",
                                consumerCell.getConsumer().groupMetadata().groupInstanceId(),
                                r.partition(),
                                r.offset(), r.key(), r.value());

                        ApiResponse apiResponse = DataUtils.uploadData(r.value());
                        res = GsonSingleton.toJson(apiResponse);
                        return;
                    }
                }
            });
        }

        while (true) {
            if (res != null) {
                log.info("return response: {}", res);
                ExecutorSingleton.shutdownNow();
                ExecutorSingleton.wakeup();
                return res;
            }
        }
    }

    public static void send(String message){
        log.info("Kafka send {}.........", message);
        KafkaProducerConnectionCell producerCell = KafkaProducerConnectionPool.getInstancePool().getConnection();
        KafkaProducer<String, String> producer = producerCell.getProducer();
        // send message
        ProducerRecord<String, String> record = new ProducerRecord<>(KafkaConnectionPoolConfig.KAFKA_PRODUCER_TOPIC, message);
        producer.send(record, (recordMetadata, e) -> {
            if (e == null) {
                log.info("Kafka producer successfully send record as: Topic = {}, partition = {}, Offset = {}",
                        recordMetadata.topic(), recordMetadata.partition(), recordMetadata.offset());
            } else {
                log.error("Can't produce,getting error", e);
            }
        });

        KafkaProducerConnectionPool.getInstancePool().releaseConnection(producerCell);
    }

    public static void createNewTopic(String topic, int partition, short replica) {
        //        //create partition
        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:29092");
        AdminClient adminClient = AdminClient.create(props);

        NewTopic newTopic = new NewTopic(topic, partition, replica);
        adminClient.createTopics(Arrays.asList(newTopic));

//        Map<String, NewPartitions> newPartitionSet = new HashMap<>();
//        newPartitionSet.put(topic, NewPartitions.increaseTo(partition));
//        adminClient.createPartitions(newPartitionSet);

        adminClient.close();
    }
}
