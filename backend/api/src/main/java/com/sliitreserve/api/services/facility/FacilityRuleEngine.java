package com.sliitreserve.api.services.facility;

import com.sliitreserve.api.dto.facility.ParsedFacilitySuggestionDTO;
import com.sliitreserve.api.entities.facility.Facility.FacilityType;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class FacilityRuleEngine {

    /**
     * Recommends facility attributes based on the parsed room code from a timetable.
     * 
     * Heuristics:
     * - Building: 'A' or 'B' -> "Main Building", 'F' or 'G' -> "New Building", 'E' -> "Engineering Building"
     * - Type: If code contains "LAB" or "PCLAB" -> COMPUTER_LAB, otherwise LECTURE_HALL
     * - Capacity: Defaults to 60 for LECTURE_HALL, 30 for COMPUTER_LAB
     * - Floor: The first digit found after the leading alphabetical letter(s). e.g., A405 -> Floor 4
     */
    public List<ParsedFacilitySuggestionDTO> generateSuggestions(java.util.Map<String, List<TimetableParserService.ExtractedSlot>> unmatchedRoomSlots) {
        return unmatchedRoomSlots.entrySet().stream()
                .map(e -> inferFacilityDetails(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    private ParsedFacilitySuggestionDTO inferFacilityDetails(String roomCode, List<TimetableParserService.ExtractedSlot> slots) {
        String upperCode = roomCode.toUpperCase();
        
        // 1. Infer Type
        FacilityType type = FacilityType.LECTURE_HALL;
        if (upperCode.contains("LAB") || upperCode.contains("PCLAB")) {
            type = FacilityType.LAB;
        } else if (slots != null) {
            for (TimetableParserService.ExtractedSlot slot : slots) {
                if (slot.getSubject() != null) {
                    String sub = slot.getSubject().toUpperCase();
                    if (sub.contains("PRACTICAL") || sub.contains("LAB")) {
                        type = FacilityType.LAB;
                        break;
                    }
                }
            }
        }

        // 2. Infer Capacity
        Integer capacity = type == FacilityType.LAB ? 30 : 60;

        // 3. Infer Building
        String building = "Unknown Building";
        if (upperCode.startsWith("A") || upperCode.startsWith("B")) {
            building = "Main Building";
        } else if (upperCode.startsWith("F") || upperCode.startsWith("G")) {
            building = "New Building";
        } else if (upperCode.startsWith("E")) {
            building = "Engineering Building";
        }

        // 4. Infer Floor
        String floor = "0";
        // Find the first digit in the room code
        for (char c : upperCode.toCharArray()) {
            if (Character.isDigit(c)) {
                floor = String.valueOf(c);
                break;
            }
        }

        return ParsedFacilitySuggestionDTO.builder()
                .facilityCode(roomCode)
                .name("Room " + roomCode)
                .type(type)
                .capacity(capacity)
                .building(building)
                .floor(floor)
                .build();
    }
}
