package com.bloom.catalog;

public record ProductSummary(long id, String sku, String name, String category, long priceCents,
                             double rating, int reviewCount) {
}
