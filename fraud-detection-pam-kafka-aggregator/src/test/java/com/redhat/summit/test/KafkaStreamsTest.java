package com.redhat.summit.test;

import com.redhat.summit.model.CreditCardTransaction;
import com.redhat.summit.stream.Aggregation;
import com.redhat.summit.stream.InteractiveQuery;
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
import org.apache.kafka.streams.KafkaStreams;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;


@Slf4j
@QuarkusTestResource(KafkaTestResource.class)
@QuarkusTest
public class KafkaStreamsTest {

    @Inject
    KafkaTestUtil testUtil;

    @Inject
    KafkaStreams streams;

    @Inject
    InteractiveQuery query;

    public static final String TRANSACTIONS_TOPIC = "transactions-topic";
    public static final String TRANSACTIONS_AGG_TOPIC = "transactions-aggregated";

    Consumer<String, Aggregation> aggregationConsumer;
    Producer<String, CreditCardTransaction> cctxProducer;

    @BeforeEach
    public void start() {
        streams.cleanUp();
        streams.start();
        // TODO : Fixme, I timeout and fail..
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
    public void testQuarkusKafkaStreamsProducerAndConsumer() throws Exception {

        // Generate 5 Transactions for same Account with varying values
        IntStream.range(0, 5).forEach(i -> {
            // Generate Amounts between 1-10
            CreditCardTransaction tx = testUtil.buildCCTX(testUtil.getTestAccount(),
                    (double) ThreadLocalRandom.current().nextInt(1, 10));
            log.info("Sending CreditCard Transaction Record: {}", i);
            getCctxProducer().send(new ProducerRecord<>(TRANSACTIONS_TOPIC, tx.getCardNumber(), tx));
        });

        // Setup Consumer of Aggregated Queue
        List<ConsumerRecord<String, Aggregation>> records = this.pollAggregateConsumer(getAggregationConsumer(), 1);

        //Expect 1 aggregated record of latest 5 transactions
        assertNotNull(records);
        assertEquals(1, records.size());

        // Pull Aggregated record
        ConsumerRecord<String, Aggregation> record = records.get(0);
        assertNotNull(record);
        assertNotNull(record.key());
        assertNotNull(record.value());

        // Assert Types
        assertTrue(record.value() instanceof Aggregation);
        assertNotNull(record.value().transactions);
        assertEquals(5, record.value().transactions.size());

    }


    private Consumer<String, Aggregation> getAggregationConsumer() {
        if (aggregationConsumer == null) {
            aggregationConsumer = new KafkaConsumer<String, Aggregation>(testUtil.baseConsumerProps("aggregation-consumer"));
            aggregationConsumer.subscribe(Collections.singletonList(TRANSACTIONS_AGG_TOPIC));
        }
        return aggregationConsumer;
    }

    private Producer<String, CreditCardTransaction> getCctxProducer() {
        if (cctxProducer == null) {
            cctxProducer = new KafkaProducer<String, CreditCardTransaction>(testUtil.baseProducerProps("transaction-producer"));
        }
        return cctxProducer;
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
