package com.example.demo.service;

import com.example.demo.dto.ImportResult;
import com.example.demo.dto.ProductSearchRequest;
import org.springframework.web.multipart.MultipartFile;

import java.io.OutputStream;

public interface ExcelService {

    void exportExcel(ProductSearchRequest request, OutputStream outputStream);

    ImportResult importExcel(MultipartFile file);
}
