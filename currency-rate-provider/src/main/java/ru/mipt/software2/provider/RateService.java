package ru.mipt.software2.provider;

import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

@Service
public class RateService {

    private static final double BASE_RATE = 95.0;
    private static final double DELTA = 1.0;

    public double getCurrentRate() {
        double fluctuation = ThreadLocalRandom.current().nextDouble(-DELTA, DELTA);
        return BASE_RATE + fluctuation;
    }
}

