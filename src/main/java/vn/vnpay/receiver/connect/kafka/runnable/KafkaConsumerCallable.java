package vn.vnpay.receiver.connect.kafka.runnable;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import vn.vnpay.receiver.connect.kafka.KafkaConsumerConnectionCell;
import vn.vnpay.receiver.connect.kafka.KafkaConsumerConnectionPool;
import vn.vnpay.receiver.model.ApiResponse;
import vn.vnpay.receiver.utils.DataUtils;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.zip.DataFormatException;


@Slf4j
public class KafkaConsumerCallable implements Callable<ApiResponse> {

    public void shutdown(){

    }

    @Override
    public ApiResponse call() throws Exception {
        ApiResponse response = null;
        KafkaConsumerConnectionCell consumerCell = KafkaConsumerConnectionPool.getInstancePool().getConnection();
        KafkaConsumer<String, String> consumer = consumerCell.getConsumer();

        // polling
        log.info("Get Kafka consumer {} - partition {}", consumer.groupMetadata().groupId(), consumer.assignment());
        try {
            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
                for (ConsumerRecord<String, String> r : records) {
                    log.info("----");
                    log.info("rabbit begin receiving data: offset = {}, key = {}, value = {}",
                            r.offset(), r.key(), r.value());
                    response = DataUtils.uploadData(r.value());
                    return response;
                }
            }
        } catch (Exception e) {
            log.error("Unsuccessfully poll ", e);
        }
        return response;
    }
}
