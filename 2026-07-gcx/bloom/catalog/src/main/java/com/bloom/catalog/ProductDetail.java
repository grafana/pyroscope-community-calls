package com.bloom.catalog;

import java.util.List;

public record ProductDetail(long id, String sku, String name, String description, String category,
                            long priceCents, double rating, List<ReviewView> reviews) {

    public record ReviewView(int rating, String comment) {
    }
}
