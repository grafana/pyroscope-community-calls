package com.bloom.gateway;

import java.util.List;

public record CheckoutResponse(long orderId, long subtotalCents, long discountCents, long totalCents,
                               List<String> appliedPromotions) {
}
