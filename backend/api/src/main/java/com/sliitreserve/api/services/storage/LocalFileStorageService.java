package com.sliitreserve.api.services.storage;

import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.sliitreserve.api.config.FileStorageConfig.FileStorageProperties;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Local File Storage Service.
 * 
 * Handles secure file upload, storage, retrieval, and thumbnail generation.
 * 
 * Features:
 * - Secure filename sanitization to prevent path traversal attacks
 * - MIME type and file size validation
 * - File extension validation against configured allowlist
 * - Automatic thumbnail generation (200x200) for images using Thumbnailator
 * - Persistent storage in /uploads/{original,thumbnails} directories
 * - Error handling with descriptive validation messages
 * 
 * Upload Workflow:
 * 1. Validate file size (max 5 MB)
 * 2. Validate MIME type against allowlist
 * 3. Validate file extension against allowlist
 * 4. Sanitize filename (remove dangerous characters, truncate)
 * 5. Generate UUID-based storage filename to guarantee uniqueness
 * 6. Store original file in /uploads/original/
 * 7. Generate 200x200 thumbnail in /uploads/thumbnails/
 * 8. Return storage path and metadata
 * 
 * File Naming Strategy:
 * - Storage filenames: {uuid}-{original-filename-sanitized}
 * - Example: "550e8400-e29b-41d4-a716-446655440000-meeting-notes.jpg"
 * - Prevents naming collisions and maintains original filename reference
 * 
 * Error Handling:
 * - FileSizeExceededException: File exceeds max size limit
 * - InvalidMimeTypeException: MIME type not allowed
 * - InvalidFileExtensionException: File extension not allowed
 * - FileStorageException: Generic storage operation failure (I/O errors, disk full, permissions)
 */
@Slf4j
@Service
public class LocalFileStorageService {

    private final FileStorageProperties storageProperties;

    /**
     * Pattern for valid filenames (alphanumeric, dots, hyphens, underscores only).
     * Used to sanitize uploaded filenames.
     */
    private static final Pattern VALID_FILENAME_PATTERN = Pattern.compile("[^a-zA-Z0-9._-]");

    /**
     * Maximum filename length after sanitization
     */
    private static final int MAX_FILENAME_LENGTH = 255;

