package com.sliitreserve.api.entities.facility;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Base Facility entity using SINGLE_TABLE inheritance strategy.
 * All facility subtypes (LectureHall, Lab, MeetingRoom, Auditorium, Equipment, SportsFacility)
 * are stored in a single 'facility' table with a discriminator column.
 */
@Entity
@Table(name = "facility")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(
    name = "facility_type",
    discriminatorType = DiscriminatorType.STRING,
    length = 50
)
@DiscriminatorValue("FACILITY")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Facility {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank(message = "Facility code is required")
    @Column(unique = true, nullable = false, length = 50)
    private String facilityCode;

    @NotBlank(message = "Facility name is required")
    @Column(nullable = false, length = 255)
    private String name;

    @NotNull(message = "Facility type is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private FacilityType type;

    @Min(value = 1, message = "Capacity must be greater than 0")
    @Column(nullable = false)
    private Integer capacity;

    @Column(length = 255)
    private String location;

    @Column(length = 100)
    private String building;

    @Column(length = 50)
    private String floor;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private FacilityStatus status = FacilityStatus.ACTIVE;

    @NotNull(message = "Availability start time is required")
    @Column(nullable = false)
    private LocalTime availabilityStart;

    @NotNull(message = "Availability end time is required")
    @Column(nullable = false)
    private LocalTime availabilityEnd;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (status == null) {
            status = FacilityStatus.ACTIVE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        // Additional validation can be added here
    }

    /**
     * Validates that availability start time is before end time.
     * Called before persistence checks.
     */
    @PostLoad
    @PostPersist
    protected void validateAvailability() {
        if (availabilityStart != null && availabilityEnd != null) {
            if (availabilityStart.isAfter(availabilityEnd) || availabilityStart.equals(availabilityEnd)) {
                throw new IllegalArgumentException(
                    "Availability start time must be before end time"
                );
            }
        }
    }

    /**
     * Enum for facility types
     */
    public enum FacilityType {
        LECTURE_HALL,
        LAB,
        MEETING_ROOM,
        AUDITORIUM,
        EQUIPMENT,
        SPORTS_FACILITY
    }

    /**
     * Enum for facility status
     */
    public enum FacilityStatus {
        ACTIVE,
        OUT_OF_SERVICE
    }
}
