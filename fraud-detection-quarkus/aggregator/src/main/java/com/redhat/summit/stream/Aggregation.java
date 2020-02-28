package com.redhat.summit.stream;

import java.util.Comparator;
import java.util.PriorityQueue;

import com.redhat.summit.model.CreditCardTransaction;

import io.quarkus.runtime.annotations.RegisterForReflection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RegisterForReflection
public class Aggregation {

    private static final Logger LOGGER = LoggerFactory.getLogger(Aggregation.class);

    private int queueDepth = 5;

    public PriorityQueue<CreditCardTransaction> transactions = new PriorityQueue<>();
    
    public Aggregation updateFrom(CreditCardTransaction transaction) {
        LOGGER.info("adding {} to transactions..............Operation result {}", transaction, transactions.offer(transaction));
        LOGGER.info("current transactions depth is {}", transactions.size());
        if(transactions.size() > queueDepth){
            LOGGER.info("removing {} from aggregated transactions(queue depth value is {})", transactions.poll(), queueDepth);
        }
        return this;
    }
}