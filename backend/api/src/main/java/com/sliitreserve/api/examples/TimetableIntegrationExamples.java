package com.sliitreserve.api.examples;

import com.sliitreserve.api.services.facility.FacilityService;
import com.sliitreserve.api.services.facility.FacilityTimetableService;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;

/**
 * Integration Examples: How to use the Timetable Parsing & Availability Engine
 * 
 * This example demonstrates:
 * 1. Loading timetable from HTML file
 * 2. Querying facility occupancy
 * 3. Checking availability for booking
 * 4. Using with FacilityService for operational checks
 * 
 * ---
 * USAGE PATTERNS:
 * ---
 */
public class TimetableIntegrationExamples {

    /**
     * EXAMPLE 1: Load a university timetable from HTML file
     * 
     * Typical use case:
     * - Admin uploads FET-generated HTML timetable
     * - System parses and caches the data
     * - Data is now used in all availability queries
     */
    public void example1_LoadTimetable(
            FacilityTimetableService timetableService
    ) throws Exception {
        // Load from file
        File timetableFile = new File("/path/to/timetable.html");
        timetableService.loadTimetable(timetableFile, false); // false = don't reload if already loaded

        // Or load from HTML string (e.g., uploaded via API)
        String htmlContent = Files.readString(Paths.get("/path/to/timetable.html"));
        timetableService.loadTimetableFromHtml(htmlContent, true); // true = force reload
    }

    /**
     * EXAMPLE 2: Check if a facility is occupied at a specific time
     * 
     * Typical use case:
     * - User wants to book facility G1301 on Monday at 08:00
     * - System checks: is it occupied?
     * - If occupied in timetable, booking should be rejected
     */
    public void example2_CheckOccupancy(
            FacilityTimetableService timetableService
    ) {
        String facilityCode = "G1301";
        DayOfWeek dayOfWeek = DayOfWeek.MONDAY;
        LocalTime timeSlot = LocalTime.of(8, 0);

        boolean isOccupied = timetableService.isOccupied(facilityCode, dayOfWeek, timeSlot);

        if (isOccupied) {
            System.out.println("❌ " + facilityCode + " is occupied on " + dayOfWeek + " at " + timeSlot);
        } else {
            System.out.println("✅ " + facilityCode + " is available on " + dayOfWeek + " at " + timeSlot);
        }
    }

    /**
     * EXAMPLE 3: Get all occupied time slots for a facility on a specific day
     * 
     * Typical use case:
     * - User wants to see which hours are busy for Lab A405 on Tuesday
     * - Display these as "unavailable" slots in UI
     */
    public void example3_GetOccupiedSlots(
            FacilityTimetableService timetableService
    ) {
        String facilityCode = "A405";
        DayOfWeek dayOfWeek = DayOfWeek.TUESDAY;

        Set<LocalTime> occupiedSlots = timetableService.getOccupiedSlots(facilityCode, dayOfWeek);

        System.out.println("Occupied times for " + facilityCode + " on " + dayOfWeek + ":");
        occupiedSlots.stream()
                .sorted()
                .forEach(time -> System.out.println("  - " + time));
    }

    /**
     * EXAMPLE 4: Get all available time slots for a facility on a specific day
     * 
     * Typical use case:
     * - Student wants to book Auditorium G1302 on Friday
     * - System shows all available 1-hour slots within facility availability window (08:00-17:00)
     * - User can pick any green/available slot
     */
    public void example4_GetAvailableSlots(
            FacilityTimetableService timetableService
    ) {
        String facilityCode = "G1302"; // Auditorium
        DayOfWeek dayOfWeek = DayOfWeek.FRIDAY;

        Set<LocalTime> availableSlots = timetableService.getAvailableSlots(facilityCode, dayOfWeek);

        System.out.println("Available times for " + facilityCode + " on " + dayOfWeek + ":");
        availableSlots.stream()
                .sorted()
                .forEach(time -> System.out.println("  ✓ " + time + " - " + time.plusHours(1)));
    }

