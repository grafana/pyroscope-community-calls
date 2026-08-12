package com.bloom.pricing;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class DiscountEngine {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final PromotionCatalog promotions;

    public DiscountEngine(PromotionCatalog promotions) {
        this.promotions = promotions;
    }

    public QuoteResponse quote(QuoteRequest request) {
        List<LineItem> items = request.items() == null ? List.of() : request.items();
        if (items.isEmpty()) {
            return new QuoteResponse(0, 0, 0, List.of());
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        for (LineItem item : items) {
            BigDecimal unit = BigDecimal.valueOf(item.unitPriceCents()).divide(HUNDRED, 10, RoundingMode.HALF_UP);
            subtotal = subtotal.add(unit.multiply(BigDecimal.valueOf(item.quantity())));
        }

        List<Promotion> eligible = new ArrayList<>();
        for (LineItem item : items) {
            for (Promotion promotion : promotions.all()) {
                if (promotion.appliesTo(item) && !containsCode(eligible, promotion.code())) {
                    eligible.add(promotion);
                }
            }
        }

        // Customers get the single best promotion, or the best pair of
        // stackable promotions, whichever saves them more.
        BigDecimal bestDiscount = BigDecimal.ZERO;
        List<String> bestCodes = List.of();
        for (int i = 0; i < eligible.size(); i++) {
            Promotion first = eligible.get(i);
            BigDecimal single = discountFor(first, items);
            if (single.compareTo(bestDiscount) > 0) {
                bestDiscount = single;
                bestCodes = List.of(first.code());
            }
            if (!first.stackable()) {
                continue;
            }
            for (int j = i + 1; j < eligible.size(); j++) {
                Promotion second = eligible.get(j);
                if (!second.stackable()) {
                    continue;
                }
                // The second promotion is applied to what remains after the first.
                BigDecimal remainder = BigDecimal.ONE.subtract(
                        discountFor(first, items).divide(subtotal, 10, RoundingMode.HALF_UP));
                BigDecimal combined = discountFor(first, items)
                        .add(discountFor(second, items).multiply(remainder));
                if (combined.compareTo(bestDiscount) > 0) {
                    bestDiscount = combined;
                    bestCodes = List.of(first.code(), second.code());
                }
            }
        }

        BigDecimal tierRate = tierRate(request.customerTier());
        BigDecimal tierDiscount = subtotal.subtract(bestDiscount).multiply(tierRate);

        List<String> applied = new ArrayList<>(bestCodes);
        if (tierDiscount.compareTo(BigDecimal.ZERO) > 0) {
            applied.add("TIER-" + request.customerTier());
        }

        long subtotalCents = toCents(subtotal);
        long discountCents = toCents(bestDiscount.add(tierDiscount));
        return new QuoteResponse(subtotalCents, discountCents, subtotalCents - discountCents, applied);
    }

    private BigDecimal discountFor(Promotion promotion, List<LineItem> items) {
        BigDecimal discount = BigDecimal.ZERO;
        for (LineItem item : items) {
            if (!promotion.appliesTo(item)) {
                continue;
            }
            BigDecimal line = BigDecimal.valueOf(item.unitPriceCents())
                    .multiply(BigDecimal.valueOf(item.quantity()))
                    .divide(HUNDRED, 10, RoundingMode.HALF_UP);
            discount = discount.add(line.multiply(promotion.percentOff()).divide(HUNDRED, 10, RoundingMode.HALF_UP));
        }
        return discount;
    }

    private static boolean containsCode(List<Promotion> eligible, String code) {
        for (Promotion promotion : eligible) {
            if (promotion.code().equals(code)) {
                return true;
            }
        }
        return false;
    }

    private static BigDecimal tierRate(String customerTier) {
        if (customerTier == null) {
            return BigDecimal.ZERO;
        }
        return switch (customerTier) {
            case "GOLD" -> new BigDecimal("0.05");
            case "SILVER" -> new BigDecimal("0.02");
            default -> BigDecimal.ZERO;
        };
    }

    private static long toCents(BigDecimal amount) {
        return amount.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }
}
