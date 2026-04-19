package com.sliitreserve.api.services.facility;

import com.sliitreserve.api.entities.facility.Facility;
import com.sliitreserve.api.repositories.facility.FacilityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for FacilityTimetableService.
 * Tests caching, availability queries, and facility resolution.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FacilityTimetableService Tests")
public class FacilityTimetableServiceTest {

    @Mock
    private FacilityRepository facilityRepository;

    private TimetableParserService parserService;
    private FacilityTimetableService timetableService;

    private static String wrapFetTable(String rows) {
        return """
                <!DOCTYPE html>
                <html><body>
                <table id="table_1" border="1">
                  <thead>
                    <tr><td rowspan="2"></td><th colspan="7">Group1</th></tr>
                    <tr>
                      <th class="xAxis">Monday</th>
                      <th class="xAxis">Tuesday</th>
                      <th class="xAxis">Wednesday</th>
                      <th class="xAxis">Thursday</th>
                      <th class="xAxis">Friday</th>
                      <th class="xAxis">Saturday</th>
                      <th class="xAxis">Sunday</th>
                    </tr>
                  </thead>
                  <tbody>
                """ + rows + """
                  </tbody>
                </table>
                </body></html>
                """;
    }

    @BeforeEach
    void setUp() {
        parserService = new TimetableParserService();
        timetableService = new FacilityTimetableService(facilityRepository, parserService);
    }

    @Test
    @DisplayName("Should cache timetable after loading")
    void testTimetableCaching() {
        String html = wrapFetTable("""
                <tr>
                  <th class="yAxis">08:00</th>
                  <td>Group A<br/>Tutor<br/>G1301<br/></td>
                  <td>-x-</td><td>-x-</td><td>-x-</td><td>-x-</td><td>-x-</td><td>-x-</td>
                </tr>
                """);

        assertFalse(timetableService.isTimetableLoaded());
        
        timetableService.loadTimetableFromHtml(html, false);
        
        assertTrue(timetableService.isTimetableLoaded());
    }

    @Test
    @DisplayName("Should not reload if already loaded (unless forced)")
    void testNoCacheReloadWithoutForce() {
        String html1 = wrapFetTable("""
                <tr>
                  <th class="yAxis">08:00</th>
                  <td>Group A<br/>Tutor<br/>G1301<br/></td>
                  <td>-x-</td><td>-x-</td><td>-x-</td><td>-x-</td><td>-x-</td><td>-x-</td>
                </tr>
                """);

        timetableService.loadTimetableFromHtml(html1, false);
        Map<String, Object> stats1 = timetableService.getStatistics();

        // Try to load again without force
        timetableService.loadTimetableFromHtml(html1, false);
        Map<String, Object> stats2 = timetableService.getStatistics();

        assertEquals(stats1, stats2);
    }

    @Test
    @DisplayName("Should reload with forceReload=true")
    void testForcedCacheReload() {
        String html = wrapFetTable("""
                <tr>
                  <th class="yAxis">08:00</th>
                  <td>Group A<br/>Tutor<br/>G1301<br/></td>
                  <td>-x-</td><td>-x-</td><td>-x-</td><td>-x-</td><td>-x-</td><td>-x-</td>
                </tr>
                """);

        timetableService.loadTimetableFromHtml(html, false);
        timetableService.clearCache();
        
        assertFalse(timetableService.isTimetableLoaded());
        
        timetableService.loadTimetableFromHtml(html, true);
        
        assertTrue(timetableService.isTimetableLoaded());
    }

    @Test
    @DisplayName("Should return empty sets when timetable not loaded")
    void testNoResultsBeforeLoding() {
        Set<LocalTime> occupied = timetableService.getOccupiedSlots("G1301", DayOfWeek.MONDAY);
        assertTrue(occupied.isEmpty());

        Set<LocalTime> available = timetableService.getAvailableSlots("G1301", DayOfWeek.MONDAY);
        assertTrue(available.isEmpty());
    }

    @Test
    @DisplayName("Should check occupancy correctly")
    void testOccupancyCheck() {
        String html = wrapFetTable("""
                <tr>
                  <th class="yAxis">08:00</th>
                  <td>Group A<br/>Tutor<br/>G1301<br/></td>
                  <td>-x-</td><td>-x-</td><td>-x-</td><td>-x-</td><td>-x-</td><td>-x-</td>
                </tr>
                <tr>
                  <th class="yAxis">09:00</th>
                  <td>-x-</td><td>-x-</td><td>-x-</td><td>-x-</td><td>-x-</td><td>-x-</td><td>-x-</td>
                </tr>
                """);

        timetableService.loadTimetableFromHtml(html, false);

        assertTrue(timetableService.isOccupied("G1301", DayOfWeek.MONDAY, LocalTime.of(8, 0)));
        assertFalse(timetableService.isOccupied("G1301", DayOfWeek.MONDAY, LocalTime.of(9, 0)));
    }

    @Test
    @DisplayName("Should get occupied slots for a facility on a day")
    void testGetOccupiedSlots() {
        String html = wrapFetTable("""
                <tr>
                  <th class="yAxis">08:00</th>
                  <td rowspan="2">Group A<br/>Tutor<br/>G1301<br/></td>
                  <td>-x-</td><td>-x-</td><td>-x-</td><td>-x-</td><td>-x-</td><td>-x-</td>
                </tr>
                <tr>
                  <th class="yAxis">09:00</th>
                  <!-- spanned -->
                  <td>-x-</td><td>-x-</td><td>-x-</td><td>-x-</td><td>-x-</td><td>-x-</td>
                </tr>
                <tr>
                  <th class="yAxis">10:00</th>
                  <td>-x-</td><td>-x-</td><td>-x-</td><td>-x-</td><td>-x-</td><td>-x-</td><td>-x-</td>
                </tr>
                """);

        timetableService.loadTimetableFromHtml(html, false);
        Set<LocalTime> occupiedSlots = timetableService.getOccupiedSlots("G1301", DayOfWeek.MONDAY);

        assertEquals(2, occupiedSlots.size());
        assertTrue(occupiedSlots.contains(LocalTime.of(8, 0)));
        assertTrue(occupiedSlots.contains(LocalTime.of(9, 0)));
        assertFalse(occupiedSlots.contains(LocalTime.of(10, 0)));
    }