    /**
     * EXAMPLE 5: Use with FacilityService for operational checks
     * 
     * Typical use case:
     * - Booking system wants to validate a facility can be booked
     * - It calls isFacilityOperational(facilityId, startDateTime, endDateTime)
     * - Under the hood, this NOW checks:
     *   1. Facility status is not OUT_OF_SERVICE
     *   2. Facility status is not MAINTENANCE
     *   3. Time is within facility availability window
     *   4. Time is not under maintenance (integration check)
     *   5. ⭐ NEW: Time is not occupied in timetable
     * 
     * OLD (before timetable):
     *   result = isFacilityOperational(...) → checks 1-4 only
     * 
     * NEW (after timetable):
     *   result = isFacilityOperational(...) → checks 1-5 (including timetable)
     */
    public void example5_OperationalCheck(
            FacilityService facilityService
    ) {
        java.util.UUID facilityId = java.util.UUID.randomUUID();
        java.time.LocalDateTime startTime = java.time.LocalDateTime.of(2026, 4, 20, 14, 0);
        java.time.LocalDateTime endTime = java.time.LocalDateTime.of(2026, 4, 20, 15, 0);

        boolean isOperational = facilityService.isFacilityOperational(facilityId, startTime, endTime);

        if (isOperational) {
            System.out.println("✅ Facility can be booked for " + startTime + " to " + endTime);
        } else {
            System.out.println("❌ Facility is NOT available (occupied in timetable or other reasons)");
        }
    }

    /**
     * EXAMPLE 6: Query the new API endpoint
     * 
     * HTTP GET Request:
     *   GET /api/v1/facilities/{facilityId}/timetable-availability?day=MONDAY
     * 
     * Response:
     *   {
     *     "facility_code": "G1301",
     *     "facility_name": "Lecture Hall 1",
     *     "day": "MONDAY",
     *     "occupied_slots": ["08:00", "09:00", "10:00"],
     *     "available_slots": ["11:00", "12:00", "13:00", "14:00", "15:00", "16:00"],
     *     "total_occupied_count": 3,
     *     "total_available_count": 6,
     *     "timetable_loaded": true
     *   }
     * 
     * Typical use case:
     * - Frontend calls this endpoint to display occupancy calendar
     * - Shows colors: red = occupied, green = available
     */
    public void example6_ApiUsage() {
        String apiUrl = "GET /api/v1/facilities/550e8400-e29b-41d4-a716-446655440000/timetable-availability?day=MONDAY";
        String response = """
                {
                  "facility_code": "G1301",
                  "facility_name": "Lecture Hall 1",
                  "day": "MONDAY",
                  "occupied_slots": ["08:00", "09:00", "10:00"],
                  "available_slots": ["11:00", "12:00", "13:00", "14:00", "15:00", "16:00"],
                  "total_occupied_count": 3,
                  "total_available_count": 6,
                  "timetable_loaded": true
                }
                """;

        System.out.println("API Request: " + apiUrl);
        System.out.println("Response:\n" + response);
    }

    /**
     * EXAMPLE 7: Reload timetable (for admin/cron job)
     * 
     * Typical use case:
     * - Admin uploads new/updated timetable at semester start
     * - System should reload from file
     * - All subsequent queries use new data
     */
    public void example7_ReloadTimetable(
            FacilityTimetableService timetableService
    ) throws Exception {
        File updatedTimetableFile = new File("/path/to/updated_timetable.html");

        // Force reload = true to refresh even if already loaded
        timetableService.loadTimetable(updatedTimetableFile, true);

        System.out.println("✅ Timetable reloaded successfully");

        // Check statistics
        var stats = timetableService.getStatistics();
        System.out.println("Rooms in timetable: " + stats.get("roomsCount"));
        System.out.println("Total occupied slots: " + stats.get("totalOccupiedSlots"));
    }

