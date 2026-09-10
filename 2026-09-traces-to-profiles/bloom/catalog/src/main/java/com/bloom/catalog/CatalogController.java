package com.bloom.catalog;

import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/products")
public class CatalogController {

    private final CatalogService catalog;

    public CatalogController(CatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping
    public Page<ProductSummary> list(@RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "20") int size) {
        return catalog.list(page, size);
    }

    @GetMapping("/search")
    public List<ProductSummary> search(@RequestParam String q) {
        return catalog.search(q);
    }

    @GetMapping("/{id}")
    public ProductDetail get(@PathVariable long id) {
        return catalog.get(id);
    }
}
