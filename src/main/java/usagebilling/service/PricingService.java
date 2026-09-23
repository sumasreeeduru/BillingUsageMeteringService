package usagebilling.service;

import usagebilling.model.ResourceType;
import usagebilling.model.ResourceUsage;
import usagebilling.model.UsageSummary;
import usagebilling.repository.PriceRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PricingService {

    private final PriceRepository priceRepository;

    public PricingService(
            PriceRepository priceRepository) {

        this.priceRepository = priceRepository;
    }

    public UsageSummary createSummary(
            String customerId,
            Instant start,
            Instant end,
            Map<ResourceType, BigDecimal> usage) {

        List<ResourceUsage> resources =
                new ArrayList<>();

        BigDecimal totalCost =
                BigDecimal.ZERO;

        for (Map.Entry<ResourceType, BigDecimal> entry
                : usage.entrySet()) {

            ResourceType resourceType =
                    entry.getKey();

            BigDecimal quantity =
                    entry.getValue();

            BigDecimal price =
                    priceRepository.getPrice(
                            resourceType
                    );

            if (price == null) {
                continue;
            }

            BigDecimal cost =
                    quantity
                            .multiply(price)
                            .setScale(
                                    2,
                                    RoundingMode.HALF_UP
                            );

            resources.add(
                    new ResourceUsage(
                            resourceType,
                            quantity,
                            price,
                            cost
                    )
            );

            totalCost =
                    totalCost.add(cost);
        }

        totalCost =
                totalCost.setScale(
                        2,
                        RoundingMode.HALF_UP
                );

        return new UsageSummary(
                customerId,
                start,
                end,
                resources,
                totalCost,
                priceRepository.getCurrency()
        );
    }
}