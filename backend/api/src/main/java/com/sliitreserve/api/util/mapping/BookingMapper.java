package com.sliitreserve.api.util.mapping;

import com.sliitreserve.api.dto.bookings.*;
import com.sliitreserve.api.entities.booking.Booking;
import com.sliitreserve.api.services.booking.BookingService;
import com.sliitreserve.api.services.booking.CheckInService;
import lombok.RequiredArgsConstructor;
import com.sliitreserve.api.repositories.bookings.ApprovalStepRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for converting between Booking entities and DTOs.
 */
@Component
@RequiredArgsConstructor
public class BookingMapper implements BaseMapper<Booking, BookingRequestDTO, BookingResponseDTO> {

    private final CheckInService checkInService;
    private final ApprovalStepRepository approvalStepRepository;

    @Override
    public BookingResponseDTO toResponseDTO(Booking booking) {
        if (booking == null) {
            return null;
        }

        BookingResponseDTO dto = new BookingResponseDTO();
        dto.setId(booking.getId());
        dto.setFacilityId(booking.getFacility() != null ? booking.getFacility().getId() : null);
        dto.setRequestedByUserId(booking.getRequestedBy() != null ? booking.getRequestedBy().getId() : null);
        dto.setBookedForUserId(booking.getBookedFor() != null ? booking.getBookedFor().getId() : null);
        dto.setBookingDate(booking.getBookingDate());
        dto.setStartTime(booking.getStartTime());
        dto.setEndTime(booking.getEndTime());
        dto.setPurpose(booking.getPurpose());
        dto.setAttendees(booking.getAttendees());
        dto.setStatus(booking.getStatus() != null ? booking.getStatus().name() : null);
        dto.setRecurrenceRule(booking.getRecurrenceRule());
        dto.setIsRecurringMaster(booking.isRecurringMaster());
        dto.setTimezone(booking.getTimezone());
        dto.setVersion(booking.getVersion());
        
        // Populate check-in status
        try {
            dto.setHasCheckedIn(checkInService.isCheckedIn(booking.getId()));
        } catch (Exception e) {
            // If check-in status cannot be determined, default to false
            dto.setHasCheckedIn(false);
        }
        
        return dto;
    }

    /**
     * Map Booking entity to BookingDetailedResponseDTO with all nested information.
     * This DTO includes facility details, user information, and approval workflow.
     * Handles lazy-loaded relationships and null values gracefully.
     */
    public BookingDetailedResponseDTO toDetailedResponseDTO(Booking booking) {
        if (booking == null) {
            return null;
        }

        BookingDetailedResponseDTO dto = new BookingDetailedResponseDTO();
        dto.setId(booking.getId());
        
        try {
            // Map facility details - handle lazy loading
            if (booking.getFacility() != null) {
                try {
                    dto.setFacility(mapFacilitySummary(booking.getFacility()));
                } catch (Exception e) {
                    // Facility might be lazy-loaded, log and continue
                    System.err.println("Warning: Failed to map facility for booking " + booking.getId() + ": " + e.getMessage());
                }
            }
            
            // Map requested by user - handle lazy loading
            if (booking.getRequestedBy() != null) {
                try {
                    dto.setRequestedBy(mapUserSummary(booking.getRequestedBy()));
                } catch (Exception e) {
                    System.err.println("Warning: Failed to map requestedBy user for booking " + booking.getId() + ": " + e.getMessage());
                }
            }
            
            // Map booked for user - handle lazy loading
            if (booking.getBookedFor() != null) {
                try {
                    dto.setBookedFor(mapUserSummary(booking.getBookedFor()));
                } catch (Exception e) {
                    System.err.println("Warning: Failed to map bookedFor user for booking " + booking.getId() + ": " + e.getMessage());
                }
            }
            
            dto.setBookingDate(booking.getBookingDate());
            dto.setStartTime(booking.getStartTime());
            dto.setEndTime(booking.getEndTime());
            dto.setPurpose(booking.getPurpose());
            dto.setAttendees(booking.getAttendees());
            dto.setStatus(booking.getStatus() != null ? booking.getStatus().name() : null);
            dto.setRecurrenceRule(booking.getRecurrenceRule());
            dto.setIsRecurringMaster(booking.isRecurringMaster());
            dto.setTimezone(booking.getTimezone());
            dto.setVersion(booking.getVersion());
            
            // Map approval steps - handle potential issues
            try {
                List<ApprovalStepDTO> approvalSteps = approvalStepRepository.findByBookingOrderByStepOrderAsc(booking)
                        .stream()
                        .map(this::mapApprovalStep)
                        .collect(Collectors.toList());
                dto.setApprovalSteps(approvalSteps);
            } catch (Exception e) {
                System.err.println("Warning: Failed to map approval steps for booking " + booking.getId() + ": " + e.getMessage());
                dto.setApprovalSteps(new ArrayList<>());
            }
            
            dto.setCreatedAt(booking.getCreatedAt());
            dto.setUpdatedAt(booking.getUpdatedAt());
            
        } catch (Exception e) {
            System.err.println("Error mapping booking " + booking.getId() + ": " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to map booking details: " + e.getMessage(), e);
        }
        
        return dto;
    }

    /**
     * Map User entity to UserSummaryDTO
     */
    private UserSummaryDTO mapUserSummary(com.sliitreserve.api.entities.auth.User user) {
        if (user == null) {
            return null;
        }
        return new UserSummaryDTO(
                user.getId(),
                user.getEmail(),
                user.getDisplayName()
        );
    }

    /**
     * Map Facility entity to FacilitySummaryDTO
     */
    private FacilitySummaryDTO mapFacilitySummary(com.sliitreserve.api.entities.facility.Facility facility) {
        if (facility == null) {
            return null;
        }
        return new FacilitySummaryDTO(
                facility.getId(),
                facility.getName(),
                facility.getType() != null ? facility.getType().name() : null,
                facility.getCapacity(),
                facility.getBuilding(),
                facility.getFloor(),
                facility.getLocation(),
                facility.getStatus() != null ? facility.getStatus().name() : null
        );
    }

    /**
     * Map ApprovalStep entity to ApprovalStepDTO
     */
    private ApprovalStepDTO mapApprovalStep(com.sliitreserve.api.entities.booking.ApprovalStep step) {
        if (step == null) {
            return null;
        }
        return new ApprovalStepDTO(
                step.getStepOrder(),
                step.getApproverRole() != null ? step.getApproverRole().name() : null,
                step.getDecision() != null ? step.getDecision().name() : null,
                step.getNote(),
                step.getDecidedBy() != null ? mapUserSummary(step.getDecidedBy()) : null,
                step.getDecidedAt()
        );
    }

    @Override
    public Booking toEntity(BookingRequestDTO dto) {
        // Typically creation is handled by the specialized BookingBuilder in BookingService. 
        // Returning a new generic instance just to satisfy the BaseMapper if required.
        return new Booking();
    }

    @Override
    public Booking updateEntity(BookingRequestDTO dto, Booking entity) {
        // Will be used for UPDATE operations if we support partial updates
        return entity;
    }
}
