package com.redhat.summit.test;

import com.redhat.summit.model.CreditCardTransaction;
import com.redhat.summit.model.GetAggregatedTransactionsResult;
import com.redhat.summit.stream.InteractiveQuery;
import com.redhat.summit.test.util.KafkaTestResource;
import com.redhat.summit.test.util.KafkaTestUtil;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

@Slf4j
@QuarkusTestResource(KafkaTestResource.class)
@QuarkusTest
public class KafkaStreamsTest {

    public static final String TRANSACTION_ENDPOINT = "transactions-topic";

    @Inject
    KafkaTestUtil kafkaTestUtil;

    @Inject
    InteractiveQuery interactiveQueries;

    @Inject
    KafkaTestUtil testUtil;

    //@Test
    public void testKafkaAggregateStream() throws Exception {

        Producer<String, CreditCardTransaction> producer =
                new KafkaProducer<String, CreditCardTransaction>(testUtil.baseProducerProps(TRANSACTION_ENDPOINT));

        // Generate 5 Transactions for same Account with varying values
        IntStream.range(0, 5).forEach(i -> {
            CreditCardTransaction tx = kafkaTestUtil.buildCCTX(kafkaTestUtil.getTestAccount(),
                    (double) ThreadLocalRandom.current().nextInt(1, 10));
            producer.send(new ProducerRecord<>(TRANSACTION_ENDPOINT, tx.getCardNumber(), tx));
        });

        GetAggregatedTransactionsResult result = interactiveQueries.getAggregatedTransactions(kafkaTestUtil.getTransactionKey());
        Assert.assertNotNull(result);
    }
}