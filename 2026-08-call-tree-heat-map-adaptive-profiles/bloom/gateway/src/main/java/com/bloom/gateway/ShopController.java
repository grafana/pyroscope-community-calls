package com.bloom.gateway;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
public class ShopController {

    private final DownstreamClient downstream;

    public ShopController(DownstreamClient downstream) {
        this.downstream = downstream;
    }

    @GetMapping(value = "/shop/products", produces = MediaType.APPLICATION_JSON_VALUE)
    public String products(@RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "20") int size) {
        return downstream.productsPage(page, size);
    }

    @GetMapping(value = "/shop/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public String search(@RequestParam String q) {
        return downstream.searchCatalog(q);
    }

    @GetMapping(value = "/shop/products/{productId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public String productDetail(@PathVariable long productId) {
        return downstream.productDetail(productId);
    }

    @PostMapping("/shop/checkout")
    public CheckoutResponse checkout(@RequestBody CheckoutRequest request) {
        List<QuoteLine> lines = new ArrayList<>();
        for (CartItem item : request.items()) {
            CatalogProduct product = downstream.product(item.productId());
            lines.add(new QuoteLine(product.sku(), product.name(), item.quantity(), product.priceCents()));
        }

        Quote quote = downstream.quote(lines, request.customerTier());
        OrderConfirmation order = downstream.placeOrder(request.customerId(), lines, quote);

        return new CheckoutResponse(
                order.orderId(),
                quote.subtotalCents(),
                quote.discountCents(),
                quote.totalCents(),
                quote.appliedPromotions());
    }

    @GetMapping(value = "/shop/orders/{orderId}/receipt", produces = MediaType.TEXT_PLAIN_VALUE)
    public String receipt(@PathVariable long orderId) {
        return downstream.receipt(orderId);
    }
}
