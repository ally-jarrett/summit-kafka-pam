package com.redhat.summit.serializers;

import com.redhat.summit.model.CreditCardTransaction;
import io.quarkus.kafka.client.serialization.JsonbDeserializer;

public class CreditCardDeserializer extends JsonbDeserializer<CreditCardTransaction> {
    public CreditCardDeserializer() {
        super(CreditCardTransaction.class);
    }
}

