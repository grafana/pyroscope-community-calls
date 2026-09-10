package com.bloom.orders;

import java.util.List;

public record OrderRequest(String customerId, List<Line> lines, long subtotalCents, long discountCents,
                           long totalCents, List<String> promotions) {

    public record Line(String sku, String name, int quantity, long unitPriceCents) {
    }
}
