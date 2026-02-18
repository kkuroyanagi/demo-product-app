package com.example.demo.service;

import com.example.demo.dto.ImportError;
import com.example.demo.dto.ImportResult;
import com.example.demo.dto.ProductSearchRequest;
import com.example.demo.entity.Product;
import com.example.demo.repository.CategoryRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.ProductSpecification;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExcelServiceImpl implements ExcelService {

    private static final String[] HEADERS = {
            "商品コード", "商品名", "カテゴリ", "単価", "在庫数量", "ステータス", "説明"
    };
    private static final Set<String> VALID_STATUSES = Set.of("ACTIVE", "INACTIVE", "DISCONTINUED");

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Override
    @Transactional(readOnly = true)
    public void exportExcel(ProductSearchRequest request, OutputStream outputStream) {
        List<Product> products = productRepository.findAll(
                ProductSpecification.search(request),
                Sort.by(Sort.Direction.ASC, "id"));

        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100)) {
            Sheet sheet = workbook.createSheet("商品一覧");

            // Header row
            Row headerRow = sheet.createRow(0);
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            int rowNum = 1;
            for (Product product : products) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(product.getProductCode());
                row.createCell(1).setCellValue(product.getProductName());
                row.createCell(2).setCellValue(product.getCategory());
                row.createCell(3).setCellValue(product.getPrice().doubleValue());
                row.createCell(4).setCellValue(product.getStockQuantity());
                row.createCell(5).setCellValue(product.getStatus());
                row.createCell(6).setCellValue(
                        product.getDescription() != null ? product.getDescription() : "");
            }

            workbook.write(outputStream);
        } catch (IOException e) {
            throw new RuntimeException("Excel export failed", e);
        }
    }

    @Override
    @Transactional
    public ImportResult importExcel(MultipartFile file) {
        Set<String> validCategories = categoryRepository.findAll().stream()
                .map(c -> c.getCategoryName())
                .collect(Collectors.toSet());

        List<ImportError> errors = new ArrayList<>();
        List<Product> toSave = new ArrayList<>();
        int insertedCount = 0;
        int updatedCount = 0;
        int totalRows = 0;

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            int lastRow = sheet.getLastRowNum();

            for (int i = 1; i <= lastRow; i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                totalRows++;
                List<ImportError> rowErrors = new ArrayList<>();

                String productCode = getCellStringValue(row, 0);
                String productName = getCellStringValue(row, 1);
                String category = getCellStringValue(row, 2);
                BigDecimal price = getCellNumericValue(row, 3);
                Integer stockQuantity = getCellIntegerValue(row, 4);
                String status = getCellStringValue(row, 5);
                String description = getCellStringValue(row, 6);

                // Validation
                if (!StringUtils.hasText(productCode)) {
                    rowErrors.add(ImportError.builder().row(i + 1).field("商品コード").message("商品コードは必須です").build());
                } else if (productCode.length() > 20 || !productCode.matches("^[a-zA-Z0-9\\-]+$")) {
                    rowErrors.add(ImportError.builder().row(i + 1).field("商品コード").message("商品コードは半角英数字とハイフンで20文字以内です").build());
                }
                if (!StringUtils.hasText(productName)) {
                    rowErrors.add(ImportError.builder().row(i + 1).field("商品名").message("商品名は必須です").build());
                } else if (productName.length() > 200) {
                    rowErrors.add(ImportError.builder().row(i + 1).field("商品名").message("商品名は200文字以内です").build());
                }
                if (!StringUtils.hasText(category)) {
                    rowErrors.add(ImportError.builder().row(i + 1).field("カテゴリ").message("カテゴリは必須です").build());
                } else if (!validCategories.contains(category)) {
                    rowErrors.add(ImportError.builder().row(i + 1).field("カテゴリ").message("無効なカテゴリです").build());
                }
                if (price == null) {
                    rowErrors.add(ImportError.builder().row(i + 1).field("単価").message("単価は必須です").build());
                } else if (price.compareTo(BigDecimal.ZERO) < 0) {
                    rowErrors.add(ImportError.builder().row(i + 1).field("単価").message("単価は0以上の数値を指定してください").build());
                }
                if (stockQuantity == null) {
                    rowErrors.add(ImportError.builder().row(i + 1).field("在庫数量").message("在庫数量は必須です").build());
                } else if (stockQuantity < 0) {
                    rowErrors.add(ImportError.builder().row(i + 1).field("在庫数量").message("在庫数量は0以上の整数を指定してください").build());
                }
                if (!StringUtils.hasText(status)) {
                    rowErrors.add(ImportError.builder().row(i + 1).field("ステータス").message("ステータスは必須です").build());
                } else if (!VALID_STATUSES.contains(status)) {
                    rowErrors.add(ImportError.builder().row(i + 1).field("ステータス").message("ステータスはACTIVE/INACTIVE/DISCONTINUEDのいずれかです").build());
                }

                if (!rowErrors.isEmpty()) {
                    errors.addAll(rowErrors);
                    continue;
                }

                // Upsert
                Optional<Product> existing = productRepository.findByProductCode(productCode);
                Product product;
                if (existing.isPresent()) {
                    product = existing.get();
                    updatedCount++;
                } else {
                    product = new Product();
                    product.setProductCode(productCode);
                    insertedCount++;
                }
                product.setProductName(productName);
                product.setCategory(category);
                product.setPrice(price);
                product.setStockQuantity(stockQuantity);
                product.setStatus(status);
                product.setDescription(description);
                toSave.add(product);
            }

            if (!toSave.isEmpty()) {
                productRepository.saveAll(toSave);
            }

        } catch (IOException e) {
            throw new RuntimeException("Excel import failed", e);
        }

        return ImportResult.builder()
                .success(true)
                .totalRows(totalRows)
                .insertedCount(insertedCount)
                .updatedCount(updatedCount)
                .errorCount(errors.size())
                .errors(errors)
                .build();
    }

    private String getCellStringValue(Row row, int cellIndex) {
        Cell cell = row.getCell(cellIndex);
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BLANK -> null;
            default -> cell.toString().trim();
        };
    }

    private BigDecimal getCellNumericValue(Row row, int cellIndex) {
        Cell cell = row.getCell(cellIndex);
        if (cell == null) return null;
        try {
            return switch (cell.getCellType()) {
                case NUMERIC -> BigDecimal.valueOf(cell.getNumericCellValue());
                case STRING -> new BigDecimal(cell.getStringCellValue().trim());
                default -> null;
            };
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer getCellIntegerValue(Row row, int cellIndex) {
        Cell cell = row.getCell(cellIndex);
        if (cell == null) return null;
        try {
            return switch (cell.getCellType()) {
                case NUMERIC -> (int) cell.getNumericCellValue();
                case STRING -> Integer.parseInt(cell.getStringCellValue().trim());
                default -> null;
            };
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
