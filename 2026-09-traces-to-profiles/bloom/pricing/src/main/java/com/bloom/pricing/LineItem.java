package com.bloom.pricing;

public record LineItem(String sku, String name, int quantity, long unitPriceCents) {
}
