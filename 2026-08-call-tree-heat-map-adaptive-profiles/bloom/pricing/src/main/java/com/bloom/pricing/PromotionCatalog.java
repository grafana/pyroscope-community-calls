package com.bloom.pricing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
public class PromotionCatalog {

    private static final Logger log = LoggerFactory.getLogger(PromotionCatalog.class);

    private static final String[] SEASONS = {"SPRING", "SUMMER", "AUTUMN", "WINTER", "HOLIDAY", "FLASH"};

    private final List<Promotion> promotions = new ArrayList<>();

    public PromotionCatalog(@Value("${bloom.pricing.promotion-count}") int promotionCount,
                            @Value("${bloom.pricing.skus-per-promotion}") int skusPerPromotion,
                            @Value("${bloom.pricing.sku-pool-size}") int skuPoolSize) {
        Random random = new Random(7);
        for (int i = 0; i < promotionCount; i++) {
            List<String> skus = new ArrayList<>(skusPerPromotion);
            for (int j = 0; j < skusPerPromotion; j++) {
                skus.add(String.format("BLM-%05d", 1 + random.nextInt(skuPoolSize)));
            }
            promotions.add(new Promotion(
                    SEASONS[random.nextInt(SEASONS.length)] + "-" + (100 + i),
                    skus,
                    BigDecimal.valueOf(5 + random.nextInt(26)),
                    random.nextInt(10) < 7,
                    random.nextInt(10) < 8 ? 1 : 2));
        }
        log.info("loaded {} promotions", promotions.size());
    }

    public List<Promotion> all() {
        return promotions;
    }
}
