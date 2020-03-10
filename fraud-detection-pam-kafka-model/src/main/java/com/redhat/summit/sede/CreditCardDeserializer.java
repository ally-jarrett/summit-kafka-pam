package com.redhat.summit.sede;

import com.redhat.summit.model.CreditCardTransaction;
import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;

public class CreditCardDeserializer extends ObjectMapperDeserializer<CreditCardTransaction> {
    public CreditCardDeserializer() {
        super(CreditCardTransaction.class);
    }
}

