package vn.vnpay.receiver.connect.kafka;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Properties;

@Slf4j
@Getter
@Setter
public class KafkaProducerCell {
    private long relaxTime;
    private long timeOut;
    private boolean isClosed;
    private org.apache.kafka.clients.producer.KafkaProducer<String, String> producer;
    private String producerTopic;
    public KafkaProducerCell(Properties producerConfig, String producerTopic) {
        this.producer = new org.apache.kafka.clients.producer.KafkaProducer<>(producerConfig);
        this.producerTopic = producerTopic;
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
