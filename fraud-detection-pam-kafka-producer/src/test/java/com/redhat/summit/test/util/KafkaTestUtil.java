package com.redhat.summit.test.util;

import com.redhat.summit.model.Card;
import com.redhat.summit.model.CreditCardAccount;
import com.redhat.summit.model.Customer;
import com.redhat.summit.model.TransactionEventConfiguration;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.ApplicationScoped;

@Slf4j
@ApplicationScoped
public class KafkaTestUtil {

    public static final int EXPECTED_MESSAGE_COUNT = 100;
    public static final double EXPECTED_TOTAL_AMOUNT = 1000.0;
    public static final long TEST_GENERATED_AMOUNT = 1000l;
    public static final int TEST_TIMEFRAME_SECONDS = 100;
    public static final int TEST_TIMEFRAME_MINUTES = 5;
    public static final int TEST_TIMEFRAME_HOURS = 1;

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

    public String getTransactionKey(){
        return this.getTestAccount().getCard().getNumber().replaceAll("\\s+", "");
    }

}
