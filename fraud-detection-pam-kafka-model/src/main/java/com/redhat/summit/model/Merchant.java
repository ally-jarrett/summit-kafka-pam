package com.redhat.summit.model;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Merchant {

    String name;
    String code;
    String authCode;

}
