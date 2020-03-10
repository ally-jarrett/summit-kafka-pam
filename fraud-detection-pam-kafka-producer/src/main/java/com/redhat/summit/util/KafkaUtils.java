package com.redhat.summit.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.summit.model.CreditCardAccount;
import com.redhat.summit.model.CreditCardTransaction;
import com.redhat.summit.model.Merchant;
import io.smallrye.reactive.messaging.kafka.KafkaMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Slf4j
@ApplicationScoped
public class KafkaUtils {

    @ConfigProperty(name = "kafka.bootstrap.servers")
    String bootstrap;

    public List<CreditCardAccount> accounts;
    public List<Merchant> merchants;
    public ObjectMapper mapper;

    @PostConstruct
    public void init() throws Exception {
        // Load JSON CC Accounts File into memory
        accounts = this.getDummyCreditCardAccounts();
        merchants = this.getMerchants();
    }

    /**
     * Load Fake CC Data
     *
     * @return
     * @throws Exception
     */
    public List<CreditCardAccount> getDummyCreditCardAccounts() throws Exception {
        return this.getMapper().readValue(this.getJsonText("creditcard_data.json"),
                new TypeReference<ArrayList<CreditCardAccount>>() {
                });
    }

    /**
     * Load Marchant Data
     *
     * @return
     * @throws Exception
     */
    public List<Merchant> getMerchants() throws Exception {
        return this.getMapper().readValue(this.getJsonText("merchants.json"),
                new TypeReference<ArrayList<Merchant>>() {
                });
    }

    /**
     * Returns raw json text of classpath file
     *
     * @param filename
     * @return
     * @throws Exception
     */
    private String getJsonText(String filename) throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource(filename).getFile());

        InputStream is = new FileInputStream(file);
        String jsonTxt = IOUtils.toString(is, "UTF-8");

        log.trace(jsonTxt);
        return jsonTxt;
    }

    public Map<String, CreditCardAccount> accountsMap() {
        Map<String, CreditCardAccount> accountMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(this.accounts)) {
            accountMap = accounts.stream()
                    .collect(Collectors.toMap(x -> x.getCard().getNumber().replaceAll("\\s+", ""), v -> v));
        }
        return accountMap;
    }

    public Map<String, Merchant> merchantMap() {
        Map<String, Merchant> merchantMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(this.merchants)) {
            merchantMap = merchants.stream()
                    .collect(Collectors.toMap(x -> x.getCode(), v -> v));
        }
        return merchantMap;
    }

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

        Merchant m = this.getRandomMerchant();
        tx.setMerchantId(m.getCode());
        tx.setAuthCode(m.getAuthCode());
        // Point of Sale ID
        tx.setPosId(UUID.randomUUID().toString());
        tx.setLocation("Somewhere");
        return tx;
    }

    public Properties baseConsumerProps(String groupId) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, IntegerDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return props;
    }

    public Properties baseProducerProps(String clientId) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        props.put(ProducerConfig.CLIENT_ID_CONFIG, clientId);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, IntegerSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        return props;
    }

    public Merchant getRandomMerchant() {
        return merchants.get(ThreadLocalRandom.current().nextInt(0, merchants.size() - 1));
    }

    public KafkaMessage<String, CreditCardTransaction> publish(CreditCardTransaction creditCardTransaction) {
        return KafkaMessage.of(creditCardTransaction.getCardNumber(), creditCardTransaction);
    }

    public CreditCardTransaction getRandomTransaction() {
        CreditCardAccount account = this.accounts.get(ThreadLocalRandom.current().nextInt(0, 100));
        final CreditCardTransaction creditCardTransaction = this.buildCCTX(account, null);
        return creditCardTransaction;
    }

    public ObjectMapper getMapper() {
        if (mapper == null) {
            mapper = new ObjectMapper();
        }
        return mapper;
    }
}
