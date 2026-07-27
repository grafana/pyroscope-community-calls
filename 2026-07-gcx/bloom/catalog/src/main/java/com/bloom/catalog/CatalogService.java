package com.bloom.catalog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class CatalogService {

    private static final int MAX_SEARCH_RESULTS = 25;

    private final ProductRepository products;
    private final ReviewRepository reviews;

    public CatalogService(ProductRepository products, ReviewRepository reviews) {
        this.products = products;
        this.reviews = reviews;
    }

    public Page<ProductSummary> list(int page, int size) {
        return products.findAll(PageRequest.of(page, size, Sort.by("id"))).map(this::toSummary);
    }

    public ProductDetail get(long id) {
        Product product = products.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "no such product"));
        List<Review> productReviews = reviews.findByProductId(product.getId());
        double rating = productReviews.stream().mapToInt(Review::getRating).average().orElse(0.0);
        List<ProductDetail.ReviewView> views = productReviews.stream()
                .map(r -> new ProductDetail.ReviewView(r.getRating(), r.getComment()))
                .toList();
        return new ProductDetail(product.getId(), product.getSku(), product.getName(), product.getDescription(),
                product.getCategory(), product.getPriceCents(), round(rating), views);
    }

    public List<ProductSummary> search(String query) {
        List<Product> catalog = products.findAll();
        List<Product> hits = new ArrayList<>();
        for (Product product : catalog) {
            if (matches(product.getName(), query) || matches(product.getDescription(), query)) {
                hits.add(product);
            }
        }
        hits.sort(Comparator.comparing(Product::getName));
        return hits.stream().limit(MAX_SEARCH_RESULTS).map(this::toSummary).toList();
    }

    private boolean matches(String text, String query) {
        return text != null && text.matches("(?i).*" + Pattern.quote(query) + ".*");
    }

    private ProductSummary toSummary(Product product) {
        List<Review> productReviews = reviews.findByProductId(product.getId());
        double rating = productReviews.stream().mapToInt(Review::getRating).average().orElse(0.0);
        return new ProductSummary(product.getId(), product.getSku(), product.getName(), product.getCategory(),
                product.getPriceCents(), round(rating), productReviews.size());
    }

    private static double round(double rating) {
        return Math.round(rating * 10.0) / 10.0;
    }
}
