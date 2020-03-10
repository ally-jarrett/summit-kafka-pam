package com.redhat.summit.test;

import com.redhat.summit.model.CreditCardTransaction;
import com.redhat.summit.model.Merchant;
import com.redhat.summit.model.TransactionEventConfiguration;
import com.redhat.summit.test.util.KafkaTestUtil;
import com.redhat.summit.util.KafkaUtils;
import com.redhat.summit.util.TransactionEventProducer;
import io.quarkus.test.junit.QuarkusTest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@QuarkusTest
public class TransactionEventProcessorTest {

    @Inject
    TransactionEventProducer eventProducer;

    @Inject
    KafkaUtils kafkaUtils;

    @Inject
    KafkaTestUtil kafkaTestUtil;

    private TransactionEventConfiguration eventConfiguration;
    private TransactionEventConfiguration configuredEventConfiguration;

    @Test
    public void testEventConfigurationBasic() throws Exception {
        eventConfiguration = kafkaTestUtil.getEventConfiguration();
        configuredEventConfiguration = eventProducer.processEventConfiguration(eventConfiguration);

        assertNotNull(configuredEventConfiguration);
        assertNotNull(configuredEventConfiguration.getCustomTransactions());
        assertEquals(kafkaTestUtil.EXPECTED_MESSAGE_COUNT, configuredEventConfiguration.getCustomTransactions().size());

        // Accumulate all amounts
        double sum = configuredEventConfiguration.getCustomTransactions().stream()
                .filter(o -> o.getAmount() > 0)
                .mapToDouble(CreditCardTransaction::getAmount)
                .sum();

        assertEquals(kafkaTestUtil.EXPECTED_TOTAL_AMOUNT, sum);
        assertEquals(kafkaTestUtil.EXPECTED_TOTAL_AMOUNT, configuredEventConfiguration.getTotalAmount());
        assertEquals(0l, configuredEventConfiguration.getIntervalInMs());

        assertEquals("Ally", configuredEventConfiguration.getAccount().getCustomer().getName());
        assertEquals("UK", configuredEventConfiguration.getAccount().getCustomer().getCountry());
        assertEquals("Bristol", configuredEventConfiguration.getAccount().getCustomer().getAddress());

        assertEquals("MasterCard", configuredEventConfiguration.getAccount().getCard().getBrand());
        assertEquals("1234 5678 9101 1121", configuredEventConfiguration.getAccount().getCard().getNumber());
        assertEquals("Debit", configuredEventConfiguration.getAccount().getCard().getType());
        assertEquals(4l, configuredEventConfiguration.getAccount().getCard().getExpirationMonth());
        assertEquals(2022l, configuredEventConfiguration.getAccount().getCard().getExpirationYear());

        this.assertCustomTransaction(configuredEventConfiguration.getCustomTransactions());
    }

    @Test
    public void testEventConfigurationTimeFrameSeconds() throws Exception {

        eventConfiguration = kafkaTestUtil.getEventConfiguration();
        eventConfiguration.setTimeframe(kafkaTestUtil.TEST_TIMEFRAME_SECONDS + "s");
        configuredEventConfiguration = eventProducer.processEventConfiguration(eventConfiguration);

        assertNotNull(configuredEventConfiguration);
        assertNotNull(configuredEventConfiguration.getCustomTransactions());
        assertEquals(kafkaTestUtil.EXPECTED_MESSAGE_COUNT, configuredEventConfiguration.getCustomTransactions().size());

        // Assert Calculated Timeframe
        long timeInMillis = TimeUnit.SECONDS.toMillis(kafkaTestUtil.TEST_TIMEFRAME_SECONDS);
        long delta = timeInMillis / configuredEventConfiguration.getTotalTransactionCount();
        assertNotNull(configuredEventConfiguration.getIntervalInMs());
        assertEquals(delta, configuredEventConfiguration.getIntervalInMs());
    }

    @Test
    public void testEventConfigurationTimeFrameMinutes() throws Exception {

        eventConfiguration = kafkaTestUtil.getEventConfiguration();
        eventConfiguration.setTimeframe(kafkaTestUtil.TEST_TIMEFRAME_MINUTES + "m");
        configuredEventConfiguration = eventProducer.processEventConfiguration(eventConfiguration);

        assertNotNull(configuredEventConfiguration);
        assertNotNull(configuredEventConfiguration.getCustomTransactions());
        assertEquals(kafkaTestUtil.EXPECTED_MESSAGE_COUNT, configuredEventConfiguration.getCustomTransactions().size());

        // Assert Calculated Timeframe
        long timeInMillis = TimeUnit.MINUTES.toMillis(kafkaTestUtil.TEST_TIMEFRAME_MINUTES);
        long delta = timeInMillis / configuredEventConfiguration.getTotalTransactionCount();
        assertNotNull(configuredEventConfiguration.getIntervalInMs());
        assertEquals(delta, configuredEventConfiguration.getIntervalInMs());
    }

