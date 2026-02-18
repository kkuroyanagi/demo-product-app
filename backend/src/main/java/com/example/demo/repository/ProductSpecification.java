package com.example.demo.repository;

import com.example.demo.dto.ProductSearchRequest;
import com.example.demo.entity.Product;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class ProductSpecification {

    public static Specification<Product> search(ProductSearchRequest req) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(req.getKeyword())) {
                String kw = "%" + req.getKeyword() + "%";
                predicates.add(cb.or(
                        cb.like(root.get("productName"), kw),
                        cb.like(root.get("productCode"), kw)
                ));
            }
            if (StringUtils.hasText(req.getCategory())) {
                predicates.add(cb.equal(root.get("category"), req.getCategory()));
            }
            if (StringUtils.hasText(req.getStatus())) {
                predicates.add(cb.equal(root.get("status"), req.getStatus()));
            }
            if (req.getPriceMin() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), req.getPriceMin()));
            }
            if (req.getPriceMax() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), req.getPriceMax()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
