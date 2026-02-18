package com.example.demo.service;

import com.example.demo.dto.PageResponse;
import com.example.demo.dto.ProductSearchRequest;
import com.example.demo.entity.Product;

public interface ProductService {

    PageResponse<Product> search(ProductSearchRequest request);

    Product findById(Long id);

    Product create(Product product);

    Product update(Long id, Product product);

    void delete(Long id);
}