    @Test
    public void testEventConfigurationMaxTimeFrame10Minutes() throws Exception {

        eventConfiguration = kafkaTestUtil.getEventConfiguration();
        eventConfiguration.setTimeframe(kafkaTestUtil.TEST_TIMEFRAME_HOURS + "h");
        configuredEventConfiguration = eventProducer.processEventConfiguration(eventConfiguration);

        assertNotNull(configuredEventConfiguration);
        assertNotNull(configuredEventConfiguration.getCustomTransactions());
        assertEquals(kafkaTestUtil.EXPECTED_MESSAGE_COUNT, configuredEventConfiguration.getCustomTransactions().size());

        // Assert Calculated Timeframe defaulted to 10 Min Max
        long timeInMillis = TimeUnit.MINUTES.toMillis(10);
        long delta = timeInMillis / configuredEventConfiguration.getTotalTransactionCount();
        assertNotNull(configuredEventConfiguration.getIntervalInMs());
        assertEquals(delta, configuredEventConfiguration.getIntervalInMs());
    }


    @Test
    public void testEventConfigurationCustomTx() throws Exception {

        double customAmount1 = 1000.0;
        double customAmount2 = 10000.0;

        eventConfiguration = kafkaTestUtil.getEventConfiguration();
        eventConfiguration.setTimeframe(kafkaTestUtil.TEST_TIMEFRAME_MINUTES + "m");

        // Add 2 Custom Specified TX
        CreditCardTransaction ccTx = kafkaUtils.buildCCTX(kafkaTestUtil.getTestAccount(), customAmount1);
        CreditCardTransaction ccTx1 = kafkaUtils.buildCCTX(kafkaTestUtil.getTestAccount(), customAmount2);
        List<CreditCardTransaction> txs = Arrays.asList(ccTx, ccTx1);
        eventConfiguration.setCustomTransactions(txs);

        // Generate Transactions
        configuredEventConfiguration = eventProducer.processEventConfiguration(eventConfiguration);
        assertNotNull(configuredEventConfiguration);
        assertNotNull(configuredEventConfiguration.getCustomTransactions());
        assertEquals(kafkaTestUtil.EXPECTED_MESSAGE_COUNT + txs.size(), configuredEventConfiguration.getCustomTransactions().size());

        // Calculate expected Amount
        double expectedAmount = kafkaTestUtil.EXPECTED_TOTAL_AMOUNT + customAmount1 + customAmount2;

        // Accumulate all amounts
        double sum = configuredEventConfiguration.getCustomTransactions().stream()
                .filter(o -> o.getAmount() > 0)
                .mapToDouble(CreditCardTransaction::getAmount)
                .sum();

        assertEquals(kafkaTestUtil.TEST_GENERATED_AMOUNT, configuredEventConfiguration.getTotalGeneratedTxAmount());
        assertEquals(customAmount1 + customAmount2, configuredEventConfiguration.getTotalCustomTxAmount());
        assertEquals(expectedAmount, sum);
        assertEquals(expectedAmount, configuredEventConfiguration.getTotalAmount());

        // Assert Calculated Timeframe
        long timeInMillis = TimeUnit.MINUTES.toMillis(kafkaTestUtil.TEST_TIMEFRAME_MINUTES);
        long delta = timeInMillis / configuredEventConfiguration.getCustomTransactions().size();
        assertNotNull(configuredEventConfiguration.getIntervalInMs());
        assertEquals(delta, configuredEventConfiguration.getIntervalInMs());
    }


    private void assertCustomTransaction(List<CreditCardTransaction> transactions) {
        transactions.forEach(tx -> {
            assertEquals("Ally", tx.getCardHolderName());
            assertEquals("Debit", tx.getCardType());
            assertEquals("MasterCard", tx.getCardBrand());
            assertEquals(kafkaTestUtil.getTransactionKey(), tx.getCardNumber());
            assertNotNull(tx.getTransactionReference());
            assertTrue(tx.getAmount() > 0);
            assertNotNull(tx.getDate());

            assertNotNull(tx.getMerchantId());
            assertNotNull(tx.getPosId());
            Merchant m = kafkaUtils.merchantMap().get(tx.getMerchantId());
            assertEquals(m.getAuthCode(), tx.getAuthCode());
        });
    }
}