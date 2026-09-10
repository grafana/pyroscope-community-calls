package com.bloom.pricing;

import java.math.BigDecimal;
import java.util.List;

public record Promotion(String code, List<String> skus, BigDecimal percentOff, boolean stackable, int minQuantity) {

    public boolean appliesTo(LineItem item) {
        return item.quantity() >= minQuantity && skus.contains(item.sku());
    }
}
