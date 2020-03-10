package com.redhat.summit.test;

import com.redhat.summit.model.CreditCardTransaction;
import io.quarkus.kafka.client.serialization.JsonbSerde;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Produced;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

@Slf4j
@ApplicationScoped
public class TestFraudTopology {

    @ConfigProperty(name = "redhat.summit.kafka.test.producer.topic")
    String producerTopic;

    @ConfigProperty(name = "redhat.summit.kafka.test.consumer.topic")
    String consumerTopic;

    /**
     * Dummy Streams Test
     *
     * @return
     */
    @Produces
    public Topology buildTopology() {
        final StreamsBuilder builder = new StreamsBuilder();

        final JsonbSerde<CreditCardTransaction> creditCardTransactionSerde = new JsonbSerde<>(
                CreditCardTransaction.class);

        builder.stream(producerTopic, Consumed.with(Serdes.String(), creditCardTransactionSerde))
                .peek((key, value) -> {
                    log.info(" ::: Test Producer Stream KEY : '{}' - VALUE '{}' ::: ", key, value);
                })
                .to(consumerTopic, Produced.with(Serdes.String(), creditCardTransactionSerde));

        return builder.build();
    }
}