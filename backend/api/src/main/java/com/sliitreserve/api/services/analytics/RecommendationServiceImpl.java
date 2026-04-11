package com.sliitreserve.api.services.analytics;

import com.sliitreserve.api.dto.analytics.UtilizationResponse;
import com.sliitreserve.api.entities.analytics.UtilizationSnapshot;
import com.sliitreserve.api.entities.facility.Facility;
import com.sliitreserve.api.entities.facility.Facility.FacilityStatus;
import com.sliitreserve.api.repositories.FacilityRepository;
import com.sliitreserve.api.repositories.UtilizationSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Recommendation service implementation.
 *
 * Strategy:
 * - For each underutilized facility, search active facilities of the same type
 * - Pick best candidate with better latest utilization and closest capacity fit
 * - Return one recommendation per underutilized facility when possible
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {

    private static final BigDecimal MIN_UTILIZATION_GAP_PERCENT = new BigDecimal("5.00");

    private final FacilityRepository facilityRepository;
    private final UtilizationSnapshotRepository snapshotRepository;

    @Override
    @Transactional(readOnly = true)
    public List<UtilizationResponse.RecommendedAlternative> generateRecommendations(
        List<UtilizationResponse.UnderutilizedFacility> underutilized
    ) {
        if (underutilized == null || underutilized.isEmpty()) {
            return List.of();
        }

        List<UtilizationResponse.RecommendedAlternative> recommendations = new ArrayList<>();

        for (UtilizationResponse.UnderutilizedFacility underutilizedFacility : underutilized) {
            Optional<Facility> requestedFacilityOpt = facilityRepository.findById(underutilizedFacility.getFacilityId());
            if (requestedFacilityOpt.isEmpty()) {
                continue;
            }

            Facility requestedFacility = requestedFacilityOpt.get();
            CandidateRecommendation bestCandidate = findBestCandidate(requestedFacility, underutilizedFacility);
            if (bestCandidate == null) {
                continue;
            }

            recommendations.add(
                UtilizationResponse.RecommendedAlternative.builder()
                    .requestedFacilityId(requestedFacility.getId())
                    .alternativeFacilityId(bestCandidate.facility().getId())
                    .alternativeFacilityName(bestCandidate.facility().getName())
                    .capacity(bestCandidate.facility().getCapacity())
                    .utilizationPercent(bestCandidate.utilizationPercent())
                    .reason(bestCandidate.reason())
                    .build()
            );
        }

        return recommendations;
    }

    private CandidateRecommendation findBestCandidate(
        Facility requestedFacility,
        UtilizationResponse.UnderutilizedFacility underutilizedFacility
    ) {
        List<Facility> sameTypeCandidates = facilityRepository.findByTypeAndStatus(
            requestedFacility.getType(),
            FacilityStatus.ACTIVE
        );

        BigDecimal requestedUtilization = zeroIfNull(underutilizedFacility.getUtilizationPercent());
        int requestedCapacity = requestedFacility.getCapacity() == null ? 0 : requestedFacility.getCapacity();

        return sameTypeCandidates.stream()
            .filter(candidate -> !candidate.getId().equals(requestedFacility.getId()))
            .map(candidate -> evaluateCandidate(candidate, requestedUtilization, requestedCapacity))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .sorted(
                Comparator
                    .comparing(CandidateRecommendation::utilizationPercent, Comparator.reverseOrder())
                    .thenComparingInt(CandidateRecommendation::capacityDelta)
            )
            .findFirst()
            .orElse(null);
    }

    private Optional<CandidateRecommendation> evaluateCandidate(
        Facility candidate,
        BigDecimal requestedUtilization,
        int requestedCapacity
    ) {
        Optional<UtilizationSnapshot> latestSnapshotOpt = snapshotRepository
            .findFirstByFacility_IdOrderBySnapshotDateDesc(candidate.getId());

        if (latestSnapshotOpt.isEmpty()) {
            return Optional.empty();
        }

        UtilizationSnapshot latestSnapshot = latestSnapshotOpt.get();
        if (latestSnapshot.isUnderutilized()) {
            return Optional.empty();
        }

        BigDecimal candidateUtilization = zeroIfNull(latestSnapshot.getUtilizationPercent());
        BigDecimal minimumCandidateUtilization = requestedUtilization.add(MIN_UTILIZATION_GAP_PERCENT);

        if (candidateUtilization.compareTo(minimumCandidateUtilization) < 0) {
            return Optional.empty();
        }

        int candidateCapacity = candidate.getCapacity() == null ? 0 : candidate.getCapacity();
        int capacityDelta = Math.abs(candidateCapacity - requestedCapacity);

        BigDecimal utilizationDiff = candidateUtilization.subtract(requestedUtilization).setScale(2, RoundingMode.HALF_UP);
        String reason = "Same type facility with " + utilizationDiff + "% higher utilization and capacity delta " + capacityDelta;

        return Optional.of(new CandidateRecommendation(candidate, candidateUtilization, capacityDelta, reason));
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private record CandidateRecommendation(
        Facility facility,
        BigDecimal utilizationPercent,
        int capacityDelta,
        String reason
    ) {
    }
}
