package com.sliitreserve.api.services.ticket;

import com.sliitreserve.api.entities.auth.User;
import com.sliitreserve.api.entities.ticket.MaintenanceTicket;
import com.sliitreserve.api.entities.ticket.TicketAttachment;
import com.sliitreserve.api.repositories.ticket.TicketAttachmentRepository;
import com.sliitreserve.api.services.storage.LocalFileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing ticket attachments with validation, storage, and thumbnail generation.
 *
 * <p><b>Purpose</b>: Enforce attachment policies (MIME type, file size, count), securely
 * store files, generate thumbnails, and maintain attachment metadata (FR-030, FR-031).
 *
 * <p><b>Features</b>:
 * <ul>
 *   <li>MIME type validation against allowlist (image/jpeg, image/png, image/gif, image/webp, application/pdf)
 *   <li>File size validation (max 5MB per file)
 *   <li>Attachment count limit enforcement (max 3 per ticket)
 *   <li>SHA-256 checksum computation for integrity verification
 *   <li>Automatic thumbnail generation for images (delegated to LocalFileStorageService)
 *   <li>Secure file storage with UUID-based naming
 *   <li>Persistence of attachment metadata in database
 * </ul>
 *
 * <p><b>Constraints</b> (from FR-030, FR-031):
 * <ul>
 *   <li>Allowed MIME types: image/jpeg, image/png, image/gif, image/webp, application/pdf
 *   <li>Max 3 attachments per ticket
 *   <li>Max file size per attachment: 5MB (5242880 bytes)
 *   <li>All attachments must have valid checksums before persistence
 * </ul>
 *
 * <p><b>Upload Workflow</b>:
 * <ol>
 *   <li>Validate ticket exists and is in appropriate state
 *   <li>Validate MIME type is in allowlist
 *   <li>Validate file size ≤ 5MB
 *   <li>Check attachment count limit (max 3 per ticket)
 *   <li>Compute SHA-256 checksum
 *   <li>Store file via LocalFileStorageService (includes sanitization and thumbnail generation)
 *   <li>Create and persist TicketAttachment entity with metadata
 * </ol>
 *
 * @see MaintenanceTicket
 * @see TicketAttachment
 * @see LocalFileStorageService
 * @author Maintenance Ticketing Module
 */
@Slf4j
@Service
@Transactional
public class TicketAttachmentService {

  private final TicketAttachmentRepository attachmentRepository;
  private final LocalFileStorageService fileStorageService;

  @Autowired
  public TicketAttachmentService(
      TicketAttachmentRepository attachmentRepository,
      LocalFileStorageService fileStorageService) {
    this.attachmentRepository = attachmentRepository;
    this.fileStorageService = fileStorageService;
  }

