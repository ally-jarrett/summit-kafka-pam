package com.redhat.summit.stream;

import com.redhat.summit.model.CreditCardAccount;
import com.redhat.summit.model.GetAggregatedTransactionsResult;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.errors.InvalidStateStoreException;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.apache.kafka.streams.state.StreamsMetadata;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class InteractiveQuery {
    private static final Logger LOGGER = LoggerFactory.getLogger(InteractiveQuery.class);

    @ConfigProperty(name = "hostname")
    String host;

    @Inject
    KafkaStreams streams;

    public List<PipelineMetadata> getMetaData() {
        LOGGER.warn("Obtaining pipeline metadata");
        return streams.allMetadataForStore(CreditCardFraudTopology.TRANSACTIONS_STORE).stream()
                .map(m -> new PipelineMetadata(m.hostInfo().host() + ":" + m.hostInfo().port(),
                        m.topicPartitions().stream().map(TopicPartition::toString).collect(Collectors.toSet())))
                .collect(Collectors.toList());
    }

    /**
     * Check Card does not have a block due to Fraudulent Activity
     *
     * @param creditCardNumber
     * @return
     */
    public boolean isCardBlockEnabled(String creditCardNumber) {
        CreditCardAccount account = this.getBlockedCreditCardStore().get(creditCardNumber);
        if (account == null) {
            LOGGER.info("No Credit Account found in global KTable, no previous blocks placed on card");
            return false;
        } else {
            LOGGER.info("Latest Account Details for card {} indicate isCardBlocked = {}", creditCardNumber, account.isBlocked());
            return account.isBlocked();
        }
    }


    /**
     * Aggregate CardTransactions
     *
     * @param creditCardNumber
     * @return
     */
    public GetAggregatedTransactionsResult getAggregatedTransactions(String creditCardNumber) {
        final StreamsMetadata metadata = streams.metadataForKey(CreditCardFraudTopology.TRANSACTIONS_STORE,
                creditCardNumber, Serdes.String().serializer());

        if (metadata == null || metadata == StreamsMetadata.NOT_AVAILABLE) {
            LOGGER.warn("Found no metadata for key {}", creditCardNumber);
            return GetAggregatedTransactionsResult.notFound();
        } else if (metadata.host().equals(host)) {
            LOGGER.info("Found data for key {} locally", creditCardNumber);
            Aggregation result = getAggregatedCreditCardTx().get(creditCardNumber);

            if (result != null) {
                LOGGER.info("Result {}", result.transactions);
                return GetAggregatedTransactionsResult.found(result.transactions);
            } else {
                LOGGER.info("Result is null");
                return GetAggregatedTransactionsResult.notFound();
            }
        } else {
            LOGGER.info("Found data for key {} on remote host {}:{}", creditCardNumber, metadata.host(), metadata.port());
            return GetAggregatedTransactionsResult.foundRemotely(metadata.host(), metadata.port());
        }
    }

    private ReadOnlyKeyValueStore<String, Aggregation> getAggregatedCreditCardTx() {
        while (true) {
            try {
                return streams.store(CreditCardFraudTopology.TRANSACTIONS_STORE, QueryableStoreTypes.keyValueStore());
            } catch (InvalidStateStoreException e) {
            }
        }
    }

    private ReadOnlyKeyValueStore<String, CreditCardAccount> getBlockedCreditCardStore() {
        while (true) {
            try {
                return streams.store(CreditCardFraudTopology.BLOCKED_CARD_STORE, QueryableStoreTypes.keyValueStore());
            } catch (InvalidStateStoreException e) {
            }
        }
    }
}