    /**
     * EXAMPLE 8: HTML Timetable Format Reference
     * 
     * The parser expects HTML like this (FET-generated format):
     * 
     * <table>
     *   <tr>
     *     <th>Time</th>
     *     <th>MONDAY</th>
     *     <th>TUESDAY</th>
     *     <th>WEDNESDAY</th>
     *     <th>THURSDAY</th>
     *     <th>FRIDAY</th>
     *     <th>SATURDAY</th>
     *     <th>SUNDAY</th>
     *   </tr>
     *   <tr>
     *     <td>08:00</td>
     *     <td>G1301 - Data Structures (Lec 01)</td>
     *     <td>-x-</td>
     *     <td>G1301 - Data Structures (Lec 02)</td>
     *     <td>-x-</td>
     *     <td>G1301 - Lab (Prac 01)</td>
     *     <td>-x-</td>
     *     <td>-x-</td>
     *   </tr>
     *   <tr>
     *     <td>09:00</td>
     *     <td colspan="2">A405 - Advanced OOP</td>
     *     <td>-x-</td>
     *     <td>...</td>
     *   </tr>
     * </table>
     * 
     * Parser handles:
     * ✓ Room codes: "G1301", "A405", "AB12", "C1234"
     * ✓ Free markers: "-x-", "---", empty cells
     * ✓ Descriptions: "G1301 - Class Name" → extracts "G1301"
     * ✓ Rowspan/colspan for multi-hour sessions
     * ✓ Multiple tables (auto-merges)
     * ✓ Nested tables (extracts outer table)
     */
    public void example8_HtmlFormatReference() {
        System.out.println("""
                Expected Timetable HTML Format (FET-generated):
                
                - Column headers: MONDAY, TUESDAY, ..., SUNDAY
                - Row headers: Time slots (08:00, 09:00, ...)
                - Cell content: Room code (G1301, A405) or free marker (-x-, ---)
                - Supports: Room code - Description format
                - Handles: rowspan for multi-hour sessions
                
                Room Code Patterns (auto-detected):
                - Format: [A-Z][0-9]{2,4}
                - Examples: G1301, A405, AB12, C1234, B3001
                
                Free Markers:
                - "-x-" or "---" or empty cell
                """);
    }

    /**
     * EXAMPLE 9: Integration with booking validation
     * 
     * Pseudo-code flow:
     * 
     * 1. User submits booking request:
     *    - Facility: G1301
     *    - Date: 2026-04-20
     *    - Time: 14:00-15:00
     * 
     * 2. System validates:
     *    a. Facility exists
     *    b. User has permission
     *    c. Facility is ACTIVE (not maintenance/out-of-service)
     *    d. Time is within facility window (08:00-17:00)
     *    e. ⭐ NEW: Check timetable occupancy
     *       - isOccupied(G1301, MONDAY, 14:00) ?
     *       - If YES in timetable → REJECT booking
     *       - If NO → ALLOW booking
     * 
     * 3. If all checks pass:
     *    - Create booking
     *    - Send confirmation
     */
    public void example9_BookingValidationFlow() {
        System.out.println("""
                Booking Validation Flow:
                
                INPUT: Booking Request
                  - Facility: G1301
                  - DateTime: 2026-04-20 14:00-15:00
                
                CHECKS:
                  ✓ Facility exists
                  ✓ Facility is ACTIVE (not maintenance/out-of-service)
                  ✓ Time is within facility availability window (08:00-17:00)
                  ✓ No maintenance conflict
                  ✓ ⭐ No timetable occupancy (NEW!)
                
                DECISION:
                  - All checks PASS → ✅ Accept booking
                  - Any check FAILS → ❌ Reject with reason
                """);
    }

