package com.redhat.summit;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

import com.redhat.summit.model.CreditCardTransaction;

import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.reactivex.Flowable;
import io.smallrye.reactive.messaging.kafka.KafkaMessage;

@ApplicationScoped
public class KafkaProducer {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaProducer.class);

    private final List<CreditCard> creditCardRegistry = new ArrayList<>();

    private final Random rand = new Random();
    
    @PostConstruct
    public void init() {
        {
            final Customer customer = new KafkaProducer.Customer("Claudio", "Luppi");
            final CreditCard creditCard = new KafkaProducer.CreditCard("0000-0000-0000-0001", customer);
            creditCardRegistry.add(creditCard);
            LOGGER.info("Customer {} {} added to registry with credit card number {}", customer.getName(),
                    customer.getSurname(), creditCard.getNumber());
        }
        {
            final Customer customer = new KafkaProducer.Customer("Ally", "Jarret");
            final CreditCard creditCard = new KafkaProducer.CreditCard("0000-0000-0000-0002", customer);
            creditCardRegistry.add(creditCard);
            LOGGER.info("Customer {} {} added to registry with credit card number {}", customer.getName(),
                    customer.getSurname(), creditCard.getNumber());
        }
        {
            final Customer customer = new KafkaProducer.Customer("Donato", "Marrazzo");
            final CreditCard creditCard = new KafkaProducer.CreditCard("0000-0000-0000-0003", customer);
            creditCardRegistry.add(creditCard);
            LOGGER.info("Customer {} {} added to registry with credit card number {}", customer.getName(),
                    customer.getSurname(), creditCard.getNumber());
        }
        {
            final Customer customer = new KafkaProducer.Customer("Efstathios", "Rouvas");
            final CreditCard creditCard = new KafkaProducer.CreditCard("0000-0000-0000-0004", customer);
            creditCardRegistry.add(creditCard);
            LOGGER.info("Customer {} {} added to registry with credit card number {}", customer.getName(),
                    customer.getSurname(), creditCard.getNumber());
        }
        {
            final Customer customer = new KafkaProducer.Customer("Gareth", "Healy");
            final CreditCard creditCard = new KafkaProducer.CreditCard("0000-0000-0000-0005", customer);
            creditCardRegistry.add(creditCard);
            LOGGER.info("Customer {} {} added to registry with credit card number {}", customer.getName(),
                    customer.getSurname(), creditCard.getNumber());
        }
        {
            final Customer customer = new KafkaProducer.Customer("Paul", "Brown");
            final CreditCard creditCard = new KafkaProducer.CreditCard("0000-0000-0000-0006", customer);
            creditCardRegistry.add(creditCard);
            LOGGER.info("Customer {} {} added to registry with credit card number {}", customer.getName(),
                    customer.getSurname(), creditCard.getNumber());
        }
        {
            final Customer customer = new KafkaProducer.Customer("Rachid", "Snoussi");
            final CreditCard creditCard = new KafkaProducer.CreditCard("0000-0000-0000-0007", customer);
            creditCardRegistry.add(creditCard);
            LOGGER.info("Customer {} {} added to registry with credit card number {}", customer.getName(),
                    customer.getSurname(), creditCard.getNumber());
        }
    }

    @Outgoing("transactions-topic") 
    public Flowable<KafkaMessage<String, CreditCardTransaction>> generate() {
       return Flowable.interval(2, TimeUnit.SECONDS)
        .onBackpressureDrop()
        .map(tick -> {
            return publish(newRandomTransaction(creditCardRegistry.get(rand.nextInt(creditCardRegistry.size()))));
        });
    }

    private KafkaMessage<String, CreditCardTransaction> publish(CreditCardTransaction creditCardTransaction) {
        return KafkaMessage.of(creditCardTransaction.getCardNumber(), creditCardTransaction);
    }

    private CreditCardTransaction newRandomTransaction(CreditCard crediCard) {
        final int max = 10000;
        final int min = 1;
        final CreditCardTransaction creditCardTransaction = new CreditCardTransaction();
        creditCardTransaction.setCardNumber(crediCard.getNumber());
        creditCardTransaction.setAmount(rand.nextInt((max - min) + 1) + min);
        creditCardTransaction
                .setCardHolderName(crediCard.getHolder().getName() + " " + crediCard.getHolder().getSurname());
                creditCardTransaction.setDate(LocalDateTime.now());
                creditCardTransaction.setTransactionReference(UUID.randomUUID().toString());
        return creditCardTransaction;
    }

    private class Customer {

        private final String name;

        private final String surname;

        public Customer(String name, String surname) {
            this.name = name;
            this.surname = surname;
        }

        public String getName() {
            return name;
        }

        public String getSurname() {
            return surname;
        }
    }

    private class CreditCard {

        private final String number;

        private final Customer holder;

        public CreditCard(String number, Customer holder) {
            this.number = number;
            this.holder = holder;
        }

        public String getNumber() {
            return number;
        }

        public Customer getHolder() {
            return holder;
        }

    }
}