    @Test
    @DisplayName("Should get available slots using facility availability window")
    void testGetAvailableSlots() {
        String html = wrapFetTable("""
                <tr>
                  <th class="yAxis">08:00</th>
                  <td>Group A<br/>Tutor<br/>G1301<br/></td>
                  <td>-x-</td><td>-x-</td><td>-x-</td><td>-x-</td><td>-x-</td><td>-x-</td>
                </tr>
                <tr>
                  <th class="yAxis">09:00</th>
                  <td>-x-</td><td>-x-</td><td>-x-</td><td>-x-</td><td>-x-</td><td>-x-</td><td>-x-</td>
                </tr>
                <tr>
                  <th class="yAxis">10:00</th>
                  <td>-x-</td><td>-x-</td><td>-x-</td><td>-x-</td><td>-x-</td><td>-x-</td><td>-x-</td>
                </tr>
                """);

        Facility mockFacility = new Facility();
        mockFacility.setFacilityCode("G1301");
        mockFacility.setAvailabilityStartTime(LocalTime.of(8, 0));
        mockFacility.setAvailabilityEndTime(LocalTime.of(11, 0)); // 08:00-11:00
        when(facilityRepository.findByFacilityCode("G1301")).thenReturn(Optional.of(mockFacility));

        timetableService.loadTimetableFromHtml(html, false);
        Set<LocalTime> availableSlots = timetableService.getAvailableSlots("G1301", DayOfWeek.MONDAY);

        assertEquals(2, availableSlots.size());
        assertTrue(availableSlots.contains(LocalTime.of(9, 0)));
        assertTrue(availableSlots.contains(LocalTime.of(10, 0)));
        assertFalse(availableSlots.contains(LocalTime.of(8, 0)));
    }

    @Test
    @DisplayName("Should case-insensitively match facility codes")
    void testCaseInsensitiveFacilityCodeMatching() {
        String html = wrapFetTable("""
                <tr>
                  <th class="yAxis">08:00</th>
                  <td>Group A<br/>Tutor<br/>g1301<br/></td>
                  <td>-x-</td><td>-x-</td><td>-x-</td><td>-x-</td><td>-x-</td><td>-x-</td>
                </tr>
                """);

        timetableService.loadTimetableFromHtml(html, false);

        assertTrue(timetableService.isOccupied("G1301", DayOfWeek.MONDAY, LocalTime.of(8, 0)));
        assertTrue(timetableService.isOccupied("g1301", DayOfWeek.MONDAY, LocalTime.of(8, 0)));
    }

    @Test
    @DisplayName("Should return statistics")
    void testGetStatistics() {
        String html = wrapFetTable("""
                <tr>
                  <th class="yAxis">08:00</th>
                  <td>Group A<br/>Tutor<br/>G1301<br/></td>
                  <td>-x-</td><td>-x-</td><td>-x-</td><td>-x-</td><td>-x-</td><td>-x-</td>
                </tr>
                <tr>
                  <th class="yAxis">09:00</th>
                  <td>Group A<br/>Tutor<br/>A405<br/></td>
                  <td>-x-</td><td>-x-</td><td>-x-</td><td>-x-</td><td>-x-</td><td>-x-</td>
                </tr>
                """);

        timetableService.loadTimetableFromHtml(html, false);
        Map<String, Object> stats = timetableService.getStatistics();

        assertNotNull(stats);
        assertTrue((Boolean) stats.get("isLoaded"));
        assertEquals(2, stats.get("roomsCount"));
        assertNotNull(stats.get("totalOccupiedSlots"));
        assertNotNull(stats.get("averageSlotsPerRoom"));
    }

    @Test
    @DisplayName("Should clear cache properly")
    void testClearCache() {
        String html = wrapFetTable("""
                <tr>
                  <th class="yAxis">08:00</th>
                  <td>Group A<br/>Tutor<br/>G1301<br/></td>
                  <td>-x-</td><td>-x-</td><td>-x-</td><td>-x-</td><td>-x-</td><td>-x-</td>
                </tr>
                """);

        timetableService.loadTimetableFromHtml(html, false);
        assertTrue(timetableService.isTimetableLoaded());

        timetableService.clearCache();
        
        assertFalse(timetableService.isTimetableLoaded());
        assertTrue(timetableService.getOccupiedSlots("G1301", DayOfWeek.MONDAY).isEmpty());
    }

    @Test
    @DisplayName("Should return facility occupancy across all days")
    void testGetFacilityOccupancy() {
        String html = wrapFetTable("""
                <tr>
                  <th class="yAxis">08:00</th>
                  <td>Group A<br/>Tutor<br/>G1301<br/></td>
                  <td>Group A<br/>Tutor<br/>G1301<br/></td>
                  <td>-x-</td><td>-x-</td><td>-x-</td><td>-x-</td><td>-x-</td>
                </tr>
                """);

        timetableService.loadTimetableFromHtml(html, false);
        var occupancy = timetableService.getFacilityOccupancy("G1301");

        assertNotNull(occupancy);
        assertTrue(occupancy.containsKey(DayOfWeek.MONDAY));
        assertTrue(occupancy.containsKey(DayOfWeek.TUESDAY));
    }
}
