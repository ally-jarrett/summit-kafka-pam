package com.redhat.summit.test;

import com.redhat.summit.KafkaProducer;
import com.redhat.summit.model.CreditCardAccount;
import com.redhat.summit.model.CreditCardTransaction;
import com.redhat.summit.model.Merchant;
import com.redhat.summit.model.TransactionEventConfiguration;
import com.redhat.summit.test.util.KafkaTestResource;
import com.redhat.summit.test.util.KafkaTestUtil;
import com.redhat.summit.sede.CreditCardDeserializer;
import com.redhat.summit.util.KafkaUtils;
import com.redhat.summit.util.TransactionEventProducer;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.codehaus.plexus.util.StringUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.After;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import javax.inject.Inject;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@QuarkusTestResource(KafkaTestResource.class)
@QuarkusTest
public class KafkaConsumerTest {

    private AdminClient adminClient;
    private Consumer<String, CreditCardTransaction> consumer;
    private Map<String, CreditCardTransaction> consumedRecords;
    private TransactionEventConfiguration eventConfiguration;
    private TransactionEventConfiguration configuredEventConfiguration;

    @Inject
    KafkaUtils kafkaUtils;

    @Inject
    TransactionEventProducer eventProducer;

    @Inject
    KafkaProducer kafkaProducer;

    @Inject
    KafkaTestUtil kafkaTestUtil;

    @ConfigProperty(name = "kafka.bootstrap.servers")
    String bootstrap;

    @ConfigProperty(name = "redhat.summit.kafka.test.producer.topic")
    String producerTopic;

    @Test
    @Timeout(value = 20, unit = TimeUnit.SECONDS)
    public void testKafkaGenericTransactionProducer() throws Exception {

        // Setup Consumer
        consumedRecords = new HashMap<>();

        // Poll for first 10 produced records,
        boolean poll = true;
        while (poll) {
            ConsumerRecords<String, CreditCardTransaction> records = getConsumer(producerTopic).poll(Duration.ofMillis(5000));
            records.forEach(r -> consumedRecords.put(r.key(), r.value()));

            log.info("Consumer Records: {}", consumedRecords.size());
            if (consumedRecords.size() >= 10) {
                poll = false;
            }
        }

        // Stop on first message
        assertNotNull(consumedRecords);
        assertEquals(10, consumedRecords.size());

        // Test Each Record is sourced from known accounts
        consumedRecords.entrySet().forEach(e -> {

            String key = e.getKey();
            CreditCardTransaction tx = e.getValue();

            assertNotNull(key);
            assertNotNull(tx);
            assertTrue(tx instanceof CreditCardTransaction);
            assertNotNull(tx.getCardNumber());
            this.assertTransaction(key, tx);
        });
    }

    @After
    public void end() {
        consumer.unsubscribe();
        consumer = null;
        consumedRecords = null;
    }

    @Test
    @Timeout(value = 20, unit = TimeUnit.SECONDS)
    public void testKafkaCustomTransactionProducer() throws Exception {

        final int TIMEFRAME = 10;

        // Setup Consumer
        List<ConsumerRecord<String, CreditCardTransaction>> customTx = new ArrayList<>();

        eventConfiguration = kafkaTestUtil.getEventConfiguration();
        eventConfiguration.setTimeframe(TIMEFRAME + "s");
        configuredEventConfiguration = eventProducer.processEventConfiguration(eventConfiguration);

        assertNotNull(configuredEventConfiguration);
        assertNotNull(configuredEventConfiguration.getCustomTransactions());
        assertEquals(kafkaTestUtil.EXPECTED_MESSAGE_COUNT, configuredEventConfiguration.getCustomTransactions().size());

        // Assert Calculated Timeframe
        long timeInMillis = TimeUnit.SECONDS.toMillis(10);
        long delta = timeInMillis / configuredEventConfiguration.getTotalTransactionCount();
        assertNotNull(configuredEventConfiguration.getIntervalInMs());
        assertEquals(delta, configuredEventConfiguration.getIntervalInMs());

        // Flowable should place messages on BlockingQueue as per at a rate set by configuredEventConfiguration.getIntervalInMs()
        // Run as an Async Process to mimic realtime consumption
        kafkaProducer.injectCustomTransactionsTemp(configuredEventConfiguration);

        // Poll for custom records
        boolean poll = true;
        while (poll) {
            ConsumerRecords<String, CreditCardTransaction> records = getConsumer("custom.transactions")
                    .poll(Duration.ofMillis(TimeUnit.SECONDS.toMillis(1)));

            // Add only the keys of our Custom Transaction Card
            records.forEach(r -> {
                if (StringUtils.equals(r.key(), kafkaTestUtil.getTransactionKey())) {
                    customTx.add(r);
                }
            });

            log.info("Custom Consumer Records: {}", customTx.size());
            if (customTx.size() >= 10) {
                poll = false;
            }
        }

        // Stop after 10 messages consumed
        assertNotNull(customTx);
        assertEquals(10, customTx.size());

        // Test Each Record is sourced from known accounts
        customTx.forEach(e -> {

            String key = e.key();
            CreditCardTransaction tx = e.value();

            assertNotNull(key);
            assertNotNull(tx);
            assertTrue(tx instanceof CreditCardTransaction);
            assertNotNull(tx.getCardNumber());
            this.assertCustomTransaction(tx);
        });
    }

    private Consumer<String, CreditCardTransaction> getConsumer(String groupId) {
        if (consumer == null) {
            consumer = new KafkaConsumer<>(this.baseConsumerProps(groupId));
            consumer.subscribe(Collections.singletonList(producerTopic));
        }
        return consumer;
    }

    private void assertTransaction(String key, CreditCardTransaction transaction) {
        log.info("Asserting Transaction : {}", transaction);
        assertNotNull(transaction);
        CreditCardAccount account = kafkaUtils.accountsMap().get(key);
        assertNotNull(account);
        assertEquals(account.getCard().getType(), transaction.getCardType());
        assertEquals(account.getCustomer().getName(), transaction.getCardHolderName());
    }

    private void assertCustomTransaction(CreditCardTransaction transaction) {
        assertNotNull(transaction);
        CreditCardAccount account = kafkaTestUtil.getTestAccount();
        assertNotNull(account);

        assertNotNull(transaction.getMerchantId());
        Merchant merchant = kafkaUtils.merchantMap().get(transaction.getMerchantId());
        assertNotNull(merchant);

        assertEquals(merchant.getAuthCode(), transaction.getAuthCode());
        assertEquals(account.getCard().getType(), transaction.getCardType());
        assertEquals(account.getCard().getBrand(), transaction.getCardBrand());
        assertEquals(account.getCustomer().getName(), transaction.getCardHolderName());
        assertEquals("Somewhere", transaction.getLocation());
    }

    public Properties baseConsumerProps(String groupId) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, CreditCardDeserializer.class.getName());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return props;
    }

    private AdminClient getAdminClient() {
        if (adminClient == null) {
            Properties config = new Properties();
            config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
            adminClient = AdminClient.create(config);
        }
        return adminClient;
    }
}
