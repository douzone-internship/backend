package com.douzone_internship.backend.service.ai;

public record PriceStats(int min, int max, int avg, int median, int sampleSize) {

    public static PriceStats empty() {
        return new PriceStats(0, 0, 0, 0, 0);
    }

    public boolean isEmpty() {
        return sampleSize == 0;
    }
}
