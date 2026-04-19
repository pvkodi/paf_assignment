package com.sliitreserve.api.controllers.bookings;

import com.sliitreserve.api.dto.bookings.BookingRecommendationRequestDTO;
import com.sliitreserve.api.dto.bookings.RecommendationDTO;
import com.sliitreserve.api.services.booking.BookingRecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.sliitreserve.api.dto.ErrorResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
@Slf4j
public class BookingRecommendationsController {
    
    private final BookingRecommendationService bookingRecommendationService;
    
    @PostMapping("/recommendations")
    @PreAuthorize("hasAnyRole('USER', 'LECTURER', 'FACILITY_MANAGER', 'ADMIN')")
    public ResponseEntity<?> getRecommendations(@Valid @RequestBody BookingRecommendationRequestDTO criteria) {
        try {
            log.info("Getting booking recommendations for criteria: facilityType={}, capacity={}, time={}",
                    criteria.getFacilityType(), criteria.getCapacity(), criteria.getStartTime());
            
            if (criteria.getStartTime() == null || criteria.getEndTime() == null) {
                return ResponseEntity.badRequest()
                    .body(new ErrorResponseDTO("INVALID_REQUEST", "startTime and endTime are required"));
            }
            
            if (criteria.getStartTime().isAfter(criteria.getEndTime())) {
                return ResponseEntity.badRequest()
                    .body(new ErrorResponseDTO("INVALID_REQUEST", "startTime must be before endTime"));
            }
            
            if (criteria.getCapacity() == null || criteria.getCapacity() <= 0) {
                return ResponseEntity.badRequest()
                    .body(new ErrorResponseDTO("INVALID_REQUEST", "capacity must be greater than 0"));
            }
            
            List<RecommendationDTO> recommendations = bookingRecommendationService.getSmartRecommendations(criteria);
            
            return ResponseEntity.ok(new RecommendationsResponse(recommendations.size(), recommendations));
        } catch (Exception e) {
            log.error("Error getting booking recommendations: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponseDTO("INTERNAL_SERVER_ERROR", "Failed to retrieve recommendations"));
        }
    }
    
    public record RecommendationsResponse(
            Integer count,
            List<RecommendationDTO> recommendations
    ) {}
    
    // Uses shared ErrorResponseDTO from com.sliitreserve.api.dto
}
