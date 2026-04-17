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

    @Mock
    private TimetableParserService parserService;

    private FacilityTimetableService timetableService;

    @BeforeEach
    void setUp() {
        timetableService = new FacilityTimetableService(facilityRepository, parserService);
    }

    @Test
    @DisplayName("Should cache timetable after loading")
    void testTimetableCaching() {
        String html = """
                <html>
                <table>
                    <tr>
                        <th>Time</th>
                        <th>MONDAY</th>
                    </tr>
                    <tr>
                        <td>08:00</td>
                        <td>G1301</td>
                    </tr>
                </table>
                </html>
                """;

        assertFalse(timetableService.isTimetableLoaded());
        
        timetableService.loadTimetableFromHtml(html, false);
        
        assertTrue(timetableService.isTimetableLoaded());
    }

    @Test
    @DisplayName("Should not reload if already loaded (unless forced)")
    void testNoCacheReloadWithoutForce() {
        String html1 = """
                <html>
                <table>
                    <tr>
                        <th>Time</th>
                        <th>MONDAY</th>
                    </tr>
                    <tr>
                        <td>08:00</td>
                        <td>G1301</td>
                    </tr>
                </table>
                </html>
                """;

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
        String html = """
                <html>
                <table>
                    <tr>
                        <th>Time</th>
                        <th>MONDAY</th>
                    </tr>
                    <tr>
                        <td>08:00</td>
                        <td>G1301</td>
                    </tr>
                </table>
                </html>
                """;

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
        String html = """
                <html>
                <table>
                    <tr>
                        <th>Time</th>
                        <th>MONDAY</th>
                    </tr>
                    <tr>
                        <td>08:00</td>
                        <td>G1301</td>
                    </tr>
                    <tr>
                        <td>09:00</td>
                        <td>-x-</td>
                    </tr>
                </table>
                </html>
                """;

        // Setup mock facility
        Facility mockFacility = new Facility();
        mockFacility.setFacilityCode("G1301");
        mockFacility.setAvailabilityStartTime(LocalTime.of(8, 0));
        mockFacility.setAvailabilityEndTime(LocalTime.of(17, 0));
        when(facilityRepository.findByFacilityCode("G1301")).thenReturn(Optional.of(mockFacility));

        timetableService.loadTimetableFromHtml(html, false);

        assertTrue(timetableService.isOccupied("G1301", DayOfWeek.MONDAY, LocalTime.of(8, 0)));
        assertFalse(timetableService.isOccupied("G1301", DayOfWeek.MONDAY, LocalTime.of(9, 0)));
    }

    @Test
    @DisplayName("Should get occupied slots for a facility on a day")
    void testGetOccupiedSlots() {
        String html = """
                <html>
                <table>
                    <tr>
                        <th>Time</th>
                        <th>MONDAY</th>
                    </tr>
                    <tr>
                        <td>08:00</td>
                        <td>G1301</td>
                    </tr>
                    <tr>
                        <td>09:00</td>
                        <td>G1301</td>
                    </tr>
                    <tr>
                        <td>10:00</td>
                        <td>-x-</td>
                    </tr>
                </table>
                </html>
                """;

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
        String html = """
                <html>
                <table>
                    <tr>
                        <th>Time</th>
                        <th>MONDAY</th>
                    </tr>
                    <tr>
                        <td>08:00</td>
                        <td>G1301</td>
                    </tr>
                    <tr>
                        <td>09:00</td>
                        <td>-x-</td>
                    </tr>
                    <tr>
                        <td>10:00</td>
                        <td>-x-</td>
                    </tr>
                </table>
                </html>
                """;

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
        String html = """
                <html>
                <table>
                    <tr>
                        <th>Time</th>
                        <th>MONDAY</th>
                    </tr>
                    <tr>
                        <td>08:00</td>
                        <td>g1301</td>
                    </tr>
                </table>
                </html>
                """;

        timetableService.loadTimetableFromHtml(html, false);

        assertTrue(timetableService.isOccupied("G1301", DayOfWeek.MONDAY, LocalTime.of(8, 0)));
        assertTrue(timetableService.isOccupied("g1301", DayOfWeek.MONDAY, LocalTime.of(8, 0)));
    }

    @Test
    @DisplayName("Should return statistics")
    void testGetStatistics() {
        String html = """
                <html>
                <table>
                    <tr>
                        <th>Time</th>
                        <th>MONDAY</th>
                    </tr>
                    <tr>
                        <td>08:00</td>
                        <td>G1301</td>
                    </tr>
                    <tr>
                        <td>09:00</td>
                        <td>A405</td>
                    </tr>
                </table>
                </html>
                """;

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
        String html = """
                <html>
                <table>
                    <tr>
                        <th>Time</th>
                        <th>MONDAY</th>
                    </tr>
                    <tr>
                        <td>08:00</td>
                        <td>G1301</td>
                    </tr>
                </table>
                </html>
                """;

        timetableService.loadTimetableFromHtml(html, false);
        assertTrue(timetableService.isTimetableLoaded());

        timetableService.clearCache();
        
        assertFalse(timetableService.isTimetableLoaded());
        assertTrue(timetableService.getOccupiedSlots("G1301", DayOfWeek.MONDAY).isEmpty());
    }

    @Test
    @DisplayName("Should return facility occupancy across all days")
    void testGetFacilityOccupancy() {
        String html = """
                <html>
                <table>
                    <tr>
                        <th>Time</th>
                        <th>MONDAY</th>
                        <th>TUESDAY</th>
                    </tr>
                    <tr>
                        <td>08:00</td>
                        <td>G1301</td>
                        <td>G1301</td>
                    </tr>
                </table>
                </html>
                """;

        timetableService.loadTimetableFromHtml(html, false);
        var occupancy = timetableService.getFacilityOccupancy("G1301");

        assertNotNull(occupancy);
        assertTrue(occupancy.containsKey(DayOfWeek.MONDAY));
        assertTrue(occupancy.containsKey(DayOfWeek.TUESDAY));
    }
}
