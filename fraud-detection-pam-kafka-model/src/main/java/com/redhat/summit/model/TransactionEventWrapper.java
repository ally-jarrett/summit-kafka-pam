package com.redhat.summit.model;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class TransactionEventWrapper {

    private long interval;
    private List<CreditCardTransaction> transactions;

}