  /**
   * Upload and attach a file to a ticket with full validation and storage.
   *
   * <p>This method orchestrates the complete attachment pipeline:
   * <ol>
   *   <li>Validates MIME type
   *   <li>Validates file size
   *   <li>Checks attachment count limit
   *   <li>Computes checksum
   *   <li>Stores file (with thumbnail generation)
   *   <li>Persists attachment metadata
   * </ol>
   *
   * @param ticket the ticket to attach file to (must be non-null and in a valid state)
   * @param file the MultipartFile to upload (must be non-null)
   * @param uploadedBy the user uploading the file (must be non-null)
   * @return the persisted TicketAttachment entity with all metadata populated
   * @throws IllegalArgumentException if any required parameter is null or ticket/user invalid
   * @throws IllegalStateException if attachment count limit exceeded or ticket in invalid state
   * @throws IllegalArgumentException if MIME type not allowed
   * @throws IllegalArgumentException if file size exceeds maximum (5MB)
   * @throws RuntimeException if checksum computation fails or file storage fails
   */
  public TicketAttachment attachFileToTicket(
      MaintenanceTicket ticket, MultipartFile file, User uploadedBy) {
    // Validation
    if (ticket == null) {
      throw new IllegalArgumentException("Ticket cannot be null");
    }
    if (file == null || file.isEmpty()) {
      throw new IllegalArgumentException("File cannot be null or empty");
    }
    if (uploadedBy == null) {
      throw new IllegalArgumentException("Uploaded by user cannot be null");
    }

    String contentType = file.getContentType();
    long fileSize = file.getSize();
    String originalFileName = file.getOriginalFilename();

    log.debug(
        "Attaching file to ticket {}: fileName={}, fileSize={}, mimeType={}",
        ticket.getId(),
        originalFileName,
        fileSize,
        contentType);

    // Step 1: Validate MIME type
    validateMimeType(contentType);

    // Step 2: Validate file size
    validateFileSize(fileSize);

    // Step 3: Check attachment count limit
    validateAttachmentCount(ticket);

    // Step 4: Compute SHA-256 checksum
    String checksum;
    try {
      checksum = computeChecksum(file);
    } catch (IOException e) {
      log.error("Error computing checksum for file {}: {}", originalFileName, e.getMessage(), e);
      throw new RuntimeException("Failed to compute file checksum", e);
    }

    // Step 5: Store file via LocalFileStorageService
    // This automatically generates thumbnails for image files
    String filePath;
    String thumbnailPath = null;
    try {
      LocalFileStorageService.FileUploadResult uploadResult = fileStorageService.uploadFile(file);
      filePath = uploadResult.storageFilename;
      thumbnailPath = uploadResult.thumbnailUrl; // Automatically generated for images
      log.debug("Stored file {} to {}. Thumbnail: {}", originalFileName, uploadResult.originalUrl, thumbnailPath);
    } catch (Exception e) {
      log.error("Failed to store file {}: {}", originalFileName, e.getMessage(), e);
      throw new RuntimeException("Failed to store file on disk", e);
    }

    // Step 6: Create and persist TicketAttachment entity
    TicketAttachment attachment =
        TicketAttachment.builder()
            .ticket(ticket)
            .uploadedBy(uploadedBy)
            .fileName(originalFileName != null ? originalFileName : "attachment")
            .fileSize(fileSize)
            .mimeType(contentType)
            .filePath(filePath)
            .thumbnailPath(thumbnailPath)
            .checksumHash(checksum)
            .build();

    TicketAttachment savedAttachment = attachmentRepository.save(attachment);
    log.info("Attachment saved for ticket {}: attachmentId={}", ticket.getId(), savedAttachment.getId());

    return savedAttachment;
  }

  /**
   * Retrieve an attachment by ID with full metadata.
   *
   * @param attachmentId the UUID of the attachment to retrieve
   * @return Optional containing the TicketAttachment if found, or empty if not found
   * @throws IllegalArgumentException if attachmentId is null
   */
  @Transactional(readOnly = true)
  public Optional<TicketAttachment> getAttachmentById(UUID attachmentId) {
    if (attachmentId == null) {
      throw new IllegalArgumentException("Attachment ID cannot be null");
    }
    return attachmentRepository.findById(attachmentId);
  }

  /**
   * Retrieve all attachments for a ticket, ordered by upload time.
   *
   * @param ticket the ticket to get attachments for (must be non-null)
   * @return list of TicketAttachments for the ticket (empty if none)
   * @throws IllegalArgumentException if ticket is null
   */
  @Transactional(readOnly = true)
  public List<TicketAttachment> getAttachmentsForTicket(MaintenanceTicket ticket) {
    if (ticket == null) {
      throw new IllegalArgumentException("Ticket cannot be null");
    }
    return attachmentRepository.findByTicketOrderByUploadedAtDesc(ticket);
  }

  /**
   * Delete an attachment and clean up associated files.
   *
   * <p>This method removes the attachment record and attempts to delete the associated files
   * from disk. Partial failures are logged but do not prevent deletion.
   *
   * @param attachment the attachment to delete (must be non-null and exist in database)
   * @throws IllegalArgumentException if attachment is null
   * @throws RuntimeException if database deletion fails
   */
  public void deleteAttachment(TicketAttachment attachment) {
    if (attachment == null) {
      throw new IllegalArgumentException("Attachment cannot be null");
    }

    UUID attachmentId = attachment.getId();
    String filePath = attachment.getFilePath();
    String thumbnailPath = attachment.getThumbnailPath();

    try {
      // Delete from database
      attachmentRepository.delete(attachment);
      log.info("Deleted attachment record: {}", attachmentId);

      // Attempt to delete files from disk (non-critical)
      if (filePath != null && !filePath.isBlank()) {
        try {
          deleteFile(filePath);
          log.debug("Deleted file: {}", filePath);
        } catch (Exception e) {
          log.warn("Failed to delete file {}: {}", filePath, e.getMessage());
        }
      }

      if (thumbnailPath != null && !thumbnailPath.isBlank()) {
        try {
          deleteFile(thumbnailPath);
          log.debug("Deleted thumbnail: {}", thumbnailPath);
        } catch (Exception e) {
          log.warn("Failed to delete thumbnail {}: {}", thumbnailPath, e.getMessage());
        }
      }
    } catch (Exception e) {
      log.error("Failed to delete attachment {}: {}", attachmentId, e.getMessage(), e);
      throw new RuntimeException("Failed to delete attachment", e);
    }
  }

