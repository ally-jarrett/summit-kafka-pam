package com.redhat.summit.serializers;

import com.redhat.summit.model.CreditCardAccount;
import com.redhat.summit.model.CreditCardTransaction;
import io.quarkus.kafka.client.serialization.JsonbDeserializer;

public class CreditAccountDeserializer extends JsonbDeserializer<CreditCardAccount> {
    public CreditAccountDeserializer() {
        super(CreditCardAccount.class);
    }
}

