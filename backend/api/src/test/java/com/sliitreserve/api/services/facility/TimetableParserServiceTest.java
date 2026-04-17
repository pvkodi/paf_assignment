package com.sliitreserve.api.services.facility;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TimetableParserService.
 * Tests HTML parsing, rowspan handling, room code extraction, and edge cases.
 */
@DisplayName("TimetableParserService Tests")
public class TimetableParserServiceTest {

    private TimetableParserService parserService;

    @BeforeEach
    void setUp() {
        parserService = new TimetableParserService();
    }

    @Test
    @DisplayName("Should parse simple timetable HTML with basic structure")
    void testParseSimpleTimetable() {
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
                        <td>-x-</td>
                    </tr>
                    <tr>
                        <td>09:00</td>
                        <td>-x-</td>
                        <td>A405</td>
                    </tr>
                </table>
                </html>
                """;

        Map<String, Map<DayOfWeek, Set<LocalTime>>> result = parserService.parseHtml(html);

        assertNotNull(result);
        assertTrue(result.containsKey("G1301"));
        assertTrue(result.get("G1301").get(DayOfWeek.MONDAY).contains(LocalTime.of(8, 0)));
    }

    @Test
    @DisplayName("Should normalize room codes to uppercase")
    void testRoomCodeNormalization() {
        String html = """
                <html>
                <table>
                    <tr>
                        <th>Time</th>
                        <th>MONDAY</th>
                    </tr>
                    <tr>
                        <td>08:00</td>
                        <td>g1301 - Lecture</td>
                    </tr>
                </table>
                </html>
                """;

        Map<String, Map<DayOfWeek, Set<LocalTime>>> result = parserService.parseHtml(html);

        assertNotNull(result);
        assertTrue(result.containsKey("G1301"));
    }

    @Test
    @DisplayName("Should handle free slot markers (-x-, ---, empty cells)")
    void testFreeSlotDetection() {
        String html = """
                <html>
                <table>
                    <tr>
                        <th>Time</th>
                        <th>MONDAY</th>
                        <th>TUESDAY</th>
                        <th>WEDNESDAY</th>
                    </tr>
                    <tr>
                        <td>08:00</td>
                        <td>-x-</td>
                        <td>---</td>
                        <td></td>
                    </tr>
                </table>
                </html>
                """;

        Map<String, Map<DayOfWeek, Set<LocalTime>>> result = parserService.parseHtml(html);

        assertNotNull(result);
        assertEquals(0, result.size()); // No rooms occupied
    }

    @Test
    @DisplayName("Should handle rowspan expansion")
    void testRowspanHandling() {
        String html = """
                <html>
                <table>
                    <tr>
                        <th>Time</th>
                        <th>MONDAY</th>
                    </tr>
                    <tr>
                        <td>08:00</td>
                        <td rowspan="2">G1301</td>
                    </tr>
                    <tr>
                        <td>09:00</td>
                    </tr>
                </table>
                </html>
                """;

        Map<String, Map<DayOfWeek, Set<LocalTime>>> result = parserService.parseHtml(html);

        // After rowspan expansion, both 08:00 and 09:00 should be marked as occupied
        assertNotNull(result);
        assertTrue(result.containsKey("G1301"));
    }

    @Test
    @DisplayName("Should extract rooms from complex room/class descriptions")
    void testComplexRoomDescriptionParsing() {
        String html = """
                <html>
                <table>
                    <tr>
                        <th>Time</th>
                        <th>MONDAY</th>
                    </tr>
                    <tr>
                        <td>08:00</td>
                        <td>G1301 - Data Structures (Lec 01)</td>
                    </tr>
                    <tr>
                        <td>09:00</td>
                        <td>AB1234 – Advanced OOP</td>
                    </tr>
                    <tr>
                        <td>10:00</td>
                        <td>C405: Practicum</td>
                    </tr>
                </table>
                </html>
                """;

        Map<String, Map<DayOfWeek, Set<LocalTime>>> result = parserService.parseHtml(html);

        assertNotNull(result);
        assertTrue(result.containsKey("G1301"), "Should extract G1301");
        assertTrue(result.containsKey("AB1234"), "Should extract AB1234");
        assertTrue(result.containsKey("C405"), "Should extract C405");
    }

    @Test
    @DisplayName("Should handle multiple timetables in same document")
    void testMultipleTables() {
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
                <table>
                    <tr>
                        <th>Time</th>
                        <th>TUESDAY</th>
                    </tr>
                    <tr>
                        <td>10:00</td>
                        <td>G1301</td>
                    </tr>
                </table>
                </html>
                """;

        Map<String, Map<DayOfWeek, Set<LocalTime>>> result = parserService.parseHtml(html);

        // Second table should be merged with first
        assertNotNull(result);
        assertTrue(result.containsKey("G1301"));
        assertTrue(result.get("G1301").get(DayOfWeek.MONDAY).contains(LocalTime.of(8, 0)));
        assertTrue(result.get("G1301").get(DayOfWeek.TUESDAY).contains(LocalTime.of(10, 0)));
    }

