package vn.vnpay.receiver.connect.kafka.runnable;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import vn.vnpay.receiver.connect.kafka.KafkaConnectionPoolConfig;
import vn.vnpay.receiver.connect.kafka.KafkaProducerConnectionCell;
import vn.vnpay.receiver.connect.kafka.KafkaProducerConnectionPool;
import vn.vnpay.receiver.model.ApiResponse;

import java.util.concurrent.Callable;

@Slf4j
public class KafkaProducerRunner implements Runnable {
    private volatile String response;
    private ApiResponse message;

    public KafkaProducerRunner(ApiResponse message) {
        this.message = message;
    }

    @Override
    public void run() {
        KafkaProducerConnectionCell producerCell = KafkaProducerConnectionPool.getInstancePool().getConnection();
        KafkaProducer<String, ApiResponse> producer = producerCell.getProducer();

        ProducerRecord<String, ApiResponse> record = new ProducerRecord<>(KafkaConnectionPoolConfig.KAFKA_PRODUCER_TOPIC, message);
        producer.send(record, (recordMetadata, e) -> {
            if (e == null) {
                log.info("Kafka producer successfully send record ot topic {} - partition {}",
                        recordMetadata.topic(),
                        record.partition());
            } else {
                log.error("Can't produce,getting error", e);
            }
        });
    }
}
