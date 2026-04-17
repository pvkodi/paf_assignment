package com.sliitreserve.api.dto.ticket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for ticket attachments.
 *
 * <p>Includes file metadata and access URLs.
 *
 * @see com.sliitreserve.api.entities.ticket.TicketAttachment for entity structure
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketAttachmentResponseDTO {

  private UUID id;
  private String originalFilename;
  private String mimeType;
  private Long fileSize;
  private String checksumHash;
  private String type; // PROBLEM or SOLUTION

  private String filePath;
  private String thumbnailPath;

  private UUID uploadedById;
  private String uploadedByName;

  private LocalDateTime uploadedAt;
}
