package com.bloom.pricing;

import io.pyroscope.labels.v2.LabelsSet;
import io.pyroscope.labels.v2.ScopedContext;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class QuoteController {

    private final DiscountEngine engine;

    public QuoteController(DiscountEngine engine) {
        this.engine = engine;
    }

    @PostMapping("/quote")
    public QuoteResponse quote(@RequestBody QuoteRequest request) {
        int items = request.items() == null ? 0 : request.items().size();
        try (ScopedContext labels = new ScopedContext(new LabelsSet(
                "customer_tier", request.customerTier() == null ? "STANDARD" : request.customerTier(),
                "cart_size", items >= 5 ? "5+" : String.valueOf(items)))) {
            return engine.quote(request);
        }
    }
}
