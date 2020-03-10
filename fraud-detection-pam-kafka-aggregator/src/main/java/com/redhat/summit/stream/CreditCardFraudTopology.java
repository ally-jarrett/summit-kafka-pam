package com.redhat.summit.stream;

import com.redhat.summit.model.CreditCardTransaction;
import io.quarkus.kafka.client.serialization.JsonbSerde;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.state.KeyValueBytesStoreSupplier;
import org.apache.kafka.streams.state.Stores;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

@Slf4j
@ApplicationScoped
public class CreditCardFraudTopology {

    @ConfigProperty(name = "redhat.summit.kafka.test.producer.topic")
    String producerTopic;

    @ConfigProperty(name = "redhat.summit.kafka.test.consumer.topic")
    String consumerTopic;

    public static final String TRANSACTIONS_STORE = "transactions-store";
    public static final String TRANSACTIONS_TOPIC = "transactions-topic";
    public static final String AGGREGATED_TOPIC = "transactions-aggregated";

    /**
     * TODO : Uncomment to use
     * @return
     */
    //@Produces
    public Topology buildTopology() {
        final StreamsBuilder builder = new StreamsBuilder();

        final JsonbSerde<CreditCardTransaction> creditCardTransactionSerde = new JsonbSerde<>(
                CreditCardTransaction.class);

        final JsonbSerde<Aggregation> aggregationSerde = new JsonbSerde<>(Aggregation.class);
        final KeyValueBytesStoreSupplier storeSupplier = Stores.persistentKeyValueStore(TRANSACTIONS_STORE);

        builder.stream(TRANSACTIONS_TOPIC, Consumed.with(Serdes.String(), creditCardTransactionSerde)) //
                .peek((key, value) -> {
                    log.info(" ::: Test Producer Stream KEY : '{}' - VALUE '{}' ::: ", key, value);
                })
                .groupByKey() //
                .aggregate( //
                        Aggregation::new, //
                        (creditCardNumber, value, aggregation) -> aggregation.updateFrom(value), //
                        Materialized.<String, Aggregation>as(storeSupplier) //
                                .withKeySerde(Serdes.String()) //
                                .withValueSerde(aggregationSerde) //
                ) //
                .toStream() //
                .to(AGGREGATED_TOPIC, Produced.with(Serdes.String(), aggregationSerde));

        return builder.build();
    }
}