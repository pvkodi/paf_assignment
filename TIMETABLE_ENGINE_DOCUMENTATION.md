# Timetable Parsing & Availability Engine

## Overview

This feature extends your Facilities module with a **timetable parsing and availability engine** that:

- **Parses** university FET-generated timetable HTML files
- **Extracts** facility occupancy data (room, day, time)
- **Caches** data in-memory for fast queries
- **Integrates** with existing `FacilityService` to enhance availability checks
- **Provides** a new API endpoint for timetable queries

---

## 🎯 What's New

### Services

#### 1. **TimetableParserService**
**Location:** `services/facility/TimetableParserService.java`

Parses HTML timetables and extracts room occupancy.

**Key Methods:**
```java
Map<String, Map<DayOfWeek, Set<LocalTime>>> parseFile(File htmlFile)
Map<String, Map<DayOfWeek, Set<LocalTime>>> parseHtml(String htmlContent)
```

**Handles:**
- Multiple timetable tables in one document
- Rowspan expansion (multi-hour sessions)
- Nested tables
- Room code normalization (`g1301` → `G1301`)
- Free slot markers (`-x-`, `---`, empty)
- Complex room descriptions (`G1301 - Data Structures (Lec 01)`)

#### 2. **FacilityTimetableService**
**Location:** `services/facility/FacilityTimetableService.java`

Manages timetable cache and provides availability queries.

**Key Methods:**
```java
void loadTimetable(File timetableFile, boolean forceReload)
void loadTimetableFromHtml(String htmlContent, boolean forceReload)

// Query occupancy
boolean isOccupied(String facilityCode, DayOfWeek day, LocalTime time)
Set<LocalTime> getOccupiedSlots(String facilityCode, DayOfWeek day)
Set<LocalTime> getAvailableSlots(String facilityCode, DayOfWeek day)
Map<DayOfWeek, Set<LocalTime>> getFacilityOccupancy(String facilityCode)

// Cache management
void clearCache()
boolean isTimetableLoaded()
Map<String, Object> getStatistics()
```

### Enhanced Services

#### **FacilityService.isFacilityOperational()**

**Before (without timetable):**
```java
boolean isOperational = ...
  && status != OUT_OF_SERVICE
  && status != MAINTENANCE
  && timeWithinWindow
  && !underMaintenance
```

**After (with timetable):** ✅ NEW
```java
boolean isOperational = ...
  && status != OUT_OF_SERVICE
  && status != MAINTENANCE
  && timeWithinWindow
  && !underMaintenance
  && !occupiedInTimetable   // ⭐ NEW!
```

### New API Endpoint

**Endpoint:** `GET /api/v1/facilities/{id}/timetable-availability?day=MONDAY`

**Auth:** `isAuthenticated()`

**Parameters:**
- `day` (optional): DayOfWeek (MONDAY, TUESDAY, ..., SUNDAY). Default: MONDAY

**Response:**
```json
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
```

### DTOs

**TimetableAvailabilityDTO**
```java
{
  facility_code: String,
  facility_name: String,
  day: DayOfWeek,
  occupied_slots: Set<String>,
  available_slots: Set<String>,
  total_available_count: Integer,
  total_occupied_count: Integer,
  timetable_loaded: Boolean
}
```

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────┐
│     FacilityController                  │
│  GET /facilities/{id}/timetable-...    │
└────────────────┬────────────────────────┘
                 │
┌────────────────▼────────────────────────┐
│     FacilityService                     │
│  - listFacilities()                     │
│  - isFacilityOperational() ← ENHANCED   │
│  - ...                                  │
└────────────────┬────────────────────────┘
                 │
┌────────────────▼────────────────────────┐
│  FacilityTimetableService               │
│  - loadTimetable()                      │
│  - isOccupied()                         │
│  - getOccupiedSlots()                   │
│  - getAvailableSlots()                  │
│  In-memory cache (ConcurrentHashMap)    │
└────────────────┬────────────────────────┘
                 │
