package com.douzone_internship.backend.service.ai;

import com.douzone_internship.backend.dto.response.ResultItemDTO;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class PriceStatsCalculator {

    public PriceStats compute(List<ResultItemDTO> items) {
        if (items == null || items.isEmpty()) {
            return PriceStats.empty();
        }

        List<ResultItemDTO> valid = items.stream()
                .filter(it -> it.minPrice() > 0 && it.maxPrice() > 0)
                .toList();

        if (valid.isEmpty()) {
            return PriceStats.empty();
        }

        int min = valid.stream().mapToInt(ResultItemDTO::minPrice).min().getAsInt();
        int max = valid.stream().mapToInt(ResultItemDTO::maxPrice).max().getAsInt();

        int[] midpoints = valid.stream()
                .mapToInt(it -> (it.minPrice() + it.maxPrice()) / 2)
                .toArray();

        int avg = (int) Math.round(Arrays.stream(midpoints).average().getAsDouble());
        int median = median(midpoints);

        return new PriceStats(min, max, avg, median, valid.size());
    }

    private int median(int[] arr) {
        int[] sorted = arr.clone();
        Arrays.sort(sorted);
        int n = sorted.length;
        return n % 2 == 1
                ? sorted[n / 2]
                : (sorted[n / 2 - 1] + sorted[n / 2]) / 2;
    }
}
