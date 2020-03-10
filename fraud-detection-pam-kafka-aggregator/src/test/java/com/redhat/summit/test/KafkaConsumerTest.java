package com.redhat.summit.test;

import com.redhat.summit.model.CreditCardTransaction;
import com.redhat.summit.test.util.KafkaTestResource;
import com.redhat.summit.test.util.KafkaTestUtil;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.time.Duration;
import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TODO : Delete me when fixed
 */
@Slf4j
@QuarkusTestResource(KafkaTestResource.class)
@QuarkusTest
public class KafkaConsumerTest {

    @ConfigProperty(name = "kafka.bootstrap.servers")
    String bootstrap;

    @ConfigProperty(name = "redhat.summit.kafka.test.producer.topic")
    String producerTopic;

    @ConfigProperty(name = "redhat.summit.kafka.test.consumer.topic")
    String consumerTopic;

    @Inject
    KafkaTestUtil testUtil;

    /**
     * Dummy Test of KafkaStreams to test moving of a message from 1 queue to another..
     *
     * @throws Exception
     */
    @Test
    public void testQuarkusKafkaStreamsProducerAndConsumer() throws Exception {

        CreditCardTransaction tx = new CreditCardTransaction();
        tx.setCardNumber(UUID.randomUUID().toString());
        tx.setCardHolderName("Ally");
        tx.setAmount(100.00);
        tx.setTransactionReference(UUID.randomUUID().toString());

        Producer<String, CreditCardTransaction> producer =
                new KafkaProducer<String, CreditCardTransaction>(testUtil.baseProducerProps(producerTopic));
        producer.send(new ProducerRecord<>(producerTopic, tx.getCardNumber(), tx));

        Consumer<String, CreditCardTransaction> consumer =
                new KafkaConsumer<String, CreditCardTransaction>(testUtil.baseConsumerProps(consumerTopic));
        consumer.subscribe(Collections.singletonList(consumerTopic));
        ConsumerRecords<String, CreditCardTransaction> records = consumer.poll(Duration.ofMillis(5000));

        assertNotNull(records);
        log.info("MESSAGE COUNT: {} ", records.count());
        assertEquals(1, records.count());

        ConsumerRecord<String, CreditCardTransaction> record = records.iterator().next();
        assertNotNull(record);

        log.info("MESSAGE VALUE: ", record.value());
        assertTrue(record.value() instanceof CreditCardTransaction);
        assertEquals(tx.getCardNumber(), record.value().getCardNumber());
        assertEquals(tx.getCardHolderName(), record.value().getCardHolderName());
        assertEquals(tx.getAmount(), record.value().getAmount());
        assertEquals(tx.getTransactionReference(), record.value().getTransactionReference());
    }

}