    /**
     * EXAMPLE 10: Troubleshooting & Statistics
     * 
     * When debugging timetable issues:
     * 1. Check if timetable is loaded
     * 2. View statistics
     * 3. Query specific facility occupancy
     * 4. Clear cache and reload if needed
     */
    public void example10_Troubleshooting(
            FacilityTimetableService timetableService
    ) {
        // Check status
        System.out.println("Timetable loaded: " + timetableService.isTimetableLoaded());

        // Get statistics
        var stats = timetableService.getStatistics();
        System.out.println("Statistics:");
        System.out.println("  Rooms: " + stats.get("roomsCount"));
        System.out.println("  Total occupied slots: " + stats.get("totalOccupiedSlots"));
        System.out.println("  Average slots per room: " + stats.get("averageSlotsPerRoom"));

        // Clear if corrupted
        if (Boolean.FALSE.equals(stats.get("isLoaded"))) {
            System.out.println("⚠️  Timetable not loaded, clearing cache...");
            timetableService.clearCache();
        }
    }
}

/**
 * ARCHITECTURE NOTES:
 * 
 * ┌─────────────────────────────────────────────────────────┐
 * │         API Layer (FacilityController)                  │
 * │  ✓ GET /facilities/{id}/timetable-availability?day=MON  │
 * └────────────────┬────────────────────────────────────────┘
 *                  │
 * ┌────────────────▼────────────────────────────────────────┐
 * │      Service Layer (FacilityService)                    │
 * │  ✓ isFacilityOperational() ← now includes timetable!   │
 * │  ✓ getFacilityById()                                    │
 * └────────────────┬────────────────────────────────────────┘
 *                  │
 * ┌────────────────▼────────────────────────────────────────┐
 * │   Timetable Layer (FacilityTimetableService)            │
 * │  ✓ In-memory cache (ConcurrentHashMap)                 │
 * │  ✓ getOccupiedSlots()                                   │
 * │  ✓ getAvailableSlots()                                  │
 * │  ✓ isOccupied()                                         │
 * │  ✓ isTimetableLoaded()                                  │
 * └────────────────┬────────────────────────────────────────┘
 *                  │
 * ┌────────────────▼────────────────────────────────────────┐
 * │   Parser Layer (TimetableParserService)                │
 * │  ✓ parseFile() / parseHtml()                            │
 * │  ✓ Handles: room extraction, rowspan, nested tables    │
 * └────────────────┬────────────────────────────────────────┘
 *                  │
 * ┌────────────────▼────────────────────────────────────────┐
 * │          Data (In-Memory Cache)                         │
 * │  Map<RoomCode, Map<DayOfWeek, Set<LocalTime>>>         │
 * └─────────────────────────────────────────────────────────┘
 * 
 * FLOW: HTML File
 *   → Parser (JSoup)
 *   → Normalized occupancy map
 *   → In-memory cache (ConcurrentHashMap)
 *   → Services query cache
 *   → Controller returns DTO
 *   → Frontend displays calendar
 * 
 * 
 * KEY DESIGN DECISIONS:
 * 
 * 1. IN-MEMORY CACHE (NOT DATABASE)
 *    - Why: Timetable is reference data, doesn't change often
 *    - Benefit: Fast lookups (O(1) map access)
 *    - Trade-off: Restart loses data (acceptable, reload from file)
 * 
 * 2. NORMALIZED ROOM CODES
 *    - HTML contains: "g1301", "G1301", "G 1301"
 *    - System normalizes to: "G1301" (uppercase, trimmed)
 *    - Benefit: Robust matching with facility codes
 * 
 * 3. HOURLY TIME SLOTS
 *    - Parser extracts hourly slots (08:00, 09:00, etc.)
 *    - Why: Matches FET output and booking granularity
 *    - Benefit: Simple availability math
 * 
 * 4. INTEGRATION WITH isFacilityOperational()
 *    - Timetable check is the LAST check
 *    - Why: Fail fast on status/window first, then timetable
 *    - Benefit: Clean separation of concerns
 * 
 * 5. NO FRAMEWORK OVERHEAD
 *    - Uses JSoup only (lightweight HTML parser)
 *    - No scheduling, events, or pub/sub
 *    - Why: Simple, easy to reason about
 * 
 */