┌────────────────▼────────────────────────┐
│  TimetableParserService                 │
│  - parseFile()                          │
│  - parseHtml()                          │
│  Uses JSoup for HTML parsing            │
└────────────────┬────────────────────────┘
                 │
┌────────────────▼────────────────────────┐
│  In-Memory Cache                        │
│  Map<RoomCode, Map<Day, Set<Time>>>     │
└─────────────────────────────────────────┘
```

### Design Principles

1. **Non-invasive:** Only adds new methods to existing services, doesn't modify existing logic (except `isFacilityOperational`)
2. **Clean separation:** Timetable is a distinct concern, isolated in its own service
3. **Performance:** In-memory cache for O(1) lookups, no database overhead
4. **Stateless requests:** Each API request is independent
5. **Defensive copies:** Service returns defensive copies to prevent cache pollution

---

## 📋 HTML Format Reference

The parser expects **FET-generated HTML** timetables:

```html
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
    <td>G1301 - Data Structures (Lec 01)</td>
    <td>-x-</td>
    <td colspan="2">A405 - Advanced OOP</td>
    <td>-x-</td>
    <td>-x-</td>
    <td>-x-</td>
  </tr>
  <tr>
    <td>09:00</td>
    <td rowspan="2">G1301</td>
    <td>-x-</td>
    <td>-x-</td>
    <td>Lab G1310</td>
    <td>-x-</td>
    <td>-x-</td>
    <td>-x-</td>
  </tr>
</table>
```

### Parser Capabilities

| Feature | Example | Status |
|---------|---------|--------|
| Time slots | 08:00, 09:00-10:00 | ✅ |
| Room codes | G1301, A405, AB12 | ✅ |
| Free markers | -x-, ---, empty cells | ✅ |
| Room + description | G1301 - Data Structures | ✅ |
| Rowspan (multi-hour) | `<td rowspan="2">G1301</td>` | ✅ |
| Colspan | `<td colspan="2">G1301</td>` | ✅ |
| Multiple tables | Both in one HTML | ✅ |
| Nested tables | Outer table parsed | ✅ |
| Whitespace | "  G1301  " → "G1301" | ✅ |
| Case insensitivity | g1301, G1301, G1301 | ✅ |

### Room Code Patterns

Supported patterns (auto-detected via regex):
- `G1301` — Letter + 4 digits
- `A405` — Letter + 3 digits
- `AB12` — 2 Letters + 2 digits
- `C1234` — Letter + 4 digits

---

## 🚀 Usage Examples

### 1. Load Timetable (One-time setup)

```java
@Component
public class TimetableBootstrap {
    
    @Autowired
    FacilityTimetableService timetableService;

    @PostConstruct
    public void init() throws IOException {
        // Load on startup
        File timetableFile = new File("/conf/timetable.html");
        timetableService.loadTimetable(timetableFile, false);
    }
}
```

### 2. Check Occupancy

```java
// Is G1301 occupied on Monday at 08:00?
boolean occupied = timetableService.isOccupied(
    "G1301", 
    DayOfWeek.MONDAY, 
    LocalTime.of(8, 0)
);

if (occupied) {
    // Reject booking
} else {
    // Allow booking
}
```

### 3. Get Available Slots

```java
Set<LocalTime> available = timetableService.getAvailableSlots(
    "G1301",
    DayOfWeek.WEDNESDAY
);

// Returns: {11:00, 12:00, 13:00, 14:00, 15:00, 16:00}
// (assuming availability window is 08:00-17:00)
```

### 4. Query API Endpoint

```bash
# Get timetable availability for facility on a day
curl -X GET \
  "http://localhost:8080/api/v1/facilities/550e8400-e29b-41d4-a716-446655440000/timetable-availability?day=MONDAY" \
  -H "Authorization: Bearer $TOKEN"

