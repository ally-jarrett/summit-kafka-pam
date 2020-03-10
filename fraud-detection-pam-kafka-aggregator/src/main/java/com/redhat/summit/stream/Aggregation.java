package com.redhat.summit.stream;

import com.redhat.summit.model.CreditCardTransaction;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.extern.slf4j.Slf4j;

import java.util.PriorityQueue;

@Slf4j
@RegisterForReflection
public class Aggregation {

    private int queueDepth = 10;

    public PriorityQueue<CreditCardTransaction> transactions = new PriorityQueue<>();

    public Aggregation updateFrom(CreditCardTransaction transaction) {
        log.info("adding {} to transactions. Operation result {}", transaction, transactions.offer(transaction));
        log.info("current transactions depth for credit card number {} is {}", transaction.getCardNumber(), transactions.size());
        if (transactions.size() > queueDepth) {
            log.info("removing {} from aggregated transactions(queue depth value is {})", transactions.poll(), queueDepth);
        }
        return this;
    }
}