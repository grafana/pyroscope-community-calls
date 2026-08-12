package com.bloom.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component
public class DownstreamClient {

    @Value("${bloom.downstream.catalog}")
    private String catalogUrl;

    @Value("${bloom.downstream.pricing}")
    private String pricingUrl;

    @Value("${bloom.downstream.orders}")
    private String ordersUrl;

    public String productsPage(int page, int size) {
        RestTemplate http = new RestTemplate();
        return http.getForObject(catalogUrl + "/products?page=" + page + "&size=" + size, String.class);
    }

    public String searchCatalog(String query) {
        RestTemplate http = new RestTemplate();
        return http.getForObject(catalogUrl + "/products/search?q={q}", String.class, query);
    }

    public CatalogProduct product(long productId) {
        RestTemplate http = new RestTemplate();
        return http.getForObject(catalogUrl + "/products/" + productId, CatalogProduct.class);
    }

    public String productDetail(long productId) {
        RestTemplate http = new RestTemplate();
        return http.getForObject(catalogUrl + "/products/" + productId, String.class);
    }

    public Quote quote(List<QuoteLine> lines, String customerTier) {
        RestTemplate http = new RestTemplate();
        Map<String, Object> payload = Map.of(
                "items", lines,
                "customerTier", customerTier == null ? "STANDARD" : customerTier);
        return http.postForObject(pricingUrl + "/quote", payload, Quote.class);
    }

    public OrderConfirmation placeOrder(String customerId, List<QuoteLine> lines, Quote quote) {
        RestTemplate http = new RestTemplate();
        Map<String, Object> payload = Map.of(
                "customerId", customerId,
                "lines", lines,
                "subtotalCents", quote.subtotalCents(),
                "discountCents", quote.discountCents(),
                "totalCents", quote.totalCents(),
                "promotions", quote.appliedPromotions());
        return http.postForObject(ordersUrl + "/orders", payload, OrderConfirmation.class);
    }

    public String receipt(long orderId) {
        RestTemplate http = new RestTemplate();
        return http.getForObject(ordersUrl + "/orders/" + orderId + "/receipt", String.class);
    }
}
