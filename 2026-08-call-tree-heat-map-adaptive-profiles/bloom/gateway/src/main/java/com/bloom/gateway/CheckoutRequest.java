package com.bloom.gateway;

import java.util.List;

public record CheckoutRequest(String customerId, String customerTier, List<CartItem> items) {
}
