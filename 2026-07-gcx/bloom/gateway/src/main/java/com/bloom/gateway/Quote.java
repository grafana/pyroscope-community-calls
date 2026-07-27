package com.bloom.gateway;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Quote(long subtotalCents, long discountCents, long totalCents, List<String> appliedPromotions) {
}
