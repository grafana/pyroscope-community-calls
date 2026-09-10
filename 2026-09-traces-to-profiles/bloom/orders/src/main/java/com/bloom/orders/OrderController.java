package com.bloom.orders;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orders;

    public OrderController(OrderService orders) {
        this.orders = orders;
    }

    @PostMapping
    public OrderConfirmation place(@RequestBody OrderRequest request) {
        return orders.place(request);
    }

    @GetMapping(value = "/{orderId}/receipt", produces = MediaType.TEXT_PLAIN_VALUE)
    public String receipt(@PathVariable long orderId) {
        return orders.receipt(orderId);
    }
}
