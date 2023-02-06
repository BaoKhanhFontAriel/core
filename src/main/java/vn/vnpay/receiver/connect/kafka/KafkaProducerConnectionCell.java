package vn.vnpay.receiver.connect.kafka;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import vn.vnpay.receiver.model.ApiResponse;
import vn.vnpay.receiver.utils.DataUtils;
import vn.vnpay.receiver.utils.GsonSingleton;

import java.util.Arrays;
import java.util.Properties;

@Slf4j
@Getter
@Setter
public class KafkaProducerConnectionCell {
    private long relaxTime;
    private long timeOut;
    private boolean isClosed;
    private ApiResponse apiResponse;
    private KafkaProducer<String, String> producer;

    private String producerTopic;

    public KafkaProducerConnectionCell(Properties producerConfig, String producerTopic, long timeOut) {
        this.producer = new KafkaProducer<>(producerConfig);
        this.producerTopic = producerTopic;
    }

    public boolean isTimeOut() {
        if (System.currentTimeMillis() - this.relaxTime > this.timeOut) {
            return true;
        }
        return false;
    }

    public void close() {
        try {
            producer.close();
            isClosed = true;
        } catch (Exception e) {
            log.warn("connection is closed: {0}", e);
        }
    }
}
