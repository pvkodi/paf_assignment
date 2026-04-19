package com.sliitreserve.api.entities.facility;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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
@Builder
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

    @Column(name = "location", length = 255)
    private String location;

    @Column(length = 100)
    private String building;

    @Column(length = 50)
    private String floor;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private FacilityStatus status = FacilityStatus.ACTIVE;

    @Column(name = "out_of_service_start")
    private LocalDateTime outOfServiceStart;

    @Column(name = "out_of_service_end")
    private LocalDateTime outOfServiceEnd;

    @NotNull(message = "Availability start time is required")
    @Column(name = "availability_start", nullable = false)
    private LocalTime availabilityStart;

    @NotNull(message = "Availability end time is required")
    @Column(name = "availability_end", nullable = false)
    private LocalTime availabilityEnd;

    /**
     * Multi-window availability schedule (Mon–Sun, multiple windows per day).
     * Replaces the flat availabilityStart/End as the primary source of truth.
     * Old flat fields are kept for backward compatibility with existing data.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "facility_availability_windows",
        joinColumns = @JoinColumn(name = "facility_id")
    )
    @Builder.Default
    private List<AvailabilityWindow> availabilityWindows = new ArrayList<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * WiFi geofencing fields for location-based check-in verification (FR-020 + geofencing enhancement)
     */
    
    @Column(name = "wifi_ssid", length = 64)
    private String wifiSSID;

    @Column(name = "wifi_mac_address", length = 17)
    private String wifiMacAddress;

    @Column(name = "facility_latitude")
    private Double latitude;

    @Column(name = "facility_longitude")
    private Double longitude;

    @Column(name = "geofence_radius_meters")
    @Builder.Default
    private Integer geofenceRadiusMeters = 100;

    @PrePersist
    protected void onCreate() {
        if (status == null) {
            status = FacilityStatus.ACTIVE;
        }
        validateAvailabilityRange();
    }

    @PreUpdate
    protected void onUpdate() {
        validateAvailabilityRange();
    }

    private void validateAvailabilityRange() {
        if (availabilityStart != null && availabilityEnd != null) {
            if (availabilityStart.isAfter(availabilityEnd) || availabilityStart.equals(availabilityEnd)) {
                throw new IllegalArgumentException(
                    "Availability start time must be before end time"
                );
            }
        }
    }

    /**
     * Returns true if the facility has any availability window covering the given day and time.
     * Falls back to the flat availabilityStart/End if no windows are configured.
     */
    public boolean isAvailableAt(DayOfWeek day, LocalTime time) {
        // First check if literally explicitly out of service via status
        if (status == FacilityStatus.OUT_OF_SERVICE) {
            return false;
        }

        // Then check if within a scheduled out-of-service range
        LocalDateTime now = LocalDateTime.now();
        // Here we assume if they check for "now", but usually this method is for checking theoretical availability.
        // It's better to have a method that takes LocalDateTime.

        if (availabilityWindows != null && !availabilityWindows.isEmpty()) {
            return availabilityWindows.stream().anyMatch(w -> w.contains(day, time));
        }
        // Legacy fallback: use flat start/end fields (treats all days as available)
        if (availabilityStart != null && availabilityEnd != null) {
            return !time.isBefore(availabilityStart) && time.isBefore(availabilityEnd);
        }
        return true;
    }

    /**
     * Comprehensive check if facility is operational at a specific date and time.
     * Incorporates status, scheduled out-of-service range, and availability windows.
     */
    public boolean isOperationalAt(LocalDateTime dateTime) {
        if (status == FacilityStatus.OUT_OF_SERVICE) {
            return false;
        }

        // Check scheduled out-of-service range
        if (outOfServiceStart != null) {
            if (!dateTime.isBefore(outOfServiceStart)) {
                if (outOfServiceEnd == null || dateTime.isBefore(outOfServiceEnd)) {
                    return false;
                }
            }
        }

        return isAvailableAt(dateTime.getDayOfWeek(), dateTime.toLocalTime());
    }

    /**
     * API alias for location field required by module contract.
     */
    public String getLocationDescription() {
        return location;
    }

    public void setLocationDescription(String locationDescription) {
        this.location = locationDescription;
    }

    /**
     * API aliases for availability field names required by module contract.
     */
    public LocalTime getAvailabilityStartTime() {
        return availabilityStart;
    }

    public void setAvailabilityStartTime(LocalTime availabilityStartTime) {
        this.availabilityStart = availabilityStartTime;
    }

    public LocalTime getAvailabilityEndTime() {
        return availabilityEnd;
    }

    public void setAvailabilityEndTime(LocalTime availabilityEndTime) {
        this.availabilityEnd = availabilityEndTime;
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
        SPORTS,
        SPORTS_FACILITY
    }

    /**
     * Enum for facility status
     */
    public enum FacilityStatus {
        ACTIVE,
        MAINTENANCE,
        OUT_OF_SERVICE
    }
}
