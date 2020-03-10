package com.redhat.summit.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Card {

    @JsonProperty("brand")
    private String brand;

    @JsonProperty("type")
    private String type;

    @JsonProperty("number")
    private String number;

    @JsonProperty("expiration-month")
    private long expirationMonth;

    @JsonProperty("expiration-year")
    private long expirationYear;
}
