package com.sliitreserve.api.services.facility;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Parses university FET-generated timetable HTML files.
 *
 * <p>FET timetable HTML structure:
 * <ul>
 *   <li>Each {@code <table>} in the body is a student-group timetable.</li>
 *   <li>thead row 1: group name (colspan); row 2: day headers (Monday…Sunday).</li>
 *   <li>tbody rows: first {@code <th class="yAxis">} is the hour (e.g. {@code 08:00}),
 *       followed by one {@code <td>} per day.</li>
 *   <li>Free cells contain {@code -x-} (blocked) or {@code ---} (empty).</li>
 *   <li>Occupied cells are either:
 *     <ul>
 *       <li><b>Plain</b>: {@code Subject – Type<br/>Lecturer<br/>RoomCode<br/>}</li>
 *       <li><b>Detailed</b>: a nested {@code <table class="detailed">} where each
 *           column represents a sub-group; each column has rows:
 *           sub-group, subject, lecturers, <b>room</b>.</li>
 *     </ul>
 *   </li>
 *   <li>{@code rowspan} on a cell means the same class continues for that many hours.</li>
 * </ul>
 *
 * <p>The parser produces {@link ExtractedSlot} objects (one per room+day+hour combination)
 * and also builds the canonical {@code Map<roomCode, Map<DayOfWeek, Set<LocalTime>>>}
 * occupancy cache used by {@link FacilityTimetableService}.
 */
@Service
@Slf4j
public class TimetableParserService {

    // ── Patterns ────────────────────────────────────────────────────────────

    /** Matches FET time headers: 08:00, 9:00, 18:00 */
    private static final Pattern TIME_PATTERN = Pattern.compile("^(\\d{1,2}):(\\d{2})$");

    /**
     * Matches room codes as found in timetable cells.
     *
     * Examples from the real file:
     *   G602, G1301, G1302, G1303, G603, G601
     *   E103 L, E204, E304
     *   A405, A503, A506, A410+A411 lab
     *   F301, F303, F305-Pclab
     *   B403-Pclab, B402-Pclab
     *
     * Strategy: the room code is always the last non-empty text line in a `<br>`-delimited
     * plain cell, or the last `<td>` row in a detailed table column.  We clean the raw
     * string and normalise it.
     */
    private static final Pattern ROOM_CODE_PATTERN =
            Pattern.compile("^([A-Z]\\d{1,4}(?:[+][A-Z]\\d{1,4})?(?:[- ].{0,10})?)$");

    private static final Set<String> FREE_MARKERS = Set.of("-x-", "---", "");

    // ── Public API ───────────────────────────────────────────────────────────

    /**
     * Parse timetable from a file.
     *
     * @return aggregated occupancy: roomCode → day → set of occupied start-times
     */
    public Map<String, Map<DayOfWeek, Set<LocalTime>>> parseFile(File htmlFile) throws IOException {
        Document doc = Jsoup.parse(htmlFile, "UTF-8");
        return buildOccupancyMap(parseDocument(doc));
    }

    /**
     * Parse timetable from an HTML string.
     *
     * @return aggregated occupancy: roomCode → day → set of occupied start-times
     */
    public Map<String, Map<DayOfWeek, Set<LocalTime>>> parseHtml(String htmlContent) {
        Document doc = Jsoup.parse(htmlContent);
        return buildOccupancyMap(parseDocument(doc));
    }

    /**
     * Parse a file and return the full list of extracted slots (for testing/reporting).
     */
    public List<ExtractedSlot> parseFileToSlots(File htmlFile) throws IOException {
        Document doc = Jsoup.parse(htmlFile, "UTF-8");
        return parseDocument(doc);
    }

    /**
     * Parse an HTML string and return the full list of extracted slots (for testing/reporting).
     */
    public List<ExtractedSlot> parseHtmlToSlots(String htmlContent) {
        Document doc = Jsoup.parse(htmlContent);
        return parseDocument(doc);
    }

    // ── Core parsing ─────────────────────────────────────────────────────────

    private List<ExtractedSlot> parseDocument(Document doc) {
        List<ExtractedSlot> allSlots = new ArrayList<>();

        // Select only top-level timetable tables (exclude nested detail tables)
        Elements tables = doc.select("body > table[id]");
        log.debug("Found {} timetable tables", tables.size());

        for (Element table : tables) {
            try {
                List<ExtractedSlot> slots = extractFromTable(table);
                allSlots.addAll(slots);
            } catch (Exception e) {
                log.warn("Failed to parse table {}: {}", table.id(), e.getMessage());
            }
        }

        log.info("Parsed timetable: {} total room-slot entries across {} tables",
                allSlots.size(), tables.size());
        return allSlots;
    }

