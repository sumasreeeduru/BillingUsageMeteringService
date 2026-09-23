package usagebilling.repository;

import usagebilling.model.ResourceType;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;

public class InMemoryPriceRepository
        implements PriceRepository {

    private final Map<ResourceType, BigDecimal> prices =
            new EnumMap<>(ResourceType.class);

    private final String currency;

    public InMemoryPriceRepository() {

        this.currency = "USD";

        prices.put(
                ResourceType.DATABASE,
                new BigDecimal("0.05")
        );

        prices.put(
                ResourceType.LOAD_BALANCER,
                new BigDecimal("0.02")
        );

        prices.put(
                ResourceType.COMPUTE,
                new BigDecimal("0.03")
        );

        prices.put(
                ResourceType.STORAGE,
                new BigDecimal("0.01")
        );
    }

    @Override
    public BigDecimal getPrice(
            ResourceType resourceType) {

        return prices.get(resourceType);
    }

    @Override
    public String getCurrency() {
        return currency;
    }
}