package com.sliitreserve.api.services.calendar;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.sliitreserve.api.config.TimePolicyConfig.TimePolicyProperties;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

/**
 * Public Holiday Service.
 * 
 * Manages institutional public holidays and provides query methods for:
 * - Detecting public holidays in recurring booking recurrence evaluation
 * - Skipping occurrences on public holidays (FR-010)
 * - Notifying requesters when occurrences are skipped (FR-011)
 * - Supporting admin-on-behalf bookings (FR-012)
 * - Timezone-aware checks in campus local timezone
 * 
 * Design:
 * - In-memory holiday store with configurable list
 * - Supports fixed-date holidays (e.g., New Year: Jan 1)
 * - Extensible for computed holidays (Easter, Eid, etc.)
 * - Campus timezone-aware for DST consistency
 * 
 * Integration Points:
 * - BookingService: Checks holidays during recurrence skip logic
 * - RecurrenceBuilder: Skips holiday occurrences in booking series
 * - NotificationService: Sends holiday skip notification to requester
 * - UI/Frontend: Displays holiday indicators on facility calendars
 * 
 * Future Extensions:
 * - Persistent storage in database via PublicHolidayRepository
 * - Support for moveable holidays (Easter, Eid)
 * - Per-facility holiday overrides (some facilities open on holidays)
 * - Public API for calendar integration
 */
@Slf4j
@Service
public class PublicHolidayService {

    private final ZoneId campusZoneId;
    private final Set<LocalDate> publicHolidays;

    public PublicHolidayService(TimePolicyProperties timePolicyProperties) {
        this.campusZoneId = timePolicyProperties.campusZoneId;
        this.publicHolidays = new TreeSet<>(initializePublicHolidays());
        log.info("Public Holiday Service initialized with {} holidays in timezone: {}", 
            publicHolidays.size(), campusZoneId);
    }

    /**
     * Check if a given date is a public holiday in campus timezone.
     * 
     * @param date The local date to check
     * @return true if date is a public holiday, false otherwise
     */
    public boolean isPublicHoliday(LocalDate date) {
        return publicHolidays.contains(date);
    }

    /**
     * Check if a given instant (with timezone) is a public holiday in campus timezone.
     * Converts the instant to campus local date before checking.
     * 
     * @param zonedDateTime The instant to check (with timezone)
     * @return true if the date in campus timezone is a public holiday, false otherwise
     */
    public boolean isPublicHolidayInCampusTimezone(ZonedDateTime zonedDateTime) {
        LocalDate dateInCampusTimezone = zonedDateTime.withZoneSameInstant(campusZoneId).toLocalDate();
        return isPublicHoliday(dateInCampusTimezone);
    }

    /**
     * Get all upcoming public holidays from a given date (inclusive).
     * Useful for UI calendars, notifications, and planning.
     * 
     * @param from Start date (inclusive)
     * @param limit Maximum number of holidays to return
     * @return List of upcoming public holidays
     */
    public List<LocalDate> getUpcomingPublicHolidays(LocalDate from, int limit) {
        return publicHolidays.stream()
            .filter(date -> !date.isBefore(from))
            .limit(limit)
            .toList();
    }

    /**
     * Get all public holidays between two dates (inclusive).
     * Used for batch operations, reporting, and recurrence evaluation.
     * 
     * @param from Start date (inclusive)
     * @param to End date (inclusive)
     * @return List of public holidays within range
     */
    public List<LocalDate> getPublicHolidaysBetween(LocalDate from, LocalDate to) {
        return publicHolidays.stream()
            .filter(date -> !date.isBefore(from) && !date.isAfter(to))
            .toList();
    }

    /**
     * Get all configured public holidays.
     * Returns a copy to prevent external mutation.
     * 
     * @return Sorted list of all public holidays
     */
    public List<LocalDate> getAllPublicHolidays() {
        return new ArrayList<>(publicHolidays);
    }

    /**
     * Reload public holidays from configuration or database.
     * Useful for runtime updates or admin configuration changes.
     * Currently resets to default holiday list.
     * 
     * Future: Load from database PublicHolidayRepository
     */
    public void reloadPublicHolidays() {
        log.info("Reloading public holidays");
        publicHolidays.clear();
        publicHolidays.addAll(initializePublicHolidays());
        log.info("Public holidays reloaded: {} total holidays", publicHolidays.size());
    }

