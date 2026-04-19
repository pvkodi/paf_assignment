package com.sliitreserve.api.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * File Storage Configuration for Local Upload Handling.
 * 
 * Established infrastructure for:
 * - Local filesystem upload directory initialization and validation
 * - File size and MIME type constraints enforcement
 * - Static resource serving of uploaded files via HTTP
 * - Thumbnail directory management
 * 
 * Storage Structure:
 * /uploads/
 *   ├── original/         # Original uploaded files
 *   └── thumbnails/       # 200x200 thumbnails
 * 
 * File Constraints (from spec and research):
 * - Max file size: 5 MB per upload
 * - Allowed MIME types: image/jpeg, image/png, image/gif, image/webp
 * - Allowed extensions: .jpg, .jpeg, .png, .gif, .webp
 * - Max attachments per ticket: 3
 * 
 * Integration Points:
 * - LocalFileStorageService: Uses configured paths for storage operations
 * - TicketAttachmentService: Validates uploads against these constraints
 * - Controller endpoints: Serve static resources via configured URL patterns
 * - Docker volume: /uploads mounted as persistent volume in docker-compose.yml
 */
@Slf4j
@Configuration
public class FileStorageConfig implements WebMvcConfigurer {

    /**
     * Base upload directory path.
     * Default: uploads (at workspace root, outside backend folder)
     * Can be overridden via app.file-storage.upload-dir property
     */
    // Use workspace root directory by default for development
    @Value("${app.file-storage.upload-dir:../../../uploads}")
    private String uploadDir;

    /**
     * Maximum file size in bytes: 5 MB
     */
    @Value("${app.file-storage.max-file-size:5242880}")
    private long maxFileSize;

    /**
     * Allowed file extensions (comma-separated)
     */
    @Value("${app.file-storage.allowed-extensions:jpg,jpeg,png,gif,webp,pdf}")
    private String allowedExtensions;

    /**
     * Allowed MIME types (comma-separated)
     */
    @Value("${app.file-storage.allowed-mime-types:image/jpeg,image/png,image/gif,image/webp,application/pdf}")
    private String allowedMimeTypes;

    /**
     * Thumbnail dimensions in pixels (square: widthxheight)
     */
    @Value("${app.file-storage.thumbnail-dimensions:200x200}")
    private String thumbnailDimensions;

    /**
     * Bean providing file storage configuration properties.
     * 
     * @return FileStorageProperties with all configured values
     */
    @Bean
    public FileStorageProperties fileStorageProperties() {
        log.info("Initializing FileStorageProperties");
        log.info("Upload directory: {}", uploadDir);
        log.info("Max file size: {} bytes (5 MB)", maxFileSize);
        log.info("Allowed extensions: {}", allowedExtensions);
        log.info("Allowed MIME types: {}", allowedMimeTypes);
        log.info("Thumbnail dimensions: {}", thumbnailDimensions);

        // Ensure upload directories exist
        try {
            Path uploadPath = Paths.get(uploadDir);
            Path originalPath = uploadPath.resolve("original");
            Path thumbnailPath = uploadPath.resolve("thumbnails");

            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                log.info("Created upload directory: {}", uploadPath.toAbsolutePath());
            }

            if (!Files.exists(originalPath)) {
                Files.createDirectories(originalPath);
                log.info("Created original files directory: {}", originalPath.toAbsolutePath());
            }

            if (!Files.exists(thumbnailPath)) {
                Files.createDirectories(thumbnailPath);
                log.info("Created thumbnails directory: {}", thumbnailPath.toAbsolutePath());
            }
        } catch (Exception e) {
            log.error("Failed to initialize upload directories", e);
            throw new RuntimeException("Failed to initialize upload directories", e);
        }

        return new FileStorageProperties(
            uploadDir,
            maxFileSize,
            allowedExtensions.split(","),
            allowedMimeTypes.split(","),
            thumbnailDimensions
        );
    }

    /**
     * Configures static resource serving for uploaded files.
     * Maps /api/uploads/** HTTP requests to the local /uploads filesystem directory.
     * 
     * URL Pattern: http://localhost:8080/api/uploads/original/{filename}
     *             http://localhost:8080/api/uploads/thumbnails/{filename}
     * 
     * Enables:
     * - Frontend access to uploaded files via HTTP GET
     * - Thumbnail serving for preview and gallery views
     * - CORS-safe static resource delivery
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        log.info("Configuring static resource handlers for uploaded files");
        
        registry.addResourceHandler("/api/uploads/**")
            .addResourceLocations("file:" + uploadDir + "/")
            // Enable cache control for static resources (24 hours)
            .setCacheControl(org.springframework.http.CacheControl.maxAge(
                java.time.Duration.ofHours(24)
            ));
        
        log.info("Registered resource handler: /api/uploads/** -> file:{}", uploadDir);
    }

    /**
     * Immutable file storage properties container.
     * Holds all validated file storage configuration values.
     */
    public static class FileStorageProperties {
        public final String uploadDir;
        public final long maxFileSize;
        public final String[] allowedExtensions;
        public final String[] allowedMimeTypes;
        public final String thumbnailDimensions;

        public FileStorageProperties(
            String uploadDir,
            long maxFileSize,
            String[] allowedExtensions,
            String[] allowedMimeTypes,
            String thumbnailDimensions
        ) {
            this.uploadDir = uploadDir;
            this.maxFileSize = maxFileSize;
            this.allowedExtensions = allowedExtensions;
            this.allowedMimeTypes = allowedMimeTypes;
            this.thumbnailDimensions = thumbnailDimensions;
        }

        /**
         * Parse thumbnail dimensions (format: "200x200")
         * @return array [width, height]
         */
        public int[] getThumbnailDimensions() {
            String[] parts = thumbnailDimensions.split("x");
            return new int[]{
                Integer.parseInt(parts[0].trim()),
                Integer.parseInt(parts[1].trim())
            };
        }
    }
}
