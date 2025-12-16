package com.example.smartshelfx.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@Slf4j
public class FileStorageService {

    @Value("${app.upload.images-dir}")
    private String imagesDir;

    @Value("${app.upload.csv-dir}")
    private String csvDir;

    @Value("${app.upload.reports-dir}")
    private String reportsDir;

    public String storeProductImage(MultipartFile file) throws IOException {
        return storeFile(file, imagesDir, "product_");
    }

    public String storeCSVFile(MultipartFile file) throws IOException {
        return storeFile(file, csvDir, "import_");
    }

    public String storeReportFile(MultipartFile file) throws IOException {
        return storeFile(file, reportsDir, "report_");
    }

    private String storeFile(MultipartFile file, String directory, String prefix) throws IOException {
        // Create directory if it doesn't exist
        Path uploadPath = Paths.get(directory);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String fileExtension = getFileExtension(originalFilename);
        String uniqueFilename = prefix + UUID.randomUUID() + fileExtension;

        // Store file
        Path filePath = uploadPath.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        log.info("File stored successfully: {}", filePath);
        return uniqueFilename;
    }

    public byte[] loadFile(String filename, String directory) throws IOException {
        Path filePath = Paths.get(directory).resolve(filename);
        if (Files.exists(filePath)) {
            return Files.readAllBytes(filePath);
        }
        throw new IOException("File not found: " + filename);
    }

    public boolean deleteFile(String filename, String directory) {
        try {
            Path filePath = Paths.get(directory).resolve(filename);
            return Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.error("Error deleting file {}: {}", filename, e.getMessage());
            return false;
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    // Initialize directories on startup
    @jakarta.annotation.PostConstruct
    public void init() {
        try {
            Files.createDirectories(Paths.get(imagesDir));
            Files.createDirectories(Paths.get(csvDir));
            Files.createDirectories(Paths.get(reportsDir));
            log.info("File upload directories initialized");
        } catch (IOException e) {
            log.error("Could not create upload directories: {}", e.getMessage());
        }
    }
}