# Response:
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
```

### 5. Reload Timetable (Admin)

```java
// Reload when new timetable is available
File newTimetableFile = new File("/conf/timetable_semester2.html");
timetableService.loadTimetable(newTimetableFile, true); // force reload
```

### 6. Integration with Booking Service

```java
// When validating a booking request
LocalDateTime bookingStart = LocalDateTime.of(2026, 4, 20, 14, 0);
LocalDateTime bookingEnd = LocalDateTime.of(2026, 4, 20, 15, 0);

// This now includes timetable check!
boolean canBook = facilityService.isFacilityOperational(
    facilityId,
    bookingStart,
    bookingEnd
);

if (canBook) {
    // Create booking
} else {
    // Return error: "Facility not available (occupied in timetable)"
}
```

---

## 🧪 Testing

### Unit Tests

**TimetableParserServiceTest**
- Simple timetable parsing
- Room code normalization
- Free slot detection
- Rowspan expansion
- Multiple tables
- All days of week
- Time range parsing
- Nested tables
- Malformed HTML handling

**FacilityTimetableServiceTest**
- Cache loading and reloading
- Occupancy checks
- Available/occupied slot queries
- Case-insensitive matching
- Statistics gathering
- Cache clearing
- Facility occupancy mapping

Run tests:
```bash
mvn test -Dtest=TimetableParserServiceTest
mvn test -Dtest=FacilityTimetableServiceTest
```

### Integration Example

See: `src/main/java/com/sliitreserve/api/examples/TimetableIntegrationExamples.java`

Contains 10 practical examples covering:
- Loading timetables
- Checking occupancy
- Querying available/occupied slots
- Using with FacilityService
- API usage patterns
- Troubleshooting

---

## 📊 Rowspan Handling (Deep Dive)

**HTML Input:**
```html
<tr>
  <td>08:00</td>
  <td rowspan="2">G1301</td>  <!-- Multi-hour session -->
</tr>
<tr>
  <td>09:00</td>
  <!-- Implicit: G1301 continues here -->
</tr>
```

**Parser Logic:**
1. Parse first row: time=08:00, room=G1301 → add (G1301, MON, 08:00)
2. Detect rowspan=2
3. Apply to next row: time=09:00 → add (G1301, MON, 09:00)
4. Result: G1301 marked as occupied from 08:00-10:00 (two 1-hour slots)

**Output:**
```
G1301:
  MONDAY: {08:00, 09:00}
```

---

## ⚙️ Performance Considerations

### Memory Usage

**Example:** 1000 rooms, 5 slots/room on average
- ~5000 LocalTime objects in memory
- ~20-30 KB total (negligible)

### Query Performance

- **isOccupied():** O(1) — map lookup + set contains
- **getOccupiedSlots():** O(1) — map lookup
- **getAvailableSlots():** O(n) — where n = availability window hours (typically 9-10)

### Cache Strategy

- **Load:** Once on startup or admin request
- **TTL:** None (cache until manually cleared or app restarts)
- **Invalidation:** Manual `clearCache()` + reload

---

## 🔄 Integration Flow

### Booking Request Flow

```
1. User submits booking request
   ↓
2. Controller receives request
   ↓
3. FacilityService.isFacilityOperational(...) called
   ├─ Check: Facility status
   ├─ Check: Availability window
   ├─ Check: Maintenance conflicts
   └─ ⭐ Check: Timetable occupancy (NEW)
   ↓
4a. If operational → Create booking ✅
4b. If not operational → Return error ❌
```

### Timetable Check Within isFacilityOperational()

```java
// Iterate through each hour in the booking request
for (LocalDateTime hour : bookingTimeRange) {
    DayOfWeek day = hour.getDayOfWeek();
    LocalTime time = hour.toLocalTime();
    
    if (timetableService.isOccupied(facilityCode, day, time)) {
        return false; // Facility occupied in timetable
    }
}
return true; // All hours are free
```

---

## 🛠️ Troubleshooting

### Issue: Timetable not loading

**Check:**
```java
boolean loaded = timetableService.isTimetableLoaded();
Map<String, Object> stats = timetableService.getStatistics();