  /**
   * Validate that the MIME type is in the allowed list.
   *
   * @param mimeType the MIME type to validate
   * @throws IllegalArgumentException if mimeType is null, blank, or not in allowed list
   */
  private void validateMimeType(String mimeType) {
    if (mimeType == null || mimeType.isBlank()) {
      throw new IllegalArgumentException("MIME type is required");
    }

    List<String> allowed = Arrays.asList(TicketAttachment.ALLOWED_MIME_TYPES);
    if (!allowed.contains(mimeType)) {
      throw new IllegalArgumentException(
          "MIME type not allowed: " + mimeType + ". Allowed types: " + String.join(", ", allowed));
    }
  }

  /**
   * Validate that the file size does not exceed the maximum limit.
   *
   * @param fileSize the file size in bytes
   * @throws IllegalArgumentException if fileSize is 0 or exceeds maximum (5MB)
   */
  private void validateFileSize(long fileSize) {
    if (fileSize <= 0) {
      throw new IllegalArgumentException("File size must be greater than 0");
    }
    if (fileSize > TicketAttachment.MAX_FILE_SIZE) {
      throw new IllegalArgumentException(
          "File size exceeds maximum limit. Max: "
              + TicketAttachment.MAX_FILE_SIZE
              + " bytes, Provided: "
              + fileSize
              + " bytes");
    }
  }

  /**
   * Validate that attachment count for the ticket does not exceed the limit (max 3).
   *
   * @param ticket the ticket to check (must be non-null)
   * @throws IllegalStateException if attachment count would exceed 3
   */
  private void validateAttachmentCount(MaintenanceTicket ticket) {
    long currentCount = attachmentRepository.countByTicket(ticket);
    if (currentCount >= 3) {
      throw new IllegalStateException(
          "Ticket has reached maximum attachment limit (3). Current count: " + currentCount);
    }
  }

  /**
   * Compute SHA-256 checksum of the file for integrity verification.
   *
   * @param file the MultipartFile to compute checksum for (must be non-null)
   * @return hexadecimal SHA-256 hash string
   * @throws IOException if reading the file fails
   * @throws RuntimeException if SHA-256 algorithm not available
   */
  private String computeChecksum(MultipartFile file) throws IOException {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] buffer = new byte[8192];
      int bytesRead;

      try (InputStream is = file.getInputStream()) {
        while ((bytesRead = is.read(buffer)) != -1) {
          digest.update(buffer, 0, bytesRead);
        }
      }

      byte[] hashBytes = digest.digest();
      StringBuilder hexString = new StringBuilder();
      for (byte b : hashBytes) {
        String hex = Integer.toHexString(0xff & b);
        if (hex.length() == 1) {
          hexString.append('0');
        }
        hexString.append(hex);
      }

      return hexString.toString();
    } catch (NoSuchAlgorithmException e) {
      log.error("SHA-256 algorithm not available: {}", e.getMessage(), e);
      throw new RuntimeException("SHA-256 algorithm not available", e);
    }
  }

  /**
   * Check if a MIME type corresponds to an image.
   *
   * @param mimeType the MIME type to check (can be null)
   * @return true if the MIME type is an image type, false otherwise
   */
  private boolean isImageMimeType(String mimeType) {
    if (mimeType == null) {
      return false;
    }
    return mimeType.startsWith("image/");
  }

  /**
   * Delete a file from the filesystem.
   *
   * @param filePath the path to the file to delete
   * @throws IOException if file deletion fails
   */
  private void deleteFile(String filePath) throws IOException {
    if (filePath == null || filePath.isBlank()) {
      return;
    }
    Path path = Paths.get(filePath);
    Files.deleteIfExists(path);
  }
}
