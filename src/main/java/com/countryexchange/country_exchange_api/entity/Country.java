package com.countryexchange.country_exchange_api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
@Getter
@Setter
@Entity
@Table(name = "countries",
        indexes = @Index(name = "idx_name", columnList = "name"))
public class Country {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name; // unique by name (case-insensitive in logic)

    private String capital;

    private String region;

    @Column(nullable = false)
    private Long population;

    private String currencyCode; // e.g. NGN

    private Double exchangeRate; // rate relative to USD (i.e., 1 USD = x currency) as double

    private Double estimatedGdp; // computed

    private String flagUrl;

    private Instant lastRefreshedAt;
}


