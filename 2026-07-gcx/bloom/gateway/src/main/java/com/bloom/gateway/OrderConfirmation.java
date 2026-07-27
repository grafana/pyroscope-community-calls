package com.bloom.gateway;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderConfirmation(long orderId, String status, long totalCents) {
}
