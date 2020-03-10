package com.redhat.summit.model;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@NoArgsConstructor
@RegisterForReflection
public class CreditCardTransaction implements Comparable<CreditCardTransaction> {

    private String transactionReference;
    private Double amount;
    private LocalDateTime date;

    // Account Meta
    private String cardHolderName;
    private String cardNumber;
    private String cardType;
    private String cardBrand;

    // TX Meta Data
    private String merchantId;
    private String authCode;
    private String posId;
    private String location;

    @Override
    public int compareTo(CreditCardTransaction o) {
        return this.getDate().compareTo(o.getDate());
    }
}