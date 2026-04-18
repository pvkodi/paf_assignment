package com.sliitreserve.api.unit.facility;

import com.sliitreserve.api.services.facility.TimetableParserService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration-style unit tests for {@link TimetableParserService} using the actual
 * SLIIT Semester I January 2026 Weekend timetable HTML as test input.
 *
 * <p>Each test targets a specific slot visible in the raw HTML so regressions
 * are detectable at the lowest granularity.
 */
@DisplayName("TimetableParserService – real HTML extraction")
public class TimetableParserServiceTest {

    private static TimetableParserService parser;
    private static List<TimetableParserService.ExtractedSlot> slots;
    private static Map<String, Map<DayOfWeek, Set<LocalTime>>> occupancy;

    @BeforeAll
    static void loadTimetable() throws Exception {
        parser = new TimetableParserService();

        URL resource = TimetableParserServiceTest.class
                .getClassLoader()
                .getResource("test-timetable.html");
        assertThat(resource)
                .as("test-timetable.html must exist in src/test/resources")
                .isNotNull();

        File htmlFile = new File(resource.toURI());
        slots     = parser.parseFileToSlots(htmlFile);
        occupancy = parser.buildOccupancyMap(slots);
    }

    @Test
    @DisplayName("Root test to ensure Surefire executes this class")
    void testRootExecution() {
        assertThat(slots).isNotEmpty();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  1.  Sanity: something was actually parsed
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Sanity checks")
    class SanityChecks {

        @Test
        @DisplayName("Should extract at least 100 slots from the full file")
        void shouldExtractManySlots() {
            assertThat(slots).hasSizeGreaterThan(100);
        }

        @Test
        @DisplayName("Should find at least 30 distinct room codes")
        void shouldFindManyRooms() {
            assertThat(occupancy.keySet()).hasSizeGreaterThan(30);
        }

        @Test
        @DisplayName("No extracted slot should have an empty room code")
        void noEmptyRoomCode() {
            assertThat(slots)
                    .extracting(TimetableParserService.ExtractedSlot::getRoomCode)
                    .allSatisfy(code -> assertThat(code).isNotBlank());
        }

        @Test
        @DisplayName("No extracted slot should have a free-marker as room code")
        void noFreeMarkerAsRoom() {
            assertThat(slots)
                    .extracting(TimetableParserService.ExtractedSlot::getRoomCode)
                    .doesNotContain("-x-", "---", "X", "");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  2.  Plain cell with rowspan: G602 Saturday 08:00–09:00
    //      HTML line 314: <td rowspan="2">IT1120 - IP Lecture<br/>…<br/>G602<br/></td>
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Plain cell extraction")
    class PlainCellExtraction {

        @Test
        @DisplayName("G602 should be occupied on Saturday at 08:00 (plain rowspan=2 cell)")
        void g602SaturdayAt08() {
            assertRoomOccupied("G602", DayOfWeek.SATURDAY, LocalTime.of(8, 0));
        }

        @Test
        @DisplayName("G602 should be occupied on Saturday at 09:00 (rowspan continuation)")
        void g602SaturdayAt09_rowspan() {
            assertRoomOccupied("G602", DayOfWeek.SATURDAY, LocalTime.of(9, 0));
        }

        @Test
        @DisplayName("E103 should be occupied on Saturday at 10:00 (plain rowspan=2 cell)")
        void e103SaturdayAt10() {
            assertRoomOccupied("E103", DayOfWeek.SATURDAY, LocalTime.of(10, 0));
        }

        @Test
        @DisplayName("E103 should be occupied on Saturday at 11:00 (rowspan continuation)")
        void e103SaturdayAt11_rowspan() {
            assertRoomOccupied("E103", DayOfWeek.SATURDAY, LocalTime.of(11, 0));
        }

        @Test
        @DisplayName("A506 should be occupied on Saturday at 16:00 (IT1130 MC Tutorial)")
        void a506SaturdayAt16() {
            assertRoomOccupied("A506", DayOfWeek.SATURDAY, LocalTime.of(16, 0));
        }

        @Test
        @DisplayName("G601 should be occupied on Sunday at 14:00 (IE1030 rowspan=3)")
        void g601SundayAt14() {
            assertRoomOccupied("G601", DayOfWeek.SUNDAY, LocalTime.of(14, 0));
        }

        @Test
        @DisplayName("G601 should be occupied on Sunday at 15:00 (rowspan continuation)")
        void g601SundayAt15() {
            assertRoomOccupied("G601", DayOfWeek.SUNDAY, LocalTime.of(15, 0));
        }

        @Test
        @DisplayName("G601 should be occupied on Sunday at 16:00 (rowspan continuation)")
        void g601SundayAt16() {
            assertRoomOccupied("G601", DayOfWeek.SUNDAY, LocalTime.of(16, 0));
        }

        @Test
        @DisplayName("F301 should be occupied on Sunday at 12:00 (IT1080 EAC Lecture)")
        void f301SundayAt12() {
            assertRoomOccupied("F301", DayOfWeek.SUNDAY, LocalTime.of(12, 0));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  3.  Nested detailed table: G1301 Sunday 08:00
    //      HTML line 315: <table class="detailed">…G1301…</table>  (single sub-group)
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Nested detailed table extraction")
    class DetailedTableExtraction {

        @Test
        @DisplayName("G1301 should be occupied on Sunday at 08:00 (single-group detailed table)")
        void g1301SundayAt08() {
            assertRoomOccupied("G1301", DayOfWeek.SUNDAY, LocalTime.of(8, 0));
        }

        @Test
        @DisplayName("G1301 should be occupied on Sunday at 09:00 (detailed table rowspan continuation)")
        void g1301SundayAt09() {
            assertRoomOccupied("G1301", DayOfWeek.SUNDAY, LocalTime.of(9, 0));
        }

        @Test
        @DisplayName("A405 should be occupied on Sunday at 10:00 (IT1130 MC Practical)")
        void a405SundayAt10() {
            assertRoomOccupied("A405", DayOfWeek.SUNDAY, LocalTime.of(10, 0));
        }

        @Test
        @DisplayName("G1302 should NOT be occupied on Sunday at 18:00 (IP Practical) - ignored after 18:00 rule")
        void g1302SundayAt18_Ignored() {
            assertRoomNotOccupied("G1302", DayOfWeek.SUNDAY, LocalTime.of(18, 0));
        }

        @Test
        @DisplayName("G1302 should NOT be occupied on Sunday at 19:00 (rowspan continuation) - ignored after 18:00 rule")
        void g1302SundayAt19_Ignored() {
            assertRoomNotOccupied("G1302", DayOfWeek.SUNDAY, LocalTime.of(19, 0));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  4.  Multiple rooms from the same detailed table cell
    //      HTML line 618 (Y1.S2.WE.IT.01 table):
    //      Saturday 08:00 detailed cell has 2 groups:
    //        col-0: B403-Pclab, col-1: G1303
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Multi-room detailed cell")
    class MultiRoomDetailedCell {

        @Test
        @DisplayName("B403 should be extracted from a multi-group detailed cell")
        void b403ExtractedFromDetailedCell() {
            // B403-Pclab → normalizes to B403
            assertRoomOccupied("B403", DayOfWeek.SATURDAY, LocalTime.of(8, 0));
        }

        @Test
        @DisplayName("G1303 should be extracted from the same multi-group detailed cell")
        void g1303ExtractedFromDetailedCell() {
            assertRoomOccupied("G1303", DayOfWeek.SATURDAY, LocalTime.of(8, 0));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  5.  Free slots must NOT produce any room entry
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Free slot filtering")
    class FreeSlotFiltering {

        @Test
        @DisplayName("Monday should have no occupied slots for Y1.S1.WE.IT.01 (all -x-)")
        void mondayAllFreeForFirstGroup() {
            // The first group (table_2) has -x- for ALL Monday columns
            // So Monday should NOT appear in G602's occupancy
            Map<DayOfWeek, Set<LocalTime>> g602Days = occupancy.get("G602");
            if (g602Days != null) {
                Set<LocalTime> mondaySlots = g602Days.get(DayOfWeek.MONDAY);
                assertThat(mondaySlots).isNullOrEmpty();
            }
            // pass if G602 has no Monday entry at all
        }

        @Test
        @DisplayName("Room code should not contain free-marker text")
        void roomsShouldNotBeFreeMarkers() {
            Set<String> allRooms = occupancy.keySet();
            assertThat(allRooms).doesNotContainAnyElementsOf(List.of("-x-", "---", "X", "FREE"));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  6.  Room code normalisation
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Room code normalisation")
    class RoomCodeNormalisation {

        @Test
        @DisplayName("'G602' plain text normalises to 'G602'")
        void normalisePlain() {
            assertThat(parser.normalizeRoomCode("G602")).isEqualTo("G602");
        }

        @Test
        @DisplayName("'E103 L' normalises to 'E103' (strip suffix)")
        void normaliseWithSuffix() {
            assertThat(parser.normalizeRoomCode("E103 L")).isEqualTo("E103");
        }

        @Test
        @DisplayName("'A410+A411 lab' normalises to 'A410+A411'")
        void normaliseComposite() {
            assertThat(parser.normalizeRoomCode("A410+A411 lab")).isEqualTo("A410+A411");
        }

        @Test
        @DisplayName("'F305-Pclab' normalises to 'F305'")
        void normalisePclab() {
            assertThat(parser.normalizeRoomCode("F305-Pclab")).isEqualTo("F305");
        }

        @Test
        @DisplayName("'B403-Pclab' normalises to 'B403'")
        void normaliseB403Pclab() {
            assertThat(parser.normalizeRoomCode("B403-Pclab")).isEqualTo("B403");
        }

        @Test
        @DisplayName("'-x-' normalises to empty string")
        void normaliseXMarker() {
            assertThat(parser.normalizeRoomCode("-x-")).isEmpty();
        }

        @Test
        @DisplayName("'---' normalises to empty string")
        void normaliseDashMarker() {
            assertThat(parser.normalizeRoomCode("---")).isEmpty();
        }

        @Test
        @DisplayName("null normalises to empty string")
        void normaliseNull() {
            assertThat(parser.normalizeRoomCode(null)).isEmpty();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  7.  Week coverage: Saturday and Sunday should dominate (weekend timetable)
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Weekend timetable coverage")
    class WeekendCoverage {

        @Test
        @DisplayName("Most occupied slots should be on Saturday or Sunday")
        void weekendSlotsAreMajority() {
            long weekendSlots = slots.stream()
                    .filter(s -> s.getDay() == DayOfWeek.SATURDAY || s.getDay() == DayOfWeek.SUNDAY)
                    .count();
            long total = slots.size();

            // Weekend timetable: at least 70% of slots should be Sat/Sun
            double ratio = (double) weekendSlots / total;
            assertThat(ratio)
                    .as("Expected >70%% of slots on Sat/Sun, actual ratio=%.2f", ratio)
                    .isGreaterThan(0.70);
        }

        @Test
        @DisplayName("Saturday should have slots for multiple rooms")
        void saturdayHasMultipleRooms() {
            long saturdayRooms = occupancy.entrySet().stream()
                    .filter(e -> e.getValue().containsKey(DayOfWeek.SATURDAY))
                    .count();
            assertThat(saturdayRooms).isGreaterThan(5);
        }

        @Test
        @DisplayName("Sunday should have slots for multiple rooms")
        void sundayHasMultipleRooms() {
            long sundayRooms = occupancy.entrySet().stream()
                    .filter(e -> e.getValue().containsKey(DayOfWeek.SUNDAY))
                    .count();
            assertThat(sundayRooms).isGreaterThan(5);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Helper
    // ─────────────────────────────────────────────────────────────────────────

    private void assertRoomOccupied(String room, DayOfWeek day, LocalTime time) {
        Map<DayOfWeek, Set<LocalTime>> roomOccupancy = occupancy.get(room);
        assertThat(roomOccupancy)
                .as("Room '%s' should have occupancy data (found rooms: %s)",
                        room, occupancy.keySet())
                .isNotNull();

        Set<LocalTime> times = roomOccupancy.get(day);
        assertThat(times)
                .as("Room '%s' should have slots on %s (found days: %s)",
                        room, day, roomOccupancy.keySet())
                .isNotNull()
                .isNotEmpty();

        assertThat(times)
                .as("Room '%s' on %s should be occupied at %s (found times: %s)",
                        room, day, time, times)
                .contains(time);
    }

    private void assertRoomNotOccupied(String room, DayOfWeek day, LocalTime time) {
        Map<DayOfWeek, Set<LocalTime>> roomOccupancy = occupancy.get(room);
        if (roomOccupancy == null || !roomOccupancy.containsKey(day)) {
            return; // Not occupied, test passes
        }
        assertThat(roomOccupancy.get(day))
                .as("Room '%s' on %s should NOT be occupied at %s (found times: %s)",
                        room, day, time, roomOccupancy.get(day))
                .doesNotContain(time);
    }
}
