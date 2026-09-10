package com.bloom.pricing;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class DiscountEngine {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final PromotionCatalog promotions;
    private final int refinementRounds;
    private final int maxEligible;

    public DiscountEngine(PromotionCatalog promotions,
                          @Value("${bloom.pricing.refinement-rounds:6}") int refinementRounds,
                          @Value("${bloom.pricing.max-eligible:250}") int maxEligible) {
        this.promotions = promotions;
        this.refinementRounds = refinementRounds;
        this.maxEligible = maxEligible;
    }

    public QuoteResponse quote(QuoteRequest request) {
        List<LineItem> items = request.items() == null ? List.of() : request.items();
        if (items.isEmpty()) {
            return new QuoteResponse(0, 0, 0, List.of());
        }

        BigDecimal subtotal = subtotal(items);
        List<Promotion> eligible = eligiblePromotions(items);

        BundleResult best = bestBundleDiscount(eligible, items, subtotal);

        BigDecimal tierRate = tierRate(request.customerTier());
        BigDecimal tierDiscount = subtotal.subtract(best.discount()).multiply(tierRate);

        List<String> applied = new ArrayList<>(best.codes());
        if (tierDiscount.compareTo(BigDecimal.ZERO) > 0) {
            applied.add("TIER-" + request.customerTier());
        }

        long subtotalCents = toCents(subtotal);
        long discountCents = toCents(best.discount().add(tierDiscount));
        return new QuoteResponse(subtotalCents, discountCents, subtotalCents - discountCents, applied);
    }

    private BigDecimal subtotal(List<LineItem> items) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (LineItem item : items) {
            BigDecimal unit = BigDecimal.valueOf(item.unitPriceCents()).divide(HUNDRED, 10, RoundingMode.HALF_UP);
            subtotal = subtotal.add(unit.multiply(BigDecimal.valueOf(item.quantity())));
        }
        return subtotal;
    }

    // Collect every promotion that applies to any cart line by scanning the
    // whole promotion catalogue for each item, so the eligible set grows with
    // cart size. Capped so a pathologically large cart can't run unbounded.
    private List<Promotion> eligiblePromotions(List<LineItem> items) {
        List<Promotion> eligible = new ArrayList<>();
        for (LineItem item : items) {
            for (Promotion promotion : promotions.all()) {
                if (promotion.appliesTo(item) && !containsCode(eligible, promotion.code())) {
                    eligible.add(promotion);
                    if (eligible.size() >= maxEligible) {
                        return eligible;
                    }
                }
            }
        }
        return eligible;
    }

    // Naive local search for the best single promotion or best pair. It
    // re-scores every pair from scratch on every refinement round and never
    // memoizes discountFor, so cost is O(rounds * eligible^2 * items). Because
    // eligible grows with cart size, a large cart makes this the dominant CPU
    // consumer for the request. Tune bloom.pricing.refinement-rounds to dial
    // the CPU cost of the hot path.
    private BundleResult bestBundleDiscount(List<Promotion> eligible, List<LineItem> items, BigDecimal subtotal) {
        BigDecimal best = BigDecimal.ZERO;
        List<String> bestCodes = List.of();
        for (int round = 0; round < refinementRounds; round++) {
            for (int i = 0; i < eligible.size(); i++) {
                Promotion a = eligible.get(i);
                BigDecimal single = discountFor(a, items);
                if (single.compareTo(best) > 0) {
                    best = single;
                    bestCodes = List.of(a.code());
                }
                for (int j = 0; j < eligible.size(); j++) {
                    if (i == j) {
                        continue;
                    }
                    Promotion b = eligible.get(j);
                    BigDecimal remainder = BigDecimal.ONE.subtract(
                            discountFor(a, items).divide(subtotal, 10, RoundingMode.HALF_UP));
                    BigDecimal combined = discountFor(a, items)
                            .add(discountFor(b, items).multiply(remainder));
                    if (combined.compareTo(best) > 0) {
                        best = combined;
                        bestCodes = List.of(a.code(), b.code());
                    }
                }
            }
        }
        return new BundleResult(best, bestCodes);
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

    private record BundleResult(BigDecimal discount, List<String> codes) {
    }
}