    @Test
    @DisplayName("Should handle all days of the week")
    void testAllDaysOfWeek() {
        String html = """
                <html>
                <table>
                    <tr>
                        <th>Time</th>
                        <th>MONDAY</th>
                        <th>TUESDAY</th>
                        <th>WEDNESDAY</th>
                        <th>THURSDAY</th>
                        <th>FRIDAY</th>
                        <th>SATURDAY</th>
                        <th>SUNDAY</th>
                    </tr>
                    <tr>
                        <td>08:00</td>
                        <td>G1301</td>
                        <td>G1301</td>
                        <td>G1301</td>
                        <td>G1301</td>
                        <td>G1301</td>
                        <td>-x-</td>
                        <td>-x-</td>
                    </tr>
                </table>
                </html>
                """;

        Map<String, Map<DayOfWeek, Set<LocalTime>>> result = parserService.parseHtml(html);

        assertNotNull(result);
        assertTrue(result.containsKey("G1301"));
        Map<DayOfWeek, Set<LocalTime>> g1301 = result.get("G1301");
        
        // Should have Monday through Friday
        for (DayOfWeek day : new DayOfWeek[]{DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY}) {
            assertTrue(g1301.get(day).contains(LocalTime.of(8, 0)));
        }
    }

    @Test
    @DisplayName("Should parse time ranges and use start time")
    void testTimeRangeParsing() {
        String html = """
                <html>
                <table>
                    <tr>
                        <th>Time</th>
                        <th>MONDAY</th>
                    </tr>
                    <tr>
                        <td>08:00-09:00</td>
                        <td>G1301</td>
                    </tr>
                </table>
                </html>
                """;

        Map<String, Map<DayOfWeek, Set<LocalTime>>> result = parserService.parseHtml(html);

        assertNotNull(result);
        assertTrue(result.containsKey("G1301"));
        assertTrue(result.get("G1301").get(DayOfWeek.MONDAY).contains(LocalTime.of(8, 0)));
    }

    @Test
    @DisplayName("Should handle malformed HTML gracefully")
    void testMalformedHtmlHandling() {
        String html = """
                <html>
                <table>
                    <tr>
                        <td>08:00</td>
                        <td>G1301</td>
                    </tr>
                </table>
                </html>
                """;

        // Should not throw, should return empty or best-effort result
        assertDoesNotThrow(() -> parserService.parseHtml(html));
        Map<String, Map<DayOfWeek, Set<LocalTime>>> result = parserService.parseHtml(html);
        assertNotNull(result);
    }

    @Test
    @DisplayName("Should handle nested tables")
    void testNestedTableParsing() {
        String html = """
                <html>
                <table>
                    <tr>
                        <th>Time</th>
                        <th>MONDAY</th>
                    </tr>
                    <tr>
                        <td>08:00</td>
                        <td>
                            <table class="detailed">
                                <tr><td>G1301</td></tr>
                            </table>
                        </td>
                    </tr>
                </table>
                </html>
                """;

        // Should parse outer table
        Map<String, Map<DayOfWeek, Set<LocalTime>>> result = parserService.parseHtml(html);
        assertNotNull(result);
    }

    @Test
    @DisplayName("Should handle empty timetable")
    void testEmptyTimetable() {
        String html = "<html><table></table></html>";

        Map<String, Map<DayOfWeek, Set<LocalTime>>> result = parserService.parseHtml(html);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should handle whitespace in room codes")
    void testWhitespaceHandling() {
        String html = """
                <html>
                <table>
                    <tr>
                        <th>Time</th>
                        <th>MONDAY</th>
                    </tr>
                    <tr>
                        <td>08:00</td>
                        <td>  G1301  </td>
                    </tr>
                </table>
                </html>
                """;

        Map<String, Map<DayOfWeek, Set<LocalTime>>> result = parserService.parseHtml(html);

        assertNotNull(result);
        assertTrue(result.containsKey("G1301"));
    }
}
