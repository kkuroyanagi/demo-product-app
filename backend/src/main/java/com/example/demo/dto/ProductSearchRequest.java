package com.example.demo.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductSearchRequest {

    @Size(max = 100)
    private String keyword;

    private String category;

    private String status;

    @Min(0)
    private BigDecimal priceMin;

    @Min(0)
    private BigDecimal priceMax;

    @Min(1)
    private Integer current = 1;

    @Min(1)
    @Max(100)
    private Integer pageSize = 20;

    private String sorter;
}
