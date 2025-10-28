package com.countryexchange.country_exchange_api.controller;

import com.countryexchange.country_exchange_api.entity.Country;
import com.countryexchange.country_exchange_api.repository.CountryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Comparator;
import java.util.Objects;

@RestController
@RequiredArgsConstructor
public class StatusController {
    private final CountryRepository repo;

    @GetMapping("/status")
    public ResponseEntity<?> status() {
        long total = repo.count();
        Instant last = repo.findAll().stream().map(Country::getLastRefreshedAt).filter(Objects::nonNull)
                .max(Comparator.naturalOrder()).orElse(null);
        return ResponseEntity.ok(
                java.util.Map.of("total_countries", total, "last_refreshed_at", last == null ? null : last.toString())
        );
    }
}
