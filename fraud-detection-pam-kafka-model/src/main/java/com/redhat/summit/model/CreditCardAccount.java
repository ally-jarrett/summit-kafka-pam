package com.redhat.summit.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class CreditCardAccount {

    private Customer customer;
    private Card card;
    private boolean isBlocked;

}