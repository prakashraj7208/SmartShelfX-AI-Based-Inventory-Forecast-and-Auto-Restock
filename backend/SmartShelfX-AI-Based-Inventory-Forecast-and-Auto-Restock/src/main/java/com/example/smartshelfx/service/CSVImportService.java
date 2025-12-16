package com.example.smartshelfx.service;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvValidationException;
import com.example.smartshelfx.model.Product;
import com.example.smartshelfx.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

@Service
public class CSVImportService {  // Changed class name to match filename

    @Autowired
    private ProductRepository productRepository;

    public List<Product> importProductsFromCSV(MultipartFile file) throws IOException {
        List<Product> importedProducts = new ArrayList<>();

        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
            String[] nextLine;
            boolean isHeader = true;

            while ((nextLine = reader.readNext()) != null) {
                if (isHeader) {
                    isHeader = false;
                    continue; // Skip header row
                }

                if (nextLine.length >= 4) {
                    Product product = new Product();
                    product.setName(nextLine[0]);
                    product.setSku(nextLine[1]);
                    product.setCategory(nextLine[2]);
                    product.setDescription(nextLine.length > 3 ? nextLine[3] : "");
                    product.setCurrentStock(0);
                    product.setReorderLevel(10);

                    importedProducts.add(product);
                }
            }
        } catch (CsvValidationException e) {
            throw new IOException("CSV validation error", e);
        }

        return productRepository.saveAll(importedProducts);
    }

    public byte[] exportProductsToCSV() throws IOException {
        List<Product> products = productRepository.findAll();

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             CSVWriter writer = new CSVWriter(new OutputStreamWriter(outputStream))) {

            // Write header
            String[] header = {"Name", "SKU", "Category", "Description", "Current Stock", "Reorder Level", "Price"};
            writer.writeNext(header);

            // Write data
            for (Product product : products) {
                String[] data = {
                        product.getName(),
                        product.getSku(),
                        product.getCategory(),
                        product.getDescription() != null ? product.getDescription() : "",
                        String.valueOf(product.getCurrentStock()),
                        String.valueOf(product.getReorderLevel()),
                        product.getPrice() != null ? String.valueOf(product.getPrice()) : ""
                };
                writer.writeNext(data);
            }

            writer.flush();
            return outputStream.toByteArray();
        }
    }

    public byte[] exportLowStockReport() throws IOException {
        List<Product> lowStockProducts = productRepository.findLowStockProducts();

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             CSVWriter writer = new CSVWriter(new OutputStreamWriter(outputStream))) {

            String[] header = {"Name", "SKU", "Category", "Current Stock", "Reorder Level", "Status"};
            writer.writeNext(header);

            for (Product product : lowStockProducts) {
                String status = product.getCurrentStock() <= product.getMinStock() ? "CRITICAL" : "LOW";
                String[] data = {
                        product.getName(),
                        product.getSku(),
                        product.getCategory(),
                        String.valueOf(product.getCurrentStock()),
                        String.valueOf(product.getReorderLevel()),
                        status
                };
                writer.writeNext(data);
            }

            writer.flush();
            return outputStream.toByteArray();
        }
    }
}