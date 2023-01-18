package vn.vnpay.receiver.connect.kafka;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import vn.vnpay.receiver.model.ApiResponse;
import vn.vnpay.receiver.utils.DataUtils;

import java.util.Arrays;
import java.util.Properties;

@Slf4j
@Setter
@Getter
public class KafkaConnectionCell {
    private long relaxTime;
    private long timeOut;
    private boolean isClosed;
    private ApiResponse apiResponse;
    private KafkaConsumer<String, String> consumer;

    private KafkaProducer<String, String> producer;

    public KafkaConnectionCell(Properties consumerProps, Properties producerConfig, String topic, long timeOut) {
        consumer = new KafkaConsumer<>(consumerProps);

        producer = new KafkaProducer<String, String>(producerConfig);

        consumer.subscribe(Arrays.asList(topic));
        log.info("Subscribed to topic " + topic);
    }

    public void receiveAndSend() {
        log.info("----");
        // receive message
        // polling
        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(100);
            for (ConsumerRecord<String, String> record : records) {
                log.info("rabbit begin receiving data: offset = %d, key = %s, value = %s\n",
                        record.offset(), record.key(), record.value());
                String stringJson = record.value();
//                apiResponse = DataUtils.uploadData(stringJson);
            }
        }

        // send message
//        log.info("rabbit start publishing data");
//        String message = GsonSingleton.getInstance().getGson().toJson(apiResponse);
//        ProducerRecord<String, String> record = new ProducerRecord<>("reply-topic", message);
//        producer.send(record, (recordMetadata, e) -> {
//            if (e == null) {
//                log.info("Successfully received the details as: \n" +
//                        "Topic:" + recordMetadata.topic() + "\n" +
//                        "Partition:" + recordMetadata.partition() + "\n" +
//                        "Offset" + recordMetadata.offset() + "\n" +
//                        "Timestamp" + recordMetadata.timestamp());
//            } else {
//                log.error("Can't produce,getting error", e);
//            }
//        });
    }

    public boolean isTimeOut() {
        if (System.currentTimeMillis() - this.relaxTime > this.timeOut) {
            return true;
        }
        return false;
    }

    public void close() {
        try {
            consumer.close();
            isClosed = true;
        } catch (Exception e) {
            log.warn("connection is closed: {0}", e);
        }
    }
}
