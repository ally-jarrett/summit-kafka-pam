package com.redhat.summit.model;

import java.time.LocalDateTime;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class CreditCardTransaction implements Comparable<CreditCardTransaction>{
    private String transactionReference;
    private String cardHolderName;
    private String cardNumber;
    private long amount;
    private LocalDateTime date;

    public String getTransactionReference() {
        return transactionReference;
    }

    public void setTransactionReference(String transactionReference) {
        this.transactionReference = transactionReference;
    }

    public String getCardHolderName() {
        return cardHolderName;
    }

    public void setCardHolderName(String cardHolderName) {
        this.cardHolderName = cardHolderName;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    @Override
    public String toString() {
        return "CreditCardTransaction [amount=" + amount + ", cardHolderName=" + cardHolderName + ", cardNumber="
                + cardNumber + ", date=" + date + ", transactionReference=" + transactionReference + "]";
    }

    @Override
    public int compareTo(CreditCardTransaction o) {
        return this.getDate().compareTo(o.getDate());
    }
}