System.out.println("Timetable loaded: " + loaded);
System.out.println("Rooms: " + stats.get("roomsCount"));
System.out.println("Total slots: " + stats.get("totalOccupiedSlots"));
```

**Solutions:**
1. Check file path is correct
2. Verify HTML is well-formed
3. Check for JSoup parsing exceptions in logs
4. Try simpler HTML first to isolate issue

### Issue: Room codes not matching

**Check:**
- Is room code uppercase? (Parser normalizes to uppercase)
- Does facility code in DB match timetable room code?
- Is room code format valid? (must match regex: `[A-Z]\d{2,}`)

**Debug:**
```java
// Get all rooms in timetable
var occupancy = timetableService.getFacilityOccupancy("G1301");
System.out.println("Occupancy for G1301: " + occupancy);

// Check if occupied at specific time
boolean occupied = timetableService.isOccupied("G1301", DayOfWeek.MONDAY, LocalTime.of(8, 0));
System.out.println("Occupied: " + occupied);
```

### Issue: Performance degradation

**Check:**
- How many timetable entries? (run `getStatistics()`)
- Is cache being cleared repeatedly?
- Are there memory leaks? (check for repeated loads without clear)

**Solution:**
```java
// Clear and reload if corrupted
timetableService.clearCache();
File timetableFile = new File("/path/to/timetable.html");
timetableService.loadTimetable(timetableFile, true);
```

---

## 📦 Files Created

### Source Code
- `services/facility/TimetableParserService.java` — HTML parser
- `services/facility/FacilityTimetableService.java` — Cache & queries
- `dto/facility/TimetableAvailabilityDTO.java` — Response DTO
- `examples/TimetableIntegrationExamples.java` — 10 usage examples

### Tests
- `services/facility/TimetableParserServiceTest.java` — Parser tests (11 test cases)
- `services/facility/FacilityTimetableServiceTest.java` — Service tests (11 test cases)

### Changes to Existing Code
- `controllers/facilities/FacilityController.java` — Added new endpoint
- `services/facility/FacilityService.java` — Enhanced `isFacilityOperational()`

---

## 🔐 Security

- **Authorization:** New endpoint uses `@PreAuthorize("isAuthenticated()")` — any authenticated user can view timetable
- **No sensitive data:** Timetable contains only room codes and times
- **No write operations:** Timetable is read-only from API
- **Cache isolation:** Each request gets defensive copies

---

## 📝 Next Steps

1. **Add timetable upload endpoint:**
   - `POST /api/v1/admin/timetable` — upload new timetable HTML file
   - Admin-only, calls `timetableService.loadTimetableFromHtml(..., true)`

2. **Add timetable statistics endpoint:**
   - `GET /api/v1/admin/timetable/stats` — view cache stats

3. **Add cron job:**
   - Periodic timetable refresh from file/external source

4. **UI integration:**
   - Display occupancy calendar using `/timetable-availability` endpoint
   - Show green/red blocks for available/occupied slots

5. **Advanced queries:**
   - Find best time slots across multiple days
   - Conflict detection for class rescheduling

---

## 📚 Dependencies

None new! Uses only:
- **JSoup** (already likely in your project for HTML parsing)
- **Spring Framework** (existing)
- **Java Time API** (built-in)

If JSoup is not present, add to `pom.xml`:
```xml
<dependency>
    <groupId>org.jsoup</groupId>
    <artifactId>jsoup</artifactId>
    <version>1.15.3</version>
</dependency>
```

---

## ✅ Summary

This timetable engine:
- ✅ Parses university FET timetables cleanly
- ✅ Handles rowspan, nested tables, complex descriptions
- ✅ Integrates seamlessly with existing `FacilityService`
- ✅ Returns smart availability queries
- ✅ Provides new API endpoint
- ✅ Uses efficient in-memory caching
- ✅ Includes comprehensive tests
- ✅ Production-ready, non-invasive architecture
- ✅ Well-documented with 10 usage examples

