package com.bloom.orders;

public record OrderConfirmation(long orderId, String status, long totalCents) {
}