    public LocalFileStorageService(FileStorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    /**
     * Upload and store a file with validation and thumbnail generation.
     * 
     * Process:
     * 1. Validate file size
     * 2. Validate MIME type
     * 3. Validate file extension
     * 4. Sanitize filename
     * 5. Store original file
     * 6. Generate and store thumbnail
     * 
     * @param file The uploaded file
     * @return FileUploadResult containing storage path and metadata
     * @throws FileSizeExceededException if file size exceeds limit
     * @throws InvalidMimeTypeException if MIME type is not allowed
     * @throws InvalidFileExtensionException if file extension is not allowed
     * @throws FileStorageException if storage operation fails
     */
    public FileUploadResult uploadFile(MultipartFile file) {
        log.info("Starting file upload: {}, size: {} bytes, MIME: {}",
            file.getOriginalFilename(), file.getSize(), file.getContentType());

        // 1. Validate file size
        if (file.getSize() > storageProperties.maxFileSize) {
            log.error("File size {} exceeds max limit {} for file: {}",
                file.getSize(), storageProperties.maxFileSize, file.getOriginalFilename());
            throw new FileSizeExceededException(
                String.format("File size %d bytes exceeds maximum allowed size %d bytes",
                    file.getSize(), storageProperties.maxFileSize)
            );
        }

        // 2. Validate MIME type
        String contentType = file.getContentType();
        if (!isValidMimeType(contentType)) {
            log.error("Invalid MIME type {} for file: {}", contentType, file.getOriginalFilename());
            throw new InvalidMimeTypeException(
                String.format("MIME type '%s' is not allowed. Allowed types: %s",
                    contentType, String.join(", ", storageProperties.allowedMimeTypes))
            );
        }

        // 3. Validate and extract file extension
        String fileExtension = extractFileExtension(file.getOriginalFilename());
        if (!isValidExtension(fileExtension)) {
            log.error("Invalid file extension '{}' for file: {}", fileExtension, file.getOriginalFilename());
            throw new InvalidFileExtensionException(
                String.format("File extension '.%s' is not allowed. Allowed extensions: %s",
                    fileExtension, String.join(", ", storageProperties.allowedExtensions))
            );
        }

        // 4. Sanitize filename
        String sanitizedFilename = sanitizeFilename(file.getOriginalFilename());

        // 5. Generate unique storage filename
        String uuid = UUID.randomUUID().toString();
        String storageFilename = uuid + "-" + sanitizedFilename;

        try {
            // 6. Store original file
            Path originalPath = Paths.get(storageProperties.uploadDir, "original", storageFilename);
            Files.copy(file.getInputStream(), originalPath);
            log.info("Stored original file at: {}", originalPath.toAbsolutePath());

            // 7. Generate and store thumbnail
            String thumbnailFilename = uuid + "-" + sanitizedFilename; // Same name, different directory
            Path thumbnailPath = Paths.get(storageProperties.uploadDir, "thumbnails", thumbnailFilename);
            
            generateThumbnail(file.getInputStream(), thumbnailPath);
            log.info("Generated thumbnail at: {}", thumbnailPath.toAbsolutePath());

            // Construct URLs for API access
            String originalUrl = "/api/uploads/original/" + storageFilename;
            String thumbnailUrl = "/api/uploads/thumbnails/" + thumbnailFilename;

            FileUploadResult result = new FileUploadResult(
                storageFilename,
                originalUrl,
                thumbnailUrl,
                file.getOriginalFilename(),
                file.getSize(),
                contentType
            );

            log.info("File upload successful: {} (stored as: {}, original URL: {}, thumbnail URL: {})",
                file.getOriginalFilename(), storageFilename, originalUrl, thumbnailUrl);

            return result;

        } catch (FileSizeExceededException | InvalidMimeTypeException | InvalidFileExtensionException e) {
            // Re-throw validation exceptions
            throw e;
        } catch (IOException e) {
            log.error("Failed to store file {}: {}", file.getOriginalFilename(), e.getMessage(), e);
            throw new FileStorageException(
                "Failed to store file: " + e.getMessage(), e
            );
        }
    }

    /**
     * Delete a file and its thumbnail by storage filename.
     * 
     * @param storageFilename The storage filename returned from uploadFile
     * @return true if both files were deleted, false if either doesn't exist
     * @throws FileStorageException if deletion fails due to I/O errors
     */
    public boolean deleteFile(String storageFilename) {
        log.info("Deleting file: {}", storageFilename);

        try {
            Path originalPath = Paths.get(storageProperties.uploadDir, "original", storageFilename);
            Path thumbnailPath = Paths.get(storageProperties.uploadDir, "thumbnails", storageFilename);

            boolean originalDeleted = Files.deleteIfExists(originalPath);
            boolean thumbnailDeleted = Files.deleteIfExists(thumbnailPath);

            if (originalDeleted || thumbnailDeleted) {
                log.info("Deleted file: {} (original: {}, thumbnail: {})",
                    storageFilename, originalDeleted, thumbnailDeleted);
            }

            return originalDeleted || thumbnailDeleted;

        } catch (IOException e) {
            log.error("Failed to delete file {}: {}", storageFilename, e.getMessage(), e);
            throw new FileStorageException(
                "Failed to delete file: " + e.getMessage(), e
            );
        }
    }

    /**
     * Retrieve file contents for download by storage filename.
     * 
     * @param storageFilename The storage filename
     * @param isThumbnail Whether to retrieve thumbnail or original
     * @return InputStream for file contents
     * @throws FileNotFoundException if file doesn't exist
     * @throws FileStorageException if read fails
     */
    public InputStream getFileStream(String storageFilename, boolean isThumbnail) throws FileNotFoundException {
        try {
            String directory = isThumbnail ? "thumbnails" : "original";
            Path filePath = Paths.get(storageProperties.uploadDir, directory, storageFilename);

            if (!Files.exists(filePath)) {
                log.warn("File not found: {}", filePath.toAbsolutePath());
                throw new FileNotFoundException("File not found: " + storageFilename);
            }

            log.info("Retrieving {} file: {}", directory, storageFilename);
            return new FileInputStream(filePath.toFile());

        } catch (FileNotFoundException e) {
            throw e;
        } catch (IOException e) {
            log.error("Failed to read file {}: {}", storageFilename, e.getMessage(), e);
            throw new FileStorageException(
                "Failed to read file: " + e.getMessage(), e
            );
        }
    }

    /**
     * Validate MIME type against allowlist.
     */
    private boolean isValidMimeType(String mimeType) {
        if (mimeType == null || mimeType.isEmpty()) {
            return false;
        }

        for (String allowed : storageProperties.allowedMimeTypes) {
            if (mimeType.equalsIgnoreCase(allowed.trim())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Validate file extension against allowlist.
     */
    private boolean isValidExtension(String extension) {
        if (extension == null || extension.isEmpty()) {
            return false;
        }

        String lowerExtension = extension.toLowerCase();
        for (String allowed : storageProperties.allowedExtensions) {
            if (lowerExtension.equals(allowed.trim().toLowerCase())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Extract file extension from filename (without dot).
     * Example: "document.jpg" -> "jpg"
     */
    private String extractFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }

        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < filename.length() - 1) {
            return filename.substring(lastDotIndex + 1);
        }

        return "";
    }

    /**
     * Sanitize filename to prevent path traversal and remove dangerous characters.
     * 
     * Process:
     * 1. Remove path separators (/, \)
     * 2. Remove path traversal attempts (..)
     * 3. Replace invalid characters with empty string (keep alphanumeric, dots, hyphens, underscores)
     * 4. Collapse consecutive dots (prevent null byte injection)
     * 5. Limit length to MAX_FILENAME_LENGTH
     * 
     * @param filename Original filename
     * @return Sanitized filename safe for filesystem storage
     */
    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "unnamed";
        }

        // Remove path separators and traversal attempts
        String sanitized = filename.replaceAll("[/\\\\]", "")
            .replaceAll("\\.\\.", "");

        // Replace invalid characters (keep only alphanumeric, dots, hyphens, underscores)
        sanitized = VALID_FILENAME_PATTERN.matcher(sanitized).replaceAll("");

        // Collapse consecutive dots (prevent null byte injection)
        sanitized = sanitized.replaceAll("\\.{2,}", ".");

        // Remove leading/trailing dots (system reserved)
        sanitized = sanitized.replaceAll("^\\.+|\\.+$", "");

        // Limit length
        if (sanitized.length() > MAX_FILENAME_LENGTH) {
            sanitized = sanitized.substring(0, MAX_FILENAME_LENGTH);
        }

        // Fallback if all characters were invalid
        if (sanitized.isEmpty()) {
            sanitized = "unnamed";
        }

        return sanitized;
    }

    /**
     * Generate 200x200 thumbnail from uploaded file.
     * 
     * Uses Thumbnailator library to:
     * - Resize/crop to 200x200 square
     * - Maintain aspect ratio with smart cropping
     * - Preserve original quality
     * - Auto-detect format from MIME type
     * 
     * @param sourceStream Input stream of original file
     * @param targetPath Path to store thumbnail
     * @throws IOException if thumbnail generation fails
     */
    private void generateThumbnail(InputStream sourceStream, Path targetPath) throws IOException {
        try {
            int[] dimensions = storageProperties.getThumbnailDimensions();
            int size = dimensions[0]; // 200x200

            Thumbnails.of(sourceStream)
                .size(size, size)
                .keepAspectRatio(true)
                .crop(net.coobird.thumbnailator.geometry.Positions.CENTER)
                .outputFormat("jpg")
                .toFile(targetPath.toFile());

            log.info("Thumbnail generated successfully: {}x{}", size, size);

        } catch (IOException e) {
            log.error("Failed to generate thumbnail: {}", e.getMessage(), e);
            throw new FileStorageException("Failed to generate thumbnail: " + e.getMessage(), e);
        }
    }

    /**
     * Result container for successful file uploads.
     * Contains storage paths, URLs, and metadata.
     */
    public static class FileUploadResult {
        public final String storageFilename;
        public final String originalUrl;
        public final String thumbnailUrl;
        public final String originalFilename;
        public final long fileSize;
        public final String mimeType;

        public FileUploadResult(
            String storageFilename,
            String originalUrl,
            String thumbnailUrl,
            String originalFilename,
            long fileSize,
            String mimeType
        ) {
            this.storageFilename = storageFilename;
            this.originalUrl = originalUrl;
            this.thumbnailUrl = thumbnailUrl;
            this.originalFilename = originalFilename;
            this.fileSize = fileSize;
            this.mimeType = mimeType;
        }
    }

    /**
     * Exception for file size validation failures.
     */
    public static class FileSizeExceededException extends RuntimeException {
        public FileSizeExceededException(String message) {
            super(message);
        }
    }

    /**
     * Exception for MIME type validation failures.
     */
    public static class InvalidMimeTypeException extends RuntimeException {
        public InvalidMimeTypeException(String message) {
            super(message);
        }
    }

    /**
     * Exception for file extension validation failures.
     */
    public static class InvalidFileExtensionException extends RuntimeException {
        public InvalidFileExtensionException(String message) {
            super(message);
        }
    }

    /**
     * Exception for generic file storage operation failures.
     */
    public static class FileStorageException extends RuntimeException {
        public FileStorageException(String message) {
            super(message);
        }

        public FileStorageException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