    /**
     * Extract all occupied slots from a single student-group timetable table.
     *
     * <p>Algorithm:
     * <ol>
     *   <li>Find the day-column headers.</li>
     *   <li>Walk body rows; first {@code <th class=yAxis>} gives the hour.</li>
     *   <li>For each data cell, resolve the actual column index accounting for
     *       rowspan cells injected from previous rows.</li>
     *   <li>Extract rooms from the cell (plain or detailed table).</li>
     *   <li>For rowspan > 1, register the same rooms for subsequent hours.</li>
     * </ol>
     */
    private List<ExtractedSlot> extractFromTable(Element table) {
        List<ExtractedSlot> slots = new ArrayList<>();

        // ── 1. Collect day headers ──────────────────────────────────────────
        List<DayOfWeek> dayColumns = parseDayHeaders(table);
        if (dayColumns.isEmpty()) {
            log.trace("Skipping table {} – no day headers found", table.id());
            return slots;
        }

        // ── 2. Collect time-axis labels ────────────────────────────────────
        // We need an ordered list of hours so rowspan can propagate to next hours
        List<LocalTime> timeAxis = parseTimeAxis(table);

        // ── 3. Build a grid: grid[rowIdx][colIdx] = room info list ─────────
        //    We expand rowspan here so every logical cell is filled.
        Elements bodyRows = table.select("tbody > tr");

        // grid[timeIdx][dayIdx] = list of rooms (may be empty for free slots)
        int numTimes = timeAxis.size();
        int numDays  = dayColumns.size();

        @SuppressWarnings("unchecked")
        List<RoomEntry>[][] grid = new List[numTimes][numDays];
        for (int r = 0; r < numTimes; r++) {
            for (int c = 0; c < numDays; c++) {
                grid[r][c] = new ArrayList<>();
            }
        }

        // Track which grid cells are already filled (due to rowspan from above)
        boolean[][] filled = new boolean[numTimes][numDays];

        int timeIdx = 0;
        for (Element row : bodyRows) {
            // Skip footer row
            if (row.hasClass("foot")) continue;

            Element timeHeader = row.selectFirst("th.yAxis");
            if (timeHeader == null) continue;

            LocalTime hour = parseTime(timeHeader.text().trim());
            if (hour == null) continue;

            timeIdx = timeAxis.indexOf(hour);
            if (timeIdx < 0) continue;

            // Collect data cells (td only – not the th time header)
            Elements dataCells = row.select("> td");

            int dayIdx = 0;
            for (Element cell : dataCells) {
                // Advance past cells already filled by a rowspan above
                while (dayIdx < numDays && filled[timeIdx][dayIdx]) {
                    dayIdx++;
                }
                if (dayIdx >= numDays) break;

                int rowspan = parseRowspan(cell);

                // Extract room entries from this cell
                List<RoomEntry> entries = extractRooms(cell);
                grid[timeIdx][dayIdx] = entries;
                filled[timeIdx][dayIdx] = true;

                // Propagate rowspan – mark subsequent rows for this day-column
                if (rowspan > 1) {
                    for (int extra = 1; extra < rowspan; extra++) {
                        int nextTimeIdx = timeIdx + extra;
                        if (nextTimeIdx < numTimes) {
                            // Copy the same entries; do not overwrite if already filled
                            if (!filled[nextTimeIdx][dayIdx]) {
                                grid[nextTimeIdx][dayIdx] = entries;
                                filled[nextTimeIdx][dayIdx] = true;
                            }
                        }
                    }
                }

                dayIdx++;
            }
        }

        // ── 4. Flatten grid to ExtractedSlot list ──────────────────────────
        for (int r = 0; r < numTimes; r++) {
            for (int c = 0; c < numDays; c++) {
                for (RoomEntry entry : grid[r][c]) {
                    if (!entry.roomCode.isEmpty()) {
                        LocalTime slotTime = timeAxis.get(r);
                        if (slotTime.getHour() < 18) {
                            slots.add(new ExtractedSlot(
                                    entry.roomCode,
                                    dayColumns.get(c),
                                    slotTime,
                                    entry.subject,
                                    entry.lecturer
                            ));
                        }
                    }
                }
            }
        }

        return slots;
    }

    // ── Cell room extraction ──────────────────────────────────────────────────

    /**
     * Extract all room entries from a single data cell.
     *
     * <p>Two cases:
     * <ul>
     *   <li><b>Detailed cell</b>: contains a {@code <table class="detailed">}.
     *       Each column of the detail table is a sub-group; the room is in the
     *       last {@code <tr>}'s {@code <td>} for that column.</li>
     *   <li><b>Plain cell</b>: {@code <br/>}-delimited lines; room is the last
     *       non-empty token.</li>
     * </ul>
     */
    private List<RoomEntry> extractRooms(Element cell) {
        if (isFreeCell(cell)) {
            return Collections.emptyList();
        }

        Element detailTable = cell.selectFirst("table.detailed");
        if (detailTable != null) {
            return extractFromDetailedTable(detailTable);
        } else {
            return extractFromPlainCell(cell);
        }
    }

