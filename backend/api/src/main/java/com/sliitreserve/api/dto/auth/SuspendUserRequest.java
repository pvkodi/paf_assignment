package com.sliitreserve.api.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SuspendUserRequest {
    /** If provided, suspend for this many days starting now */
    private Integer suspendDays;

    /** If provided, set suspendedUntil to this exact timestamp */
    private LocalDateTime suspendUntil;

    /** If true, lift suspension (release user) */
    private Boolean lift;
}
