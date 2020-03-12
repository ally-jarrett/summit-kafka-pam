package com.redhat.summit.stream;

import com.redhat.summit.model.CreditCardTransaction;
import io.smallrye.reactive.messaging.kafka.KafkaMessage;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
@ApplicationScoped
public class TransactionProcessor {

    @Inject
    InteractiveQuery query;

    private BlockingQueue<KafkaMessage<String, CreditCardTransaction>> blocked = new LinkedBlockingQueue<>();
    private BlockingQueue<KafkaMessage<String, CreditCardTransaction>> eligible = new LinkedBlockingQueue<>();

    public static final String TRANSACTIONS_TOPIC = "transactions-topic";
    public static final String TRANSACTIONS_IN_TOPIC = "transactions-in-topic";
    public static final String TRANSACTIONS_OUT_TOPIC = "transactions-out-topic";
    public static final String TRANSACTIONS_BLOCKED_TOPIC = "transactions-blocked-topic";

    @Incoming(TRANSACTIONS_TOPIC)
    public void processTx(CreditCardTransaction transaction) {
        log.debug("New Eligible Transaction received: " + transaction);
    }

    /**
     * Pre-Validate all transactions
     * @param transaction
     */
    @Incoming(TRANSACTIONS_IN_TOPIC)
    public void process(CreditCardTransaction transaction) {
        if (query.isCardBlockEnabled(transaction.getCardNumber())) {
            this.blocked.add(publish(transaction));
        } else {
            this.eligible.add(publish(transaction));
        }
    }

    /**
     * Send eligible transactions to queue
     * @return
     */
    @Outgoing(TRANSACTIONS_OUT_TOPIC)
    public CompletionStage<KafkaMessage<String, CreditCardTransaction>> processEligibleTransaction() {
        return CompletableFuture.<KafkaMessage<String, CreditCardTransaction>>supplyAsync(() -> {
            try {
                KafkaMessage<String, CreditCardTransaction> message = eligible.take();
                log.info("Sending TX to Kafka Queue with Key {} and payload: {}", message.getKey(), message);
                return message;
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Send blocked transactions to queue
     * @return
     */
    @Outgoing(TRANSACTIONS_BLOCKED_TOPIC)
    public CompletionStage<KafkaMessage<String, CreditCardTransaction>> processBlockedTransaction() {
        return CompletableFuture.<KafkaMessage<String, CreditCardTransaction>>supplyAsync(() -> {
            try {
                KafkaMessage<String, CreditCardTransaction> message = blocked.take();
                log.info("Sending TX to Kafka Queue with Key {} and payload: {}", message.getKey(), message);
                return message;
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public KafkaMessage<String, CreditCardTransaction> publish(CreditCardTransaction creditCardTransaction) {
        return KafkaMessage.of(creditCardTransaction.getCardNumber(), creditCardTransaction);
    }
}