    /**
     * Extract rooms from a {@code <table class="detailed">} cell.
     *
     * <p>Each column in the detail table corresponds to one sub-group.
     * Rows in the detail table are: sub-group, subject, lecturers, room.
     *
     * <p>Algorithm:
     * <ol>
     *   <li>Collect all rows of the detail table.</li>
     *   <li>Determine number of columns from the widest row.</li>
     *   <li>For each column index, gather the text of each row in that column.</li>
     *   <li>Room = last row's text for that column.</li>
     * </ol>
     */
    private List<RoomEntry> extractFromDetailedTable(Element detailTable) {
        List<RoomEntry> results = new ArrayList<>();

        Elements rows = detailTable.select("tr");
        if (rows.isEmpty()) return results;

        // Find max columns
        int maxCols = rows.stream()
                .mapToInt(r -> r.select("td").size())
                .max()
                .orElse(0);

        if (maxCols == 0) return results;

        // For each column, build array of row-texts
        for (int col = 0; col < maxCols; col++) {
            String subject  = "";
            String lecturer = "";
            String room     = "";

            for (int rowNum = 0; rowNum < rows.size(); rowNum++) {
                Elements tds = rows.get(rowNum).select("td");
                String text = (col < tds.size()) ? tds.get(col).text().trim() : "";

                switch (rowNum) {
                    case 0 -> { /* sub-group label – skip */ }
                    case 1 -> subject  = text;
                    case 2 -> lecturer = text;
                    case 3 -> room     = text;
                    default -> {
                        // sometimes there are extra rows; treat last as room
                        if (!text.isEmpty()) room = text;
                    }
                }
            }

            String normalizedRoom = normalizeRoomCode(room);
            if (!normalizedRoom.isEmpty()) {
                results.add(new RoomEntry(normalizedRoom, subject, lecturer));
            }
        }

        return results;
    }

    /**
     * Extract rooms from a plain (non-detailed) occupied cell.
     *
     * <p>Plain cell structure:
     * <pre>
     *   IT1120 - IP Lecture&lt;br/&gt;
     *   Ms. Chathurya Kumarapperuma&lt;br/&gt;
     *   G602&lt;br/&gt;
     * </pre>
     *
     * <p>Room is always the last non-empty {@code <br/>}-split segment.
     */
    private List<RoomEntry> extractFromPlainCell(Element cell) {
        // Use own-text nodes split by <br> instead of .text() which flattens everything
        String html = cell.html();
        // Split on <br> tags (all variants)
        String[] lines = html.split("<br\\s*/?>", -1);

        List<String> tokens = new ArrayList<>();
        for (String raw : lines) {
            // strip remaining tags and entities
            String cleaned = Jsoup.parse(raw).text().trim();
            if (!cleaned.isEmpty()) tokens.add(cleaned);
        }

        if (tokens.isEmpty()) return Collections.emptyList();

        // Room is the last non-empty token
        String roomRaw  = tokens.get(tokens.size() - 1);
        String subject  = tokens.size() > 0 ? tokens.get(0) : "";
        String lecturer = tokens.size() > 1 ? tokens.get(1) : "";

        String normalizedRoom = normalizeRoomCode(roomRaw);
        if (normalizedRoom.isEmpty()) {
            log.trace("Could not extract room from plain cell tokens: {}", tokens);
            return Collections.emptyList();
        }

        return List.of(new RoomEntry(normalizedRoom, subject, lecturer));
    }

    // ── Helper: room code normalisation ──────────────────────────────────────

    /**
     * Normalise a raw room code string.
     *
     * <p>Input examples: {@code "G602"}, {@code "E103 L"}, {@code "A410+A411 lab"},
     * {@code "F305-Pclab"}, {@code "B403-Pclab"}.
     *
     * <p>Strategy: trim, uppercase, keep the primary location token (everything up to
     * an optional descriptive suffix like " lab", " L"). We keep composite rooms like
     * {@code A410+A411} intact so they can be matched if the system uses that code.
     */
    public String normalizeRoomCode(String raw) {
        if (raw == null || raw.isBlank()) return "";

        String trimmed = raw.trim();

        // Remove trailing punctuation / trailing whitespace
        trimmed = trimmed.replaceAll("[.,;]+$", "").trim();

        // If it is a free marker, return empty
        if (FREE_MARKERS.contains(trimmed) || trimmed.matches("-+") || trimmed.matches("x+")) {
            return "";
        }

        // Uppercase for normalisation
        String upper = trimmed.toUpperCase();

        // Extract primary room token: letter(s) followed by digits, optional suffix
        // e.g. "G602", "E103 L" → "E103", "A410+A411 LAB" → "A410+A411", "F305-PCLAB" → "F305"
        java.util.regex.Matcher m = Pattern.compile(
                "([A-Z]\\d{2,4}(?:\\+[A-Z]\\d{2,4})?)"
        ).matcher(upper);

        if (m.find()) {
            return m.group(1);
        }

        // Fallback: return the trimmed upper string if it looks like a short identifier
        if (upper.matches("[A-Z0-9+\\- ]{2,15}")) {
            return upper;
        }

        return "";
    }

