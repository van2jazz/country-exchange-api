package com.countryexchange.country_exchange_api.controller;

import com.countryexchange.country_exchange_api.entity.Country;
import com.countryexchange.country_exchange_api.exception.ResourceNotFoundException;
import com.countryexchange.country_exchange_api.repository.CountryRepository;
import com.countryexchange.country_exchange_api.service.CountryRefreshService;
import com.countryexchange.country_exchange_api.service.ImageService;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.file.Files;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/countries")
@RequiredArgsConstructor
@Validated
public class CountryController {

    private final CountryRepository repo;
    private final CountryRefreshService refreshService;
    private final ImageService imageService;

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh() {
        Map<String,Object> result = refreshService.refreshAll();
        return ResponseEntity.ok(result);
    }

    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String currency,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false, defaultValue = "0") @Min(0) int page,
            @RequestParam(required = false, defaultValue = "1000") @Min(1) int size
    ) {
        // fetch all and filter in-memory (for simplicity). For large datasets, convert to dynamic queries
        List<Country> all = repo.findAll();

        if (region != null) {
            all = all.stream().filter(c -> region.equalsIgnoreCase(c.getRegion())).collect(Collectors.toList());
        }
        if (currency != null) {
            all = all.stream().filter(c -> currency.equalsIgnoreCase(c.getCurrencyCode())).collect(Collectors.toList());
        }

        if ("gdp_desc".equalsIgnoreCase(sort)) {
            all = all.stream().sorted((a,b) -> {
                Double ag = a.getEstimatedGdp();
                Double bg = b.getEstimatedGdp();
                if (ag == null && bg == null) return 0;
                if (ag == null) return 1;
                if (bg == null) return -1;
                return Double.compare(bg, ag);
            }).collect(Collectors.toList());
        }

        // pagination manual
        int from = Math.min(page * size, all.size());
        int to = Math.min(from + size, all.size());
        List<Country> pageList = all.subList(from, to);

        return ResponseEntity.ok(pageList);
    }

    @GetMapping("/{name}")
    public ResponseEntity<?> getOne(@PathVariable String name) {
        return repo.findByNameIgnoreCase(name)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("Country not found"));
    }


    @Transactional
    @DeleteMapping("/{name}")
    public ResponseEntity<?> delete(@PathVariable String name) {
        if (!repo.existsByNameIgnoreCase(name)) {
            throw new ResourceNotFoundException("Country not found");
        }
        repo.deleteByNameIgnoreCase(name);
        return ResponseEntity.noContent().build();
    }


    @GetMapping("/image")
    public ResponseEntity<?> getImage() {
        File f = imageService.getSummaryImageFile();
        if (f == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Summary image not found"));
        }
        try {
            byte[] bytes = Files.readAllBytes(f.toPath());
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .body(bytes);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Internal server error"));
        }
    }

    @GetMapping("/../status")
    public ResponseEntity<?> statusHack() {
        // for convenience; endpoint also provided below at /status
        long total = repo.count();
        Instant last = repo.findAll().stream().map(Country::getLastRefreshedAt).filter(Objects::nonNull)
                .max(Comparator.naturalOrder()).orElse(null);
        return ResponseEntity.ok(Map.of("total_countries", total, "last_refreshed_at", last == null ? null : last.toString()));
    }
}
