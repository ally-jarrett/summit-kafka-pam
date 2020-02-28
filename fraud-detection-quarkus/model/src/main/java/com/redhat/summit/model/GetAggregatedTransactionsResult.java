package com.redhat.summit.model;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.PriorityQueue;

public class GetAggregatedTransactionsResult {

    private static GetAggregatedTransactionsResult NOT_FOUND = new GetAggregatedTransactionsResult(null, null, null);

    private final PriorityQueue<CreditCardTransaction> transactions;
    private final String host;
    private final Integer port;

    public GetAggregatedTransactionsResult(PriorityQueue<CreditCardTransaction> transactions, String host, Integer port) {
        this.transactions = transactions;
        this.host = host;
        this.port = port;
    }

    public static GetAggregatedTransactionsResult found(PriorityQueue<CreditCardTransaction> transactions) {
        return new GetAggregatedTransactionsResult(transactions, null, null);
    }

    public static GetAggregatedTransactionsResult foundRemotely(String host, int port) {
        return new GetAggregatedTransactionsResult(null, host, port);
    }

    public static GetAggregatedTransactionsResult notFound() {
        return NOT_FOUND;
    }

    public Optional<PriorityQueue<CreditCardTransaction>> getResult() {
        return Optional.ofNullable(transactions);
    }

    public PriorityQueue<CreditCardTransaction> getTransactions() {
        return transactions;
    }

    public Optional<String> getHost() {
        return Optional.ofNullable(host);
    }

    public OptionalInt getPort() {
        return port != null ? OptionalInt.of(port) : OptionalInt.empty();
    }
}