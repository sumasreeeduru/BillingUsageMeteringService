package usagebilling.repository;

import usagebilling.model.ResourceType;

import java.math.BigDecimal;

public interface PriceRepository {

    BigDecimal getPrice(ResourceType resourceType);

    String getCurrency();
}