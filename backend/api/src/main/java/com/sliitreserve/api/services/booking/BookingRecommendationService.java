package com.sliitreserve.api.services.booking;

import com.sliitreserve.api.dto.bookings.BookingRecommendationRequestDTO;
import com.sliitreserve.api.dto.bookings.RecommendationDTO;
import com.sliitreserve.api.entities.facility.Facility;
import com.sliitreserve.api.repositories.bookings.BookingRepository;
import com.sliitreserve.api.repositories.facility.FacilityRepository;
import com.sliitreserve.api.repositories.facility.UtilizationSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for generating smart booking recommendations
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class BookingRecommendationService {
    
    private final FacilityRepository facilityRepository;
    private final BookingRepository bookingRepository;
    private final UtilizationSnapshotRepository utilizationSnapshotRepository;
    
    /**
     * Get smart facility recommendations based on booking criteria
     */
    public List<RecommendationDTO> getSmartRecommendations(BookingRecommendationRequestDTO criteria) {
        log.debug("Getting recommendations for capacity: {}, building: {}", 
            criteria.getCapacity(), criteria.getPreferredBuilding());
        
        LocalDateTime now = LocalDateTime.now();
        
        // Get all active facilities
        List<Facility> facilities = facilityRepository.findByStatus(Facility.FacilityStatus.ACTIVE);
        
        List<RecommendationDTO> recommendations = new ArrayList<>();
        int rank = 1;
        
        // Score and sort facilities
        var scoreResults = facilities.stream()
            .map(facility -> scoreAndFacilityRecommendation(facility, criteria, now))
            .filter(rec -> rec.getRecommendationScore() > 30)  // Only show reasonable matches
            .sorted((a, b) -> Integer.compare(b.getRecommendationScore(), a.getRecommendationScore()))
            .limit(criteria.getMaxRecommendations() != null ? criteria.getMaxRecommendations() : 5)
            .toList();
        
        for (RecommendationDTO rec : scoreResults) {
            rec.setRank(rank++);
            recommendations.add(rec);
        }
        
        return recommendations;
    }
    
    /**
     * Score a facility against booking criteria
     */
    private RecommendationDTO scoreAndFacilityRecommendation(
            Facility facility,
            BookingRecommendationRequestDTO criteria,
            LocalDateTime now) {
        
        int score = 0;
        List<String> reasons = new ArrayList<>();
        
        LocalDateTime requestStartTime = criteria.getStartTime();
        String facilityId = facility.getId().toString();
        
        // Check capacity match
        int requiredCapacity = criteria.getCapacity() != null ? criteria.getCapacity() : 10;
        if (facility.getCapacity().equals(requiredCapacity)) {
            score += 30;
            reasons.add("Perfect size match");
        } else if (facility.getCapacity() > requiredCapacity) {
            score += 20;
            reasons.add("Sufficient capacity");
        } else {
            score += 5;
            reasons.add("Limited capacity");
        }
        
        // Check building preference
        if (criteria.getPreferredBuilding() != null &&
            facility.getBuilding() != null &&
            facility.getBuilding().equals(criteria.getPreferredBuilding())) {
            score += 20;
            reasons.add("Your preferred building");
        }
        
        // Check availability at requested time
        long currentBookings = bookingRepository.countActiveBookings(facilityId, requestStartTime);
        
        String availabilityStatus;
        int utilizationAtTime;
        
        if (currentBookings >= facility.getCapacity()) {
            availabilityStatus = "FULL";
            utilizationAtTime = 100;
            score += 0;
        } else if (currentBookings > 0) {
            availabilityStatus = "BUSY";
            utilizationAtTime = (int) ((currentBookings * 100) / facility.getCapacity());
            score += 15;
            reasons.add("Partially available");
        } else {
            availabilityStatus = "FREE";
            utilizationAtTime = 0;
            score += 25;
            reasons.add("Available at requested time");
        }
        
        // Check historical utilization
        Integer avg7day = utilizationSnapshotRepository.getAverageUtilization(facilityId, 7);
        
        if (avg7day != null && avg7day < 40) {
            score += 10;
            reasons.add("Historically available");
        }
        
        return RecommendationDTO.builder()
            .facilityId(facilityId)
            .facilityName(facility.getName())
            .facilityType(facility.getType() != null ? facility.getType().name() : "UNKNOWN")
            .building(facility.getBuilding())
            .capacity(facility.getCapacity())
            .recommendationScore(Math.min(score, 100))
            .availabilityAtTime(availabilityStatus)
            .utilizationPercentAtTime(utilizationAtTime)
            .reasons(reasons)
            .alternativeSlots(new ArrayList<>())
            .build();
    }
    
    /**
     * Find alternative time slots when facility is not available at requested time
     */
    private void findAlternativeSlots(
            String facilityId,
            BookingRecommendationRequestDTO criteria,
            List<String> slots) {
        
        // Find next 3 available time slots
        LocalDateTime currentTime = criteria.getStartTime();
        int slotsFound = 0;
        int maxSlots = 3;
        
        while (slotsFound < maxSlots && currentTime.isBefore(currentTime.plusDays(7))) {
            long bookings = bookingRepository.countActiveBookings(facilityId, currentTime);
            
            if (bookings == 0) {
                slots.add(currentTime.toString());
                slotsFound++;
            }
            
            currentTime = currentTime.plusHours(1);
        }
    }
}
