package com.sliitreserve.api.services.facility;

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
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses university FET-generated timetable HTML files.
 * Extracts facility (room) occupancy by day and time slot.
 * Handles rowspan, nested tables, and normalizes room names.
 */
@Service
@Slf4j
public class TimetableParserService {

    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d{1,2}):(\\d{2})");
    private static final Pattern ROOM_NAME_PATTERN = Pattern.compile("[A-Z]\\d{2,4}|[A-Z]{1,3}\\d{1,2}\\d{2}");
    private static final String[] DAY_HEADERS = {"MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"};

    /**
     * Parse timetable from HTML file.
     * Returns: Map<RoomCode, Map<DayOfWeek, Set<LocalTime>>>
     */
    public Map<String, Map<DayOfWeek, Set<LocalTime>>> parseFile(File htmlFile) throws IOException {
        Document doc = Jsoup.parse(htmlFile, "UTF-8");
        return parseDocument(doc);
    }

    /**
     * Parse timetable from HTML string.
     */
    public Map<String, Map<DayOfWeek, Set<LocalTime>>> parseHtml(String htmlContent) {
        Document doc = Jsoup.parse(htmlContent);
        return parseDocument(doc);
    }

    private Map<String, Map<DayOfWeek, Set<LocalTime>>> parseDocument(Document doc) {
        Map<String, Map<DayOfWeek, Set<LocalTime>>> roomOccupancy = new HashMap<>();

        // Find all timetable elements
        Elements tables = doc.select("table");
        log.debug("Found {} tables in document", tables.size());

        for (Element table : tables) {
            Map<String, Map<DayOfWeek, Set<LocalTime>>> extracted = extractFromTable(table);
            mergeOccupancy(roomOccupancy, extracted);
        }

        log.info("Parsed timetable: {} rooms with occupancy data", roomOccupancy.size());
        return roomOccupancy;
    }

    /**
     * Extract occupancy from a single table.
     * Handles: rowspan, nested tables, multiple time slots per cell.
     */
    private Map<String, Map<DayOfWeek, Set<LocalTime>>> extractFromTable(Element table) {
        Map<String, Map<DayOfWeek, Set<LocalTime>>> result = new HashMap<>();

        try {
            Elements rows = table.select("> tbody > tr, > tr");
            if (rows.isEmpty()) {
                return result;
            }

            // Parse header row to map day columns
            Element headerRow = rows.first();
            List<DayOfWeek> dayColumns = parseDayColumns(headerRow);

            if (dayColumns.isEmpty()) {
                log.warn("No day columns found in table header");
                return result;
            }

            // Track rowspan continuations: Map<row, List<cell content for each day>>
            Map<Integer, List<String>> rowspanData = new HashMap<>();

            // Process data rows
            for (int rowIdx = 1; rowIdx < rows.size(); rowIdx++) {
                Element row = rows.get(rowIdx);
                Elements cells = row.select("> td, > th");

                LocalTime timeSlot = null;
                int dayColumnIdx = 0;

                for (int cellIdx = 0; cellIdx < cells.size(); cellIdx++) {
                    Element cell = cells.get(cellIdx);
                    String cellText = cell.text().trim();

                    // First cell usually contains time
                    if (cellIdx == 0) {
                        timeSlot = parseTimeFromCell(cellText);
                        if (timeSlot == null && !cellText.isEmpty()) {
                            log.debug("Could not parse time from cell: {}", cellText);
                        }
                        continue;
                    }

                    // Handle rowspan: skip if content already handled
                    int rowspan = parseRowspan(cell);
                    if (rowspan > 1 && rowspanData.containsKey(rowIdx)) {
                        List<String> stored = rowspanData.get(rowIdx);
                        if (dayColumnIdx < stored.size()) {
                            cellText = stored.get(dayColumnIdx);
                        }
                    }

                    if (dayColumnIdx < dayColumns.size() && timeSlot != null) {
                        DayOfWeek day = dayColumns.get(dayColumnIdx);
                        String roomCode = normalizeRoomCode(cellText);

                        if (!roomCode.isEmpty() && !isFreeSlot(cellText)) {
                            result.computeIfAbsent(roomCode, k -> new HashMap<>())
                                   .computeIfAbsent(day, k -> new HashSet<>())
                                   .add(timeSlot);
                        }
                    }

                    dayColumnIdx++;
                }
            }
        } catch (Exception e) {
            log.error("Error extracting from table: {}", e.getMessage(), e);
        }

        return result;
    }

    /**
     * Parse day columns from header row.
     * Matches headers like "MONDAY", "TUESDAY", etc.
     */
    private List<DayOfWeek> parseDayColumns(Element headerRow) {
        List<DayOfWeek> days = new ArrayList<>();
        Elements headerCells = headerRow.select("> th, > td");

        for (int i = 1; i < headerCells.size(); i++) {
            String headerText = headerCells.get(i).text().trim().toUpperCase();

            for (DayOfWeek day : DayOfWeek.values()) {
                if (headerText.contains(day.name())) {
                    days.add(day);
                    break;
                }
            }
        }

        return days;
    }

    /**
     * Parse time slot from cell (e.g., "08:00", "08:00-09:00").
     * Returns the start time.
     */
    private LocalTime parseTimeFromCell(String cellText) {
        if (cellText == null || cellText.trim().isEmpty()) {
            return null;
        }

        Matcher matcher = TIME_PATTERN.matcher(cellText);
        if (matcher.find()) {
            try {
                int hour = Integer.parseInt(matcher.group(1));
                int minute = Integer.parseInt(matcher.group(2));
                return LocalTime.of(hour, minute);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Extract room code from cell content.
     * Matches patterns like "G1301", "A405", "AB12".
     * Handles cases like "G1301 - Class Name" or nested HTML.
     */
    private String normalizeRoomCode(String cellContent) {
        if (cellContent == null || cellContent.isEmpty() || isFreeSlot(cellContent)) {
            return "";
        }

        // Split by common delimiters
        String[] parts = cellContent.split("[-–—]|\\n|<br>");
        String firstPart = parts[0].trim().toUpperCase();

        // Try primary pattern
        Matcher matcher = ROOM_NAME_PATTERN.matcher(firstPart);
        if (matcher.find()) {
            return matcher.group().toUpperCase();
        }

        // Fallback: take first word that looks like a room code
        String[] words = firstPart.split("\\s+");
        for (String word : words) {
            if (word.matches("[A-Z]\\d+.*")) {
                return word.toUpperCase();
            }
        }

        return "";
    }

    /**
     * Check if cell indicates a free slot (e.g., "-x-", "---", empty).
     */
    private boolean isFreeSlot(String cellText) {
        if (cellText == null || cellText.trim().isEmpty()) {
            return true;
        }

        String normalized = cellText.trim().replaceAll("[\\s<>/]", "");
        return normalized.isEmpty()
                || normalized.equals("-x-")
                || normalized.equals("---")
                || normalized.matches("-+")
                || normalized.equalsIgnoreCase("free");
    }

    /**
     * Parse rowspan attribute from cell.
     * Defaults to 1 if not present or invalid.
     */
    private int parseRowspan(Element cell) {
        String rowspanAttr = cell.attr("rowspan");
        if (rowspanAttr == null || rowspanAttr.trim().isEmpty()) {
            return 1;
        }

        try {
            return Integer.parseInt(rowspanAttr);
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    /**
     * Merge extracted occupancy into main map.
     * Handles duplicates by merging time sets.
     */
    private void mergeOccupancy(
            Map<String, Map<DayOfWeek, Set<LocalTime>>> main,
            Map<String, Map<DayOfWeek, Set<LocalTime>>> extracted) {

        for (var roomEntry : extracted.entrySet()) {
            String roomCode = roomEntry.getKey();
            Map<DayOfWeek, Set<LocalTime>> dayMap = roomEntry.getValue();

            main.computeIfAbsent(roomCode, k -> new HashMap<>())
                    .forEach((day, times) -> {
                        dayMap.computeIfAbsent(day, k -> new HashSet<>()).addAll(times);
                    });

            for (var dayEntry : dayMap.entrySet()) {
                main.get(roomCode).putIfAbsent(dayEntry.getKey(), dayEntry.getValue());
            }
        }
    }

    /**
     * Check if cell contains a room assignment (not empty, not free marker).
     */
    boolean cellContainsRoom(Element cell) {
        return !normalizeRoomCode(cell.text()).isEmpty();
    }
}
