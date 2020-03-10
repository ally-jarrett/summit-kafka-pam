package com.redhat.summit.test.util;

import com.redhat.summit.model.*;
import com.redhat.summit.serializers.CreditCardDeserializer;
import io.quarkus.kafka.client.serialization.JsonbSerializer;
import io.smallrye.reactive.messaging.kafka.KafkaMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@ApplicationScoped
public class KafkaTestUtil {

    @ConfigProperty(name = "kafka.bootstrap.servers")
    String bootstrap;

    public static final int EXPECTED_MESSAGE_COUNT = 100;
    public static final double EXPECTED_TOTAL_AMOUNT = 1000.0;
    public static final long TEST_GENERATED_AMOUNT = 1000l;
    public static final int TEST_TIMEFRAME_SECONDS = 100;
    public static final int TEST_TIMEFRAME_MINUTES = 5;
    public static final int TEST_TIMEFRAME_HOURS = 1;


    public CreditCardTransaction buildCCTX(CreditCardAccount account, Double amount) {
        CreditCardTransaction tx = new CreditCardTransaction();
        tx.setTransactionReference(UUID.randomUUID().toString());
        tx.setCardNumber(account.getCard().getNumber().replaceAll("\\s+", ""));
        tx.setCardHolderName(account.getCustomer().getName());
        tx.setCardType(account.getCard().getType());
        tx.setCardBrand(account.getCard().getBrand());
        tx.setDate(LocalDateTime.now());

        if (amount == null || amount <= 0) {
            tx.setAmount((double) ThreadLocalRandom.current().nextInt(1, 10));
        } else {
            tx.setAmount(amount);
        }

        Merchant m = this.getTestMerchant();
        tx.setMerchantId(m.getCode());
        tx.setAuthCode(m.getAuthCode());
        // Point of Sale ID
        tx.setPosId(UUID.randomUUID().toString());
        tx.setLocation("Somewhere");
        return tx;
    }

    public TransactionEventConfiguration getEventConfiguration() {
        TransactionEventConfiguration eventConfiguration = new TransactionEventConfiguration();

        // Create Base Event Configuration
        eventConfiguration.setAccount(getTestAccount());
        eventConfiguration.setTotalGeneratedTxAmount(TEST_GENERATED_AMOUNT);
        eventConfiguration.setTotalTransactionCount(EXPECTED_MESSAGE_COUNT);
        return eventConfiguration;
    }

    public CreditCardAccount getTestAccount() {
        // Create Card
        Card card = new Card();
        card.setBrand("MasterCard");
        card.setType("Debit");
        card.setNumber("1234 5678 9101 1121");
        card.setExpirationMonth(4l);
        card.setExpirationYear(2022l);

        // Create Account Holder
        Customer customer = new Customer();
        customer.setName("Ally");
        customer.setAddress("Bristol");
        customer.setCountry("UK");

        // Create account
        CreditCardAccount account = new CreditCardAccount();
        account.setCard(card);
        account.setCustomer(customer);
        return account;
    }

    public Merchant getTestMerchant() {
        Merchant m = new Merchant();
        m.setName("Nike");
        m.setCode("NIK-01");
        m.setAuthCode("NIKE-0000");
        return m;
    }

    public KafkaMessage<String, CreditCardTransaction> publish(CreditCardTransaction creditCardTransaction) {
        return KafkaMessage.of(creditCardTransaction.getCardNumber(), creditCardTransaction);
    }

    public String getTransactionKey() {
        return this.getTestAccount().getCard().getNumber().replaceAll("\\s+", "");
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

    public Properties baseProducerProps(String clientId) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        props.put(ProducerConfig.CLIENT_ID_CONFIG, clientId);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonbSerializer.class.getName());
        return props;
    }

}
