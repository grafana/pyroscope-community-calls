package com.bloom.catalog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
public class SeedData implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedData.class);

    private static final String[] SIZES = {"Mini", "Petite", "Classic", "Grand", "Deluxe"};
    private static final String[] COLORS = {"White", "Crimson", "Golden", "Violet", "Blush",
            "Ivory", "Coral", "Midnight", "Amber", "Emerald"};
    private static final String[] FLOWERS = {"Rose", "Tulip", "Orchid", "Peony", "Lily",
            "Fern", "Ivy", "Daisy", "Dahlia", "Iris",
            "Lavender", "Magnolia", "Jasmine", "Camellia", "Sunflower"};
    private static final String[] FORMS = {"Bouquet", "Stem", "Bundle", "Basket", "Wreath", "Pot", "Box", "Garland"};
    private static final String[] OCCASIONS = {"birthdays", "weddings", "anniversaries", "housewarmings",
            "quiet Sunday mornings", "office desks", "spring celebrations", "get-well wishes"};
    private static final String[] COMMENTS = {
            "Arrived fresh and lasted over two weeks.",
            "Beautiful colors, exactly as pictured.",
            "The stems were a bit shorter than expected.",
            "My mother loved it, will order again.",
            "Smelled wonderful, packaging could be better.",
            "Wilted after a few days, disappointing.",
            "Stunning arrangement, worth every cent.",
            "Delivery was quick and the blooms were perfect.",
    };

    private final ProductRepository products;
    private final ReviewRepository reviews;

    public SeedData(ProductRepository products, ReviewRepository reviews) {
        this.products = products;
        this.reviews = reviews;
    }

    @Override
    public void run(String... args) {
        if (products.count() > 0) {
            log.info("catalog already seeded, skipping");
            return;
        }

        Random random = new Random(42);
        int sku = 1;
        long reviewCount = 0;
        List<Product> batch = new ArrayList<>();

        for (String size : SIZES) {
            for (String color : COLORS) {
                for (String flower : FLOWERS) {
                    for (String form : FORMS) {
                        String name = size + " " + color + " " + flower + " " + form;
                        String description = "A " + size.toLowerCase() + " arrangement of "
                                + color.toLowerCase() + " " + flower.toLowerCase()
                                + " blooms, presented as a " + form.toLowerCase()
                                + ". Perfect for " + OCCASIONS[random.nextInt(OCCASIONS.length)] + ".";
                        long priceCents = 500 + random.nextInt(14500);
                        int stock = 10 + random.nextInt(90);
                        batch.add(new Product(String.format("BLM-%05d", sku++), name, description,
                                flower, priceCents, stock));

                        if (batch.size() == 500) {
                            reviewCount += persistBatch(batch, random);
                        }
                    }
                }
            }
        }
        if (!batch.isEmpty()) {
            reviewCount += persistBatch(batch, random);
        }

        log.info("seeded {} products and {} reviews", sku - 1, reviewCount);
    }

    private long persistBatch(List<Product> batch, Random random) {
        List<Product> saved = products.saveAll(batch);
        batch.clear();

        List<Review> productReviews = new ArrayList<>();
        for (Product product : saved) {
            int count = random.nextInt(8);
            for (int i = 0; i < count; i++) {
                productReviews.add(new Review(product.getId(), 2 + random.nextInt(4),
                        COMMENTS[random.nextInt(COMMENTS.length)]));
            }
        }
        reviews.saveAll(productReviews);
        return productReviews.size();
    }
}
