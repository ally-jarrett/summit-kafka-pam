package com.redhat.summit;

import com.redhat.summit.model.CreditCardTransaction;
import com.redhat.summit.model.TransactionEventConfiguration;
import com.redhat.summit.util.KafkaUtils;
import io.reactivex.Flowable;
import io.smallrye.reactive.messaging.kafka.KafkaMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.concurrent.*;

/**
 * Credit Card Transaction Producer
 */
@Slf4j
@ApplicationScoped
public class KafkaProducer {

    @Inject
    KafkaUtils kafkaUtils;

    public static final String TRANSACTION_ENDPOINT = "transactions-topic";

    @PostConstruct
    public void init() {
        this.generateGenericTransactions();
    }

    private BlockingQueue<KafkaMessage<String, CreditCardTransaction>> messages = new LinkedBlockingQueue<>();

    public void add(KafkaMessage<String, CreditCardTransaction> message) {
        log.info("Adding new message to blocking queue: {}", message);
        this.messages.add(message);
    }

    public BlockingQueue<KafkaMessage<String, CreditCardTransaction>> get() {
        return this.messages;
    }

    /**
     * Pull from BlockingQueue and send transaction to kafkaTopic
     *
     * @return
     */
    @Outgoing(TRANSACTION_ENDPOINT)
    public CompletionStage<KafkaMessage<String, CreditCardTransaction>> send() {
        return CompletableFuture.<KafkaMessage<String, CreditCardTransaction>>supplyAsync(() -> {
            try {
                KafkaMessage<String, CreditCardTransaction> message = messages.take();
                log.info("Sending TX to Kafka Queue with Key {} and payload: {}", message.getKey(), message);
                return message;
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Produce Base/Generic/Random Transactions every 500ms
     *
     * @return
     */
    public void generateGenericTransactions() {
        Flowable.interval(500, TimeUnit.MILLISECONDS)
                .onBackpressureDrop()
                .doOnNext(tick -> this.add(kafkaUtils.publish(kafkaUtils.getRandomTransaction()))).subscribe();
    }

    /**
     * Produce Custom Transactions for given TransactionEventConfiguration
     *
     * @return
     */
    // TODO : Fix Flowable to work with delay
    public void injectCustomTransactions(TransactionEventConfiguration eventConfiguration) {
        log.info("{} custom transactions received, processing at a rate of {}ms per message",
                eventConfiguration.getCustomTransactions().size(), eventConfiguration.getIntervalInMs());
        Flowable.range(1, eventConfiguration.getCustomTransactions().size())
                .concatMap(i -> Flowable.just(i).delay(eventConfiguration.getIntervalInMs(), TimeUnit.MILLISECONDS))
                .doOnNext(i -> {
                    KafkaMessage<String, CreditCardTransaction> msg = kafkaUtils.publish(eventConfiguration.getCustomTransactions().get(i));
                    log.info("Publishing Custom CC Transaction with key : {}", msg.getKey());
                    this.add(msg);
                });
    }

    /**
     * Produce Custom Transactions for given TransactionEventConfiguration
     *
     * @return
     */
    public void injectCustomTransactionsTemp(TransactionEventConfiguration eventConfiguration) {
        if (CollectionUtils.isNotEmpty(eventConfiguration.getCustomTransactions())) {

            // Run Async to not block calling thread
            CompletableFuture.runAsync(() -> {
                        eventConfiguration.getCustomTransactions().stream().forEach(tx -> {
                            try {
                                TimeUnit.MILLISECONDS.sleep(eventConfiguration.getIntervalInMs());
                                KafkaMessage<String, CreditCardTransaction> msg = kafkaUtils.publish(tx);
                                log.info("Publishing Custom CC Transaction with key : {}", msg.getKey());
                                this.add(msg);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        });
                    }
            );
        }
    }
}