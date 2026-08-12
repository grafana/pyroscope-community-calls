package com.bloom.pricing;

import java.util.List;

public record QuoteRequest(List<LineItem> items, String customerTier) {
}
