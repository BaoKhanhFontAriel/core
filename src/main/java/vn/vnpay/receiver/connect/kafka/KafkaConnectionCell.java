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
@Setter
@Getter
public class KafkaConnectionCell {
    private long relaxTime;
    private long timeOut;
    private boolean isClosed;
    private ApiResponse apiResponse;
    private KafkaConsumer<String, String> consumer;
    private KafkaProducer<String, String> producer;

    private String producerTopic;

    public KafkaConnectionCell(Properties consumerProps, Properties producerConfig, String consumerTopic, String producerTopic, long timeOut) {
        this.consumer = new KafkaConsumer<>(consumerProps);
        this.producer = new KafkaProducer<>(producerConfig);
        this.producerTopic = producerTopic;
        this.consumer.subscribe(Arrays.asList(consumerTopic));
        log.info("Subscribed to topic " + consumerTopic);
    }

    public KafkaConnectionCell() {
        String consumerTopic = KafkaConnectionPoolConfig.KAFKA_CONSUMER_TOPIC;
        String producerTopic = KafkaConnectionPoolConfig.KAFKA_PRODUCER_TOPIC;
        String bootstrapServers = KafkaConnectionPoolConfig.KAFKA_SERVER;
        String grp_id = "khanh-group-3";


        Properties consumerProps = new Properties();
        consumerProps.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        consumerProps.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.setProperty(ConsumerConfig.GROUP_ID_CONFIG, grp_id);
        consumerProps.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");

        Properties producerProps = new Properties();
        producerProps.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        producerProps.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        this.consumer = new KafkaConsumer<>(consumerProps);
        this.producer = new KafkaProducer<>(producerProps);
        this.producerTopic = producerTopic;
        this.consumer.subscribe(Arrays.asList(consumerTopic));
        log.info("Subscribed to topic " + consumerTopic);
    }

    public void receiveAndSend() {
        log.info("----");
        // receive message
        // polling
        try {
            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(100);
                for (ConsumerRecord<String, String> record : records) {
                    log.info("kafka successfully receives data: offset = {}, key = {}, value = {}\n",
                            record.offset(), record.key(), record.value());
                    String stringJson = record.value();
                    apiResponse = DataUtils.uploadData(stringJson);
                    String message = GsonSingleton.getInstance().getGson().toJson(apiResponse);

                    // send message
                    log.info("kafka start publishing data");
                    ProducerRecord<String, String> producerRecord = new ProducerRecord<>(producerTopic, message);
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
        } catch (WakeupException we) {
            log.error("consumer cant poll results ", we);
        }
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
            consumer.wakeup();
        } catch (Exception e) {
            log.warn("connection is closed: {0}", e);
        }
    }
}
