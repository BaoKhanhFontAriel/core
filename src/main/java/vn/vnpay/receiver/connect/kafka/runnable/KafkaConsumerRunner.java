package vn.vnpay.receiver.connect.kafka.runnable;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import vn.vnpay.receiver.connect.kafka.KafkaConsumerConnectionCell;
import vn.vnpay.receiver.connect.kafka.KafkaConsumerConnectionPool;

import java.time.Duration;


//@Slf4j
//public class KafkaConsumerRunner implements Runnable{
//    private volatile String response;
//    @Override
//    public void run() {
//        response = null;
//        KafkaConsumerConnectionCell consumerCell = KafkaConsumerConnectionPool.getInstancePool().getConnection();
//        KafkaConsumer<String, String> consumer = consumerCell.getConsumer();
//
//        // polling
//        log.info("Kafka consumer {} waiting for message...", consumer.groupMetadata());
//        try {
//            while (true) {
//                synchronized (this){
//
//                }
//                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
//                for (ConsumerRecord<String, String> r : records) {
//                    log.info("----");
//                    log.info("rabbit begin receiving data: offset = {}, key = {}, value = {}",
//                            r.offset(), r.key(), r.value());
//                    response = (String) r.value();
//                }
//            }
//        } catch (Exception e) {
//            log.error("Unsuccessfully poll ", e);
//        }
//    }
//}
