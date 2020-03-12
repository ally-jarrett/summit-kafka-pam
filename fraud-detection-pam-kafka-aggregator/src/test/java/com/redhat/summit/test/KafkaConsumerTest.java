package com.redhat.summit.test;

import com.redhat.summit.model.CreditCardAccount;
import com.redhat.summit.model.CreditCardTransaction;
import com.redhat.summit.serializers.CreditAccountDeserializer;
import com.redhat.summit.stream.Aggregation;
import com.redhat.summit.stream.InteractiveQuery;
import com.redhat.summit.test.util.KafkaTestResource;
import com.redhat.summit.test.util.KafkaTestUtil;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.streams.KafkaStreams;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@QuarkusTestResource(KafkaTestResource.class)
@QuarkusTest
public class KafkaConsumerTest {

    public static final String BLOCKED_ACCOUNT_TOPIC = "blocked-account-topic";

    @Inject
    KafkaTestUtil testUtil;

    @Inject
    KafkaStreams streams;

    @Inject
    InteractiveQuery query;

    @BeforeEach
    public void start() {
        streams.cleanUp();
        streams.start();
        // TODO : This doesnt work, it times out
        // await().atMost(100000, TimeUnit.MILLISECONDS).until(() -> streams.state().isRunning());
    }

    @AfterEach
    public void end() {
        streams.close();
        streams.cleanUp();
    }

    /**
     * Dummy Test of KafkaStreams to test moving of a message from 1 queue to another..
     *
     * @throws Exception
     */
    @Test
    public void testCardAccountBlockQuery() throws Exception {

        CreditCardAccount account = testUtil.getTestAccount();
        CreditCardTransaction tx = testUtil.buildCCTX(testUtil.getTestAccount(),
                (double) ThreadLocalRandom.current().nextInt(1, 10));

        // Send Blocked Account to GlobalKTable
        account.setBlocked(true);
        getAccountProducer().send(new ProducerRecord<>(BLOCKED_ACCOUNT_TOPIC, tx.getCardNumber(), account));

        Consumer<String, CreditCardAccount> consumer = this.getAccountConsumer();
        ConsumerRecords<String, CreditCardAccount> records = consumer.poll(Duration.ofMillis(5000));
        assertNotNull(records);
        assertEquals(1, records.count());
        assertTrue(records.iterator().next().value().isBlocked());
        assertTrue(query.isCardBlockEnabled(tx.getCardNumber()));

    }

    public void testCardAccountBlockTransaction() throws Exception {
        // TODO - Test blocked Account Transaction is sent to blocked queue
    }

    public void testCardAccountEligibleTransaction() throws Exception {
        // TODO - Test Non-blocked Account Transaction is send to eligble queue
    }

    private Producer<String, CreditCardAccount> getAccountProducer() {
        return new KafkaProducer<String, CreditCardAccount>(testUtil.baseProducerProps("blocked-account-producer"));
    }

    private Consumer<String, CreditCardAccount> getAccountConsumer() {
        Properties props = testUtil.baseConsumerProps("blocked-account-consumer");
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, CreditAccountDeserializer.class.getName());
        Consumer<String, CreditCardAccount> c = new KafkaConsumer<String, CreditCardAccount>(props);
        c.subscribe(Collections.singletonList(BLOCKED_ACCOUNT_TOPIC));
        return c;
    }

    private List<ConsumerRecord<String, Aggregation>> pollAggregateConsumer(Consumer<String, Aggregation> consumer, int expectedRecordCount) {

        int fetched = 0;
        List<ConsumerRecord<String, Aggregation>> result = new ArrayList<>();

        while (fetched < expectedRecordCount) {
            log.info("Poll fetched: {} records", fetched);
            ConsumerRecords<String, Aggregation> records = consumer.poll(Duration.ofMillis(5000));
            records.forEach(result::add);
            fetched = result.size();
        }

        return result;
    }


}
