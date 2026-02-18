package com.example.demo.service;

import com.example.demo.dto.PageResponse;
import com.example.demo.dto.ProductSearchRequest;
import com.example.demo.entity.Product;
import com.example.demo.exception.BusinessException;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.ProductSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    @Override
    public PageResponse<Product> search(ProductSearchRequest request) {
        Sort sort = parseSort(request.getSorter());
        Pageable pageable = PageRequest.of(request.getCurrent() - 1, request.getPageSize(), sort);

        Page<Product> page = productRepository.findAll(
                ProductSpecification.search(request), pageable);

        return PageResponse.<Product>builder()
                .data(page.getContent())
                .total(page.getTotalElements())
                .success(true)
                .current(request.getCurrent())
                .pageSize(request.getPageSize())
                .build();
    }

    @Override
    public Product findById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new BusinessException("商品が見つかりません: ID=" + id));
    }

    @Override
    @Transactional
    public Product create(Product product) {
        return productRepository.save(product);
    }

    @Override
    @Transactional
    public Product update(Long id, Product product) {
        Product existing = findById(id);
        existing.setProductCode(product.getProductCode());
        existing.setProductName(product.getProductName());
        existing.setCategory(product.getCategory());
        existing.setPrice(product.getPrice());
        existing.setStockQuantity(product.getStockQuantity());
        existing.setStatus(product.getStatus());
        existing.setDescription(product.getDescription());
        return productRepository.save(existing);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Product existing = findById(id);
        productRepository.delete(existing);
    }

    private Sort parseSort(String sorter) {
        if (!StringUtils.hasText(sorter)) {
            return Sort.by(Sort.Direction.ASC, "id");
        }
        String[] parts = sorter.split(",");
        if (parts.length != 2) {
            return Sort.by(Sort.Direction.ASC, "id");
        }
        Sort.Direction direction = "desc".equalsIgnoreCase(parts[1])
                ? Sort.Direction.DESC : Sort.Direction.ASC;
        return Sort.by(direction, parts[0]);
    }
}
