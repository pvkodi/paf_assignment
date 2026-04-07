package com.sliitreserve.api.entities.facility;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Equipment subtype of Facility.
 * Inherits all base Facility properties and adds equipment-specific metadata.
 */
@Entity
@DiscriminatorValue("EQUIPMENT")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Equipment extends Facility {

    @NotBlank(message = "Equipment brand is required")
    @Column(name = "brand", nullable = false, length = 100)
    private String brand;

    @Column(name = "model", length = 100)
    private String model;

    @Column(name = "serial_number", unique = true, length = 100)
    private String serialNumber;

    @Column(name = "maintenance_schedule", length = 255)
    private String maintenanceSchedule;

    public Equipment(String facilityCode, String name, Integer capacity, String location, String brand) {
        super();
        this.setFacilityCode(facilityCode);
        this.setName(name);
        this.setType(FacilityType.EQUIPMENT);
        this.setCapacity(capacity);
        this.setLocation(location);
        this.brand = brand;
    }
}
