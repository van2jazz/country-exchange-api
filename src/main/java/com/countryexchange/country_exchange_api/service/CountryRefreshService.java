package com.countryexchange.country_exchange_api.service;


import com.countryexchange.country_exchange_api.entity.Country;
import com.countryexchange.country_exchange_api.exception.ExternalApiException;
import com.countryexchange.country_exchange_api.repository.CountryRepository;
import com.countryexchange.country_exchange_api.util.Utils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
//import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CountryRefreshService {

    private final CountryRepository countryRepository;
    private final ImageService imageService;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${external.countries.url:https://restcountries.com/v2/all?fields=name,capital,region,population,flag,currencies}")
    private String countriesUrl;

    @Value("${external.rates.url:https://open.er-api.com/v6/latest/USD}")
    private String ratesUrl;

    // global last refresh stored separately? we will store in DB record timestamps and image file timestamp
    @Transactional
    public Map<String,Object> refreshAll() {
        System.out.println("zzzzzzz");
        // 1. Fetch countries
        List<Map<String,Object>> rawCountries = fetchCountries();
        // 2. Fetch exchange rates once
        Map<String, Double> rates = fetchRates();

        Instant now = Instant.now();

        List<Country> processed = new ArrayList<>();
        for (Map<String,Object> rc : rawCountries) {
            // name, capital, region, population, flag, currencies
            String name = (String) rc.get("name");
            Object popObj = rc.get("population");
            long population = 0;
            if (popObj instanceof Number) population = ((Number) popObj).longValue();
            else population = 0L;

            String capital = rc.getOrDefault("capital", null) != null ? rc.get("capital").toString() : null;
            String region = rc.getOrDefault("region", null) != null ? rc.get("region").toString() : null;
            String flag = rc.getOrDefault("flag", null) != null ? rc.get("flag").toString() : null;

            // extract currency code: currencies is array of {code,name,symbol}. We store first code. If none, currencyCode null
            String currencyCode = null;
            Object currenciesObj = rc.get("currencies");
            if (currenciesObj instanceof List) {
                List<?> currencies = (List<?>) currenciesObj;
                if (!currencies.isEmpty()) {
                    Object first = currencies.get(0);
                    if (first instanceof Map) {
                        Object code = ((Map<?,?>) first).get("code");
                        if (code != null) currencyCode = code.toString();
                    }
                }
            }

            Double exchangeRate = null;
            Double estimatedGdp = null;
            if (currencyCode == null) {
                exchangeRate = null;
                estimatedGdp = 0.0;
            } else {
                // match currency code (rates map keys are currency codes). If not found, set both to null.
                if (rates.containsKey(currencyCode)) {
                    exchangeRate = rates.get(currencyCode);
                    // estimated_gdp = population * random(1000-2000) / exchange_rate
                    long multiplier = Utils.randomBetween(1000, 2000);
                    estimatedGdp = (double) population * multiplier / exchangeRate;
                } else {
                    exchangeRate = null;
                    estimatedGdp = null;
                }
            }

            // upsert by name (case-insensitive)
            Optional<Country> existingOpt = countryRepository.findByNameIgnoreCase(name);
            Country country;
            if (existingOpt.isPresent()) {
                Country e = existingOpt.get();
                e.setCapital(capital);
                e.setRegion(region);
                e.setPopulation(population);
                e.setCurrencyCode(currencyCode);
                e.setExchangeRate(exchangeRate);
                e.setEstimatedGdp(estimatedGdp);
                e.setFlagUrl(flag);
                e.setLastRefreshedAt(now);
                country = e;
            } else {
                country = Country.builder()
                        .name(name)
                        .capital(capital)
                        .region(region)
                        .population(population)
                        .currencyCode(currencyCode)
                        .exchangeRate(exchangeRate)
                        .estimatedGdp(estimatedGdp)
                        .flagUrl(flag)
                        .lastRefreshedAt(now)
                        .build();
            }
            processed.add(country);
        }

        // Save all in one transaction
        countryRepository.saveAll(processed);

        // Generate image with top 5 by estimatedGdp (nulls handled)
        imageService.generateSummaryImage(now, processed);

        Map<String,Object> result = new HashMap<>();
        result.put("total", processed.size());
        result.put("last_refreshed_at", now.toString());
        return result;
    }

    private List<Map<String,Object>> fetchCountries() {
        try {
            System.out.println("🌍 Fetching countries from: " + countriesUrl);
            ResponseEntity<List> resp = restTemplate.getForEntity(countriesUrl, List.class);
            System.out.println("✅ Countries API status: " + resp.getStatusCode());
            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                throw new ExternalApiException("Countries API returned no data");
            }
            return (List<Map<String,Object>>) resp.getBody();
        } catch (Exception e) {
            e.printStackTrace();
            throw new ExternalApiException("Could not fetch data from Countries API: " + e.getMessage());
        }
    }

    private Map<String, Double> fetchRates() {
        try {
            System.out.println("💱 Fetching exchange rates from: " + ratesUrl);
            ResponseEntity<Map> resp = restTemplate.getForEntity(ratesUrl, Map.class);
            System.out.println("✅ Rates API status: " + resp.getStatusCode());
            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                throw new ExternalApiException("Rates API returned no data");
            }
            Map body = resp.getBody();
            Object ratesObj = body.get("rates");
            if (!(ratesObj instanceof Map)) throw new ExternalApiException("Rates object missing");
            Map<String, Object> ratesMap = (Map<String, Object>) ratesObj;
            return ratesMap.entrySet()
                    .stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> ((Number) e.getValue()).doubleValue()));
        } catch (Exception e) {
            e.printStackTrace();
            throw new ExternalApiException("Could not fetch data from Exchange Rates API: " + e.getMessage());
        }
    }


//    private List<Map<String,Object>> fetchCountries() {
//        try {
//            ResponseEntity<List> resp = restTemplate.getForEntity(countriesUrl, List.class);
//            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
//                throw new ExternalApiException("Could not fetch data from Countries API");
//            }
//            // Each element is a Map
//            return (List<Map<String,Object>>) resp.getBody();
//        } catch (Exception e) {
//            throw new ExternalApiException("Could not fetch data from Countries API");
//        }
//
//    }
//
//
//
//    private Map<String, Double> fetchRates() {
//        try {
//            ResponseEntity<Map> resp = restTemplate.getForEntity(ratesUrl, Map.class);
//            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
//                throw new ExternalApiException("Could not fetch data from Exchange Rates API");
//            }
//            Map body = resp.getBody();
//            // open.er-api returns "rates" object
//            Object ratesObj = body.get("rates");
//            if (!(ratesObj instanceof Map)) throw new ExternalApiException("Rates object missing");
//            Map<String, Object> ratesMap = (Map<String, Object>) ratesObj;
//            return ratesMap.entrySet()
//                    .stream()
//                    .collect(Collectors.toMap(Map.Entry::getKey, e -> ((Number) e.getValue()).doubleValue()));
//        } catch (Exception e) {
//            throw new ExternalApiException("Could not fetch data from Exchange Rates API");
//        }
//    }
}
