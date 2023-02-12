package vn.vnpay.receiver.connect.kafka;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewPartitions;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import vn.vnpay.receiver.model.ApiResponse;
import vn.vnpay.receiver.utils.DataUtils;
import vn.vnpay.receiver.utils.GsonSingleton;

import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.TimeUnit;


@Slf4j
@Getter
@Setter
public class KafkaConsumerConnectionCell {
    private long relaxTime;
    private long timeOut;
    private boolean isClosed;
    private ApiResponse apiResponse;
    private KafkaConsumer<String, String> consumer;

    public KafkaConsumerConnectionCell(Properties consumerProps, String consumerTopic, long timeOut, int index) {

        String member_id = String.valueOf(index);
        consumerProps.setProperty(ConsumerConfig.GROUP_INSTANCE_ID_CONFIG, member_id);

        this.consumer = new KafkaConsumer<>(consumerProps);
        this.consumer.subscribe(Arrays.asList(consumerTopic));
        log.info("create consumer {} - partition {} - topic {}", consumer.groupMetadata().groupInstanceId(), consumer.assignment(), consumerTopic);
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

    public ConsumerRecords<String, String> poll(Duration ofMillis) {
        return consumer.poll(ofMillis);
    }
}
