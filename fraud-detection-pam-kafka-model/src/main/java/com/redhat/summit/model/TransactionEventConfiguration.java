package com.redhat.summit.model;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class TransactionEventConfiguration {

    CreditCardAccount account;

    // Use longs to avoid decimal calculations
    private long totalGeneratedTxAmount;
    private double totalAmount;
    private double totalCustomTxAmount;
    private int totalTransactionCount;

    // Expects String :: Xs or Xm i.e. 10s, 10m, consider ISO 8601
    private String timeframe;

    // Processed timeframe interval per tx
    private long intervalInMs;

    // Generated Tx
    private List<CreditCardTransaction> customTransactions;

}
