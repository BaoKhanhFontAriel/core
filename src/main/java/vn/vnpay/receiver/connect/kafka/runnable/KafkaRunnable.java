package vn.vnpay.receiver.connect.kafka.runnable;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.protocol.types.Field;
import vn.vnpay.receiver.connect.kafka.*;
import vn.vnpay.receiver.model.ApiResponse;
import vn.vnpay.receiver.utils.DataUtils;
import vn.vnpay.receiver.utils.GsonSingleton;


@Slf4j
public class KafkaRunnable implements Runnable{
    @Override
    public void run() {
        receiveAndSend();
    }

    public void receiveAndSend() {
        log.info("kafka consumer receive message");
        KafkaConsumerConnectionCell consumerCell = KafkaConsumerConnectionPool.getInstancePool().getConnection();
        KafkaProducerConnectionCell producerCell = KafkaProducerConnectionPool.getInstancePool().getConnection();
        KafkaConsumer<String,String> consumer = consumerCell.getConsumer();
        KafkaProducer<String, String> producer = producerCell.getProducer();


        // receive message
        try{
            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(100);
                for (ConsumerRecord<String, String> record : records) {
                    log.info("kafka successfully receives data: offset = {}, key = {}, value = {}\n",
                            record.offset(), record.key(), record.value());
                    String stringJson = record.value();
                    ApiResponse apiResponse = DataUtils.uploadData(stringJson);
                    String res = GsonSingleton.getInstance().getGson().toJson(apiResponse);

                    // send message
                    ProducerRecord<String, String> producerRecord =
                            new ProducerRecord<>(KafkaConnectionPoolConfig.KAFKA_PRODUCER_TOPIC, res);
                    producer.send(producerRecord, (recordMetadata, e) -> {
                        if (e == null) {
                            log.info("kafka successfully sent the details as: Topic = {}, partition = {}, Offset = {}",
                                    recordMetadata.topic(), recordMetadata.partition(), recordMetadata.offset());
                        } else {
                            log.error("Can't produce,getting error", e);
                        }
                    });
                }
            }
        }
        catch (Exception e){
            log.error("Unsuccessfully poll ", e);
        }
        finally {

        }
    }
}
