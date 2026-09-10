package com.bloom.gateway;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CatalogProduct(long id, String sku, String name, String category, long priceCents) {
}
