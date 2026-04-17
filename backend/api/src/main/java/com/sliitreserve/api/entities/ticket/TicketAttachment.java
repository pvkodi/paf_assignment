package com.sliitreserve.api.entities.ticket;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

import com.sliitreserve.api.entities.auth.User;

/**
 * TicketAttachment entity representing a file attachment on a maintenance ticket.
 *
 * <p><b>Purpose</b>: Store file metadata and paths for ticket attachments (images, PDFs).
 * Supports image thumbnails, integrity checking via checksum, and MIME type validation
 * (FR-030, FR-031).
 *
 * <p><b>Key Fields</b> (from data-model):
 * <ul>
 *   <li><b>id</b>: UUID primary key
 *   <li><b>ticket</b>: Many-to-one reference to MaintenanceTicket
 *   <li><b>uploadedBy</b>: Many-to-one reference to User (uploader; required)
 *   <li><b>fileName</b>: Original filename (max 255 chars; not blank)
 *   <li><b>fileSize</b>: File size in bytes (> 0, <= 5MB = 5242880 bytes)
 *   <li><b>mimeType</b>: MIME type (max 50 chars; required; must be in allowed list)
 *   <li><b>filePath</b>: Path to stored file in local filesystem
 *   <li><b>thumbnailPath</b>: Path to thumbnail (nullable; for images only)
 *   <li><b>checksumHash</b>: SHA-256 hash for integrity verification
 *   <li><b>uploadedAt</b>: Creation timestamp (immutable)
 * </ul>
 *
 * <p><b>Constraints</b> (FR-030, FR-031):
 * <ul>
 *   <li>Allowed MIME types: image/jpeg, image/png, image/gif, image/webp, application/pdf
 *   <li>Max 3 attachments per ticket (enforced in service layer)
 *   <li>Max file size: 5MB (5242880 bytes)
 *   <li>Checksum: SHA-256 hash for integrity validation
 * </ul>
 *
 * <p><b>Relationships</b>:
 * <ul>
 *   <li>Many-to-one MaintenanceTicket (inverse of Ticket.attachments)
 *   <li>Many-to-one User (inverse of User.uploadedAttachments, if bidirectional)
 * </ul>
 *
 * @see MaintenanceTicket
 * @see User
 */
@Entity
@Table(
    name = "ticket_attachment",
    indexes = {
      @Index(name = "idx_attachment_ticket", columnList = "ticket_id"),
      @Index(name = "idx_attachment_uploaded_by", columnList = "uploaded_by_user_id"),
      @Index(name = "idx_attachment_uploaded_at", columnList = "uploaded_at")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketAttachment {

  public static final long MAX_FILE_SIZE = 5242880L; // 5MB in bytes
  public static final String[] ALLOWED_MIME_TYPES = {
    "image/jpeg",
    "image/png",
    "image/gif",
    "image/webp",
    "application/pdf"
  };

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @NotNull(message = "Ticket is required")
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ticket_id", nullable = false)
  private MaintenanceTicket ticket;

  @NotNull(message = "Uploaded by user is required")
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "uploaded_by_user_id", nullable = false)
  private User uploadedBy;

  @NotBlank(message = "File name is required")
  @Column(name = "file_name", nullable = false, length = 255)
  private String fileName;

  @NotNull(message = "File size is required")
  @Positive(message = "File size must be greater than 0")
  @Column(name = "file_size", nullable = false)
  private Long fileSize;

  @NotBlank(message = "MIME type is required")
  @Column(name = "mime_type", nullable = false, length = 50)
  private String mimeType;

  @NotBlank(message = "File path is required")
  @Column(name = "file_path", nullable = false, columnDefinition = "TEXT")
  private String filePath;

  @Column(name = "thumbnail_path", columnDefinition = "TEXT")
  private String thumbnailPath;

  @NotBlank(message = "Checksum hash is required")
  @Column(name = "checksum_hash", nullable = false, length = 64)
  private String checksumHash;

  @Column(name = "type", nullable = false, length = 50)
  @Builder.Default
  private String type = "PROBLEM"; // PROBLEM or SOLUTION

  @CreationTimestamp
  @Column(name = "uploaded_at", nullable = false, updatable = false)
  private LocalDateTime uploadedAt;

  /**
   * Check if the file size exceeds the maximum allowed size (5MB).
   *
   * @return true if fileSize > MAX_FILE_SIZE
   */
  public boolean isFileSizeExceeded() {
    return fileSize > MAX_FILE_SIZE;
  }

  /**
   * Check if the MIME type is allowed.
   *
   * @return true if mimeType is in ALLOWED_MIME_TYPES
   */
  public boolean isMimeTypeAllowed() {
    for (String allowed : ALLOWED_MIME_TYPES) {
      if (allowed.equals(mimeType)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Check if the attachment is an image.
   *
   * @return true if mimeType starts with "image/"
   */
  public boolean isImage() {
    return mimeType != null && mimeType.startsWith("image/");
  }
}