    // ── Helper: grid building utilities ──────────────────────────────────────

    /**
     * Parse the day-column headers from the {@code <thead>} of a timetable table.
     *
     * <p>The first {@code <tr>} of thead is the group-name row; the second has
     * {@code <th class="xAxis">Monday</th>} etc.
     */
    private List<DayOfWeek> parseDayHeaders(Element table) {
        List<DayOfWeek> days = new ArrayList<>();
        Elements xAxisHeaders = table.select("thead th.xAxis");
        for (Element th : xAxisHeaders) {
            String text = th.text().trim().toUpperCase();
            for (DayOfWeek d : DayOfWeek.values()) {
                if (text.contains(d.name())) {
                    days.add(d);
                    break;
                }
            }
        }
        return days;
    }

    /**
     * Parse the ordered list of time slots from the {@code yAxis} headers.
     */
    private List<LocalTime> parseTimeAxis(Element table) {
        List<LocalTime> times = new ArrayList<>();
        for (Element th : table.select("th.yAxis")) {
            LocalTime t = parseTime(th.text().trim());
            if (t != null && !times.contains(t)) {
                times.add(t);
            }
        }
        return times;
    }

    private LocalTime parseTime(String text) {
        if (text == null) return null;
        java.util.regex.Matcher m = TIME_PATTERN.matcher(text.trim());
        if (!m.matches()) return null;
        try {
            int h = Integer.parseInt(m.group(1));
            int min = Integer.parseInt(m.group(2));
            return LocalTime.of(h, min);
        } catch (DateTimeParseException | NumberFormatException e) {
            return null;
        }
    }

    private int parseRowspan(Element cell) {
        String attr = cell.attr("rowspan");
        if (attr.isBlank()) return 1;
        try {
            return Math.max(1, Integer.parseInt(attr.trim()));
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    /**
     * A cell is free if its visible text is one of the FREE_MARKERS
     * and it contains no nested detail table.
     */
    private boolean isFreeCell(Element cell) {
        // A cell with a nested table is definitely not free
        if (cell.selectFirst("table.detailed") != null) return false;
        String text = cell.text().trim();
        return FREE_MARKERS.contains(text) || text.matches("-+");
    }

    // ── Conversion helper ──────────────────────────────────────────────────────

    /**
     * Collapse a list of {@link ExtractedSlot}s into the compact occupancy map
     * required by {@link FacilityTimetableService}.
     */
    public Map<String, Map<DayOfWeek, Set<LocalTime>>> buildOccupancyMap(List<ExtractedSlot> slots) {
        Map<String, Map<DayOfWeek, Set<LocalTime>>> occupancy = new HashMap<>();
        for (ExtractedSlot s : slots) {
            occupancy
                    .computeIfAbsent(s.getRoomCode(), k -> new HashMap<>())
                    .computeIfAbsent(s.getDay(), k -> new HashSet<>())
                    .add(s.getTime());
        }
        return occupancy;
    }

    // ── Inner model classes ───────────────────────────────────────────────────

    /** A single room-time-day assignment extracted from the timetable. */
    @Getter
    public static class ExtractedSlot {
        private final String roomCode;
        private final DayOfWeek day;
        private final LocalTime time;
        private final String subject;
        private final String lecturer;

        public ExtractedSlot(String roomCode, DayOfWeek day, LocalTime time,
                             String subject, String lecturer) {
            this.roomCode = roomCode;
            this.day      = day;
            this.time     = time;
            this.subject  = subject;
            this.lecturer = lecturer;
        }

        @Override
        public String toString() {
            return String.format("Slot{room='%s', day=%s, time=%s, subject='%s'}",
                    roomCode, day, time, subject);
        }
    }

    /** Internal model used while building the grid. */
    private static class RoomEntry {
        final String roomCode;
        final String subject;
        final String lecturer;

        RoomEntry(String roomCode, String subject, String lecturer) {
            this.roomCode = roomCode;
            this.subject  = subject;
            this.lecturer = lecturer;
        }
    }
}
