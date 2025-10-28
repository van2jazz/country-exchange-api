package com.countryexchange.country_exchange_api.repository;


import com.countryexchange.country_exchange_api.entity.Country;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CountryRepository extends JpaRepository<Country, Long> {
    Optional<Country> findByNameIgnoreCase(String name);
    void deleteByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);
}
