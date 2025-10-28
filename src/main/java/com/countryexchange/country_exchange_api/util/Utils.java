package com.countryexchange.country_exchange_api.util;

import java.util.concurrent.ThreadLocalRandom;

public class Utils {
    public static int randomBetween(int a, int b) {
        return ThreadLocalRandom.current().nextInt(a, b + 1);
    }
}