    /**
     * Initialize default public holidays for the institution.
     * 
     * Currently hardcoded for Sri Lankan public holidays.
     * Future: Load from configuration, database, or external calendar service.
     * 
     * Sri Lankan Public Holidays (sample):
     * - Independence Day: February 4
     * - Full Moon Poya Day: Monthly (lunar calendar)
     * - New Year: January 1 and April 14
     * - Religious holidays: Sinhala/Tamil New Year, Vesak, Eid, Christmas
     * 
     * @return Set of public holiday dates for 2024-2025
     */
    private Set<LocalDate> initializePublicHolidays() {
        Set<LocalDate> holidays = new TreeSet<>();

        // 2024 Sri Lankan Public Holidays
        holidays.add(LocalDate.of(2024, 1, 14));  // Thai Pongal
        holidays.add(LocalDate.of(2024, 2, 4));   // Independence Day
        holidays.add(LocalDate.of(2024, 2, 5));   // Independence Day Observed
        holidays.add(LocalDate.of(2024, 3, 8));   // Maha Shivaratri
        holidays.add(LocalDate.of(2024, 3, 25));  // Full Moon Poya (Medin)
        holidays.add(LocalDate.of(2024, 4, 11));  // Eid-ul-Fitr
        holidays.add(LocalDate.of(2024, 4, 12));  // Eid-ul-Fitr
        holidays.add(LocalDate.of(2024, 4, 13));  // Sinhala & Tamil New Year
        holidays.add(LocalDate.of(2024, 4, 14));  // Sinhala & Tamil New Year
        holidays.add(LocalDate.of(2024, 4, 25));  // Full Moon Poya (Bak)
        holidays.add(LocalDate.of(2024, 5, 1));   // May Day
        holidays.add(LocalDate.of(2024, 5, 23));  // Vesak Poya Day
        holidays.add(LocalDate.of(2024, 5, 24));  // Vesak Poya Day Observed
        holidays.add(LocalDate.of(2024, 6, 17));  // Eid-ul-Adha
        holidays.add(LocalDate.of(2024, 6, 18));  // Eid-ul-Adha
        holidays.add(LocalDate.of(2024, 6, 23));  // Full Moon Poya (Asal)
        holidays.add(LocalDate.of(2024, 7, 22));  // Full Moon Poya (Esala)
        holidays.add(LocalDate.of(2024, 8, 15));  // Independence Day (Colombo)
        holidays.add(LocalDate.of(2024, 8, 20));  // Full Moon Poya (Nikini)
        holidays.add(LocalDate.of(2024, 9, 18));  // Full Moon Poya (Binara)
        holidays.add(LocalDate.of(2024, 10, 17)); // Full Moon Poya (Vap)
        holidays.add(LocalDate.of(2024, 10, 31)); // Deepavali
        holidays.add(LocalDate.of(2024, 11, 1));  // Halloewee
        holidays.add(LocalDate.of(2024, 11, 15)); // Full Moon Poya (Il)
        holidays.add(LocalDate.of(2024, 12, 15)); // Full Moon Poya (Unduwap)
        holidays.add(LocalDate.of(2024, 12, 25)); // Christmas

        // 2025 Sri Lankan Public Holidays
        holidays.add(LocalDate.of(2025, 1, 1));   // New Year's Day
        holidays.add(LocalDate.of(2025, 1, 14));  // Thai Pongal
        holidays.add(LocalDate.of(2025, 2, 4));   // Independence Day
        holidays.add(LocalDate.of(2025, 2, 5));   // Independence Day Observed
        holidays.add(LocalDate.of(2025, 3, 8));   // Full Moon Poya (Medin)
        holidays.add(LocalDate.of(2025, 3, 29));  // Good Friday
        holidays.add(LocalDate.of(2025, 4, 1));   // Eid-ul-Fitr
        holidays.add(LocalDate.of(2025, 4, 2));   // Eid-ul-Fitr
        holidays.add(LocalDate.of(2025, 4, 13));  // Sinhala & Tamil New Year
        holidays.add(LocalDate.of(2025, 4, 14));  // Sinhala & Tamil New Year
        holidays.add(LocalDate.of(2025, 5, 1));   // May Day
        holidays.add(LocalDate.of(2025, 5, 12));  // Full Moon Poya (Bak)
        holidays.add(LocalDate.of(2025, 5, 13));  // Vesak Poya Day
        holidays.add(LocalDate.of(2025, 6, 7));   // Eid-ul-Adha
        holidays.add(LocalDate.of(2025, 6, 8));   // Eid-ul-Adha
        holidays.add(LocalDate.of(2025, 6, 11));  // Full Moon Poya (Asal)
        holidays.add(LocalDate.of(2025, 7, 10));  // Full Moon Poya (Esala)
        holidays.add(LocalDate.of(2025, 8, 15));  // Independence Day (Colombo)
        holidays.add(LocalDate.of(2025, 8, 9));   // Full Moon Poya (Nikini)
        holidays.add(LocalDate.of(2025, 9, 7));   // Full Moon Poya (Binara)
        holidays.add(LocalDate.of(2025, 10, 6));  // Full Moon Poya (Vap)
        holidays.add(LocalDate.of(2025, 10, 31)); // Deepavali
        holidays.add(LocalDate.of(2025, 11, 4));  // Full Moon Poya (Il)
        holidays.add(LocalDate.of(2025, 11, 5));  // Halloewee
        holidays.add(LocalDate.of(2025, 12, 3));  // Full Moon Poya (Unduwap)
        holidays.add(LocalDate.of(2025, 12, 25)); // Christmas

        log.info("Initialized {} public holidays", holidays.size());
        return holidays;
    }

    /**
     * Add a public holiday programmatically.
     * Used for runtime configuration or experimental features.
     * 
     * @param date The date to add as a public holiday
     * @return true if added (was not already present), false if already existed
     */
    public boolean addPublicHoliday(LocalDate date) {
        boolean added = publicHolidays.add(date);
        if (added) {
            log.info("Public holiday added: {}", date);
        }
        return added;
    }

    /**
     * Remove a public holiday programmatically.
     * Used for runtime configuration or error correction.
     * 
     * @param date The date to remove from public holidays
     * @return true if removed (was present), false if not found
     */
    public boolean removePublicHoliday(LocalDate date) {
        boolean removed = publicHolidays.remove(date);
        if (removed) {
            log.info("Public holiday removed: {}", date);
        }
        return removed;
    }

    /**
     * Get the count of public holidays.
     * @return Number of configured public holidays
     */
    public int getPublicHolidayCount() {
        return publicHolidays.size();
    }
}
