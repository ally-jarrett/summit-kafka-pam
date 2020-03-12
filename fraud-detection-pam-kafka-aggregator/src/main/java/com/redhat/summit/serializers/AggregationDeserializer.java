package com.redhat.summit.serializers;

import com.redhat.summit.stream.Aggregation;
import io.quarkus.kafka.client.serialization.JsonbDeserializer;

public class AggregationDeserializer extends JsonbDeserializer<Aggregation> {
    public AggregationDeserializer() {
        super(Aggregation.class);
    }
}

