package vn.vnpay.receiver.connect.kafka.runnable;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import vn.vnpay.receiver.connect.kafka.KafkaConnectionPoolConfig;
import vn.vnpay.receiver.connect.kafka.KafkaProducerConnectionCell;
import vn.vnpay.receiver.connect.kafka.KafkaProducerConnectionPool;
import vn.vnpay.receiver.model.ApiResponse;
import vn.vnpay.receiver.utils.GsonSingleton;

@Slf4j
public class KafkaProducerRunner implements Runnable {
    private volatile String response;
    private ApiResponse message;

    public KafkaProducerRunner(ApiResponse message) {
        this.message = message;
    }

    @Override
    public void run() {
        log.info("Get Kafka Producer conenction...");
        KafkaProducerConnectionCell producerCell = KafkaProducerConnectionPool.getInstancePool().getConnection();
        String res = GsonSingleton.getInstance().getGson().toJson(message);
        KafkaProducer<String, String> producer = producerCell.getProducer();
        ProducerRecord<String, String> record = new ProducerRecord<>(KafkaConnectionPoolConfig.KAFKA_PRODUCER_TOPIC, res);
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
