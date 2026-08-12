package com.bloom.pricing;

import java.util.List;

public record QuoteResponse(long subtotalCents, long discountCents, long totalCents,
                            List<String> appliedPromotions) {
}
