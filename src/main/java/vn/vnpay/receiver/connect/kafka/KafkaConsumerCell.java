package vn.vnpay.receiver.connect.kafka;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;


@Slf4j
@Getter
@Setter
public class KafkaConsumerCell {
    private long relaxTime;
    private long timeOut;
    private boolean isClosed;
    private KafkaConsumer<String, String> consumer;

    public KafkaConsumerCell(Properties consumerProps, String consumerTopic) {
//        String memberId = String.valueOf(index);
//        consumerProps.setProperty(ConsumerConfig.GROUP_INSTANCE_ID_CONFIG, memberId);

        this.consumer = new KafkaConsumer<>(consumerProps);
        this.consumer.subscribe(Collections.singletonList(consumerTopic));
        log.info("create consumer {} - partition {} - topic {}", consumer.groupMetadata().groupInstanceId(), consumer.assignment(), consumerTopic);
    }
    public void close() {
        try {
            consumer.close();
            isClosed = true;
        } catch (Exception e) {
            log.warn("connection is closed: {0}", e);
        }
    }

    public ConsumerRecords<String, String> poll(Duration ofMillis) {
        return consumer.poll(ofMillis);
    }
}
