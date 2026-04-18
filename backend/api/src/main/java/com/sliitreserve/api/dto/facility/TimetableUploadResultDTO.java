package com.sliitreserve.api.dto.facility;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;

/**
 * DTO returned when a timetable HTML file is uploaded and parsed.
 * Contains both a summary and a per-room breakdown to be displayed in the UI.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimetableUploadResultDTO {

    /** Total distinct room codes extracted from the timetable. */
    private int roomsFound;

    /** Total slot entries (room + day + hour combinations) extracted. */
    private int totalSlotsExtracted;

    /** Whether the timetable was loaded into the in-memory cache successfully. */
    private boolean cacheLoaded;

    /**
     * The scheduling allocations successfully extracted for each facility,
     * grouped by room code, then by Day of Week, holding a sorted list of times.
     */
    private Map<String, Map<DayOfWeek, List<String>>> roomSchedules;

    /**
     * Room codes found in the timetable that do NOT correspond to any known
     * Facility in the database. These are provided as pre-filled suggestions
     * to easily import them into the system.
     */
    private List<ParsedFacilitySuggestionDTO> unmatchedRooms;

    /**
     * Room codes that were matched to existing Facilities in the database.
     */
    private List<String> matchedRooms;
}
