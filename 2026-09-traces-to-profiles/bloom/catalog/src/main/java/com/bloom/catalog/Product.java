package com.bloom.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;

@Entity
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "product_seq")
    @SequenceGenerator(name = "product_seq", sequenceName = "product_seq", allocationSize = 100)
    private Long id;

    @Column(unique = true, nullable = false)
    private String sku;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    private String category;

    private long priceCents;

    private int stock;

    protected Product() {
    }

    public Product(String sku, String name, String description, String category, long priceCents, int stock) {
        this.sku = sku;
        this.name = name;
        this.description = description;
        this.category = category;
        this.priceCents = priceCents;
        this.stock = stock;
    }

    public Long getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category;
    }

    public long getPriceCents() {
        return priceCents;
    }

    public int getStock() {
        return stock;
    }
}
