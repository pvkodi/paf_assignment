# Backend — Facilities module (current state)

Summary
- Repository path: `backend/api/src/main/java/com/sliitreserve/api/` (controllers, services, repositories, dto, entities, factories, util)
- Base API path: `/api/v1/facilities` (also exposed as `/api/facilities` in controller)

Controllers and Endpoints

- FacilityController (backend/api/src/main/java/com/sliitreserve/api/controllers/facilities/FacilityController.java)
  - Base: `/api/v1/facilities` and `/api/facilities`
  - GET `/` — listFacilities(page,size)
    - Auth: `isAuthenticated()`
    - Returns: `Page<FacilityResponseDTO>`
    - Calls: `FacilityService.listFacilities(Pageable)`

  - GET `/search` — searchFacilities(type,minCapacity,building,location,status,page,size)
    - Auth: `isAuthenticated()`
    - Query params: `type` (FacilityType), `minCapacity` (Integer), `building`, `location`, `status` (FacilityStatus), `page`, `size`
    - Returns: `Page<FacilityResponseDTO>`
    - Calls: `FacilityService.searchFacilities(...)`

  - GET `/{id}` — getFacilityById(UUID id)
    - Auth: `isAuthenticated()`
    - Returns: `FacilityResponseDTO`
    - Calls: `FacilityService.getFacilityById(UUID)`

  - POST `/` — createFacility(FacilityRequestDTO)
    - Auth: `hasRole('ADMIN')`
    - Body: `FacilityRequestDTO`
    - Returns: `201 Created` with `FacilityResponseDTO`
    - Calls: `FacilityService.createFacility(...)`

  - PUT `/{id}` — updateFacility(UUID id, FacilityRequestDTO)
    - Auth: `hasRole('ADMIN')`
    - Returns: `FacilityResponseDTO`
    - Notes: prevents changing facility type; validates availability times

  - DELETE `/{id}` — markOutOfService(UUID id)
    - Auth: `hasRole('ADMIN')`
    - Action: sets status `OUT_OF_SERVICE`

  - GET `/{id}/utilization` — getFacilityUtilization(id, start, end)
    - Auth: `hasRole('ADMIN')`
    - Query params: `start`, `end` (ISO date-time). Defaults: `end` -> now, `start` -> end - 30 days
    - Returns: `FacilityUtilizationDTO`
    - Calls: `FacilityOptimizationService.getFacilityUtilization(...)`

  - GET `/underutilized` — getUnderutilizedFacilities(end)
    - Auth: `hasRole('ADMIN')`
    - Returns: `List<UnderutilizedFacilityDTO>`
    - Calls: `FacilityOptimizationService.getUnderutilizedFacilities(...)`

  - POST `/suggestions` — suggestAlternativeFacilities(FacilitySuggestionRequestDTO)
    - Auth: `isAuthenticated()`
    - Body: `FacilitySuggestionRequestDTO` (type, capacity, start, end, preferredBuilding)
    - Returns: `List<FacilitySuggestionDTO>`
    - Calls: `FacilityOptimizationService.suggestAlternativeFacilities(...)`

- FacilityHeatmapController (backend/api/src/main/java/com/sliitreserve/api/controllers/facilities/FacilityHeatmapController.java)
  - GET `/{facilityId}/heatmap` — weekly heatmap
    - Roles: `hasAnyRole('USER','LECTURER','FACILITY_MANAGER','ADMIN')`
    - Params: optional `startDate`, `endDate` (ISO date). Defaults: last 30 days
    - Returns: `HeatmapResponse` containing `List<HeatmapCellDTO>`

  - GET `/{facilityId}/heatmap/daily` — daily heatmap for a single date
    - Roles: same as above
    - Returns: `HeatmapResponse` with 24 cells

- FacilityInsightsController (backend/api/src/main/java/com/sliitreserve/api/controllers/facilities/FacilityInsightsController.java)
  - GET `/{facilityId}/insights` — availability & insights
    - Roles: `hasAnyRole('USER','LECTURER','FACILITY_MANAGER','ADMIN')`
    - Returns: `AvailabilityStatusDTO`
    - Calls: `FacilityInsightsService.getFacilityInsights(...)`

Services (primary public methods)

- FacilityService (backend/api/src/main/java/com/sliitreserve/api/services/facility/FacilityService.java)
  - `Page<FacilityResponseDTO> listFacilities(Pageable)`
  - `FacilityResponseDTO getFacilityById(UUID)`
  - `Page<FacilityResponseDTO> searchFacilities(FacilityType type, Integer minCapacity, String building, String location, FacilityStatus status, Pageable pageable)`
  - `FacilityResponseDTO createFacility(FacilityRequestDTO)`
  - `FacilityResponseDTO updateFacility(UUID, FacilityRequestDTO)`
  - `void markOutOfService(UUID)`
  - `boolean isFacilityOperational(UUID, LocalDateTime start, LocalDateTime end)` — checks status, availability window and `MaintenanceIntegrationService` for conflicts
  - `List<Facility> findActiveByType(FacilityType)`
  - `List<Facility> searchFacilities(Boolean active, FacilityType type, Integer minCapacity, String building, String namePattern)`
  - `Facility findByCode(String facilityCode)`

- FacilityOptimizationService (backend/api/src/main/java/com/sliitreserve/api/services/facility/FacilityOptimizationService.java)
  - `FacilityUtilizationDTO getFacilityUtilization(UUID facilityId, LocalDateTime start, LocalDateTime end)`
    - Computes available hours (excludes maintenance), booked hours (calls `BookingIntegrationService.getBookedHours`), utilization percentage
  - `List<UnderutilizedFacilityDTO> getUnderutilizedFacilities(LocalDateTime endInclusive)`
    - Uses threshold (30%) and min available hours (50h) to filter
  - `List<FacilitySuggestionDTO> suggestAlternativeFacilities(FacilitySuggestionRequestDTO request)`
    - Collects same-type active facilities then other active facilities, filters by capacity and operational status, sorts by capacity delta and preferred building
  - Internal helpers: `collectSuggestions`, `resolveConsecutiveUnderutilizedDays`, `calculateTotalAvailableHours`, `validateRange`, `calculateUtilization`, `roundTwoDecimals`

- FacilityHeatmapService (backend/api/src/main/java/com/sliitreserve/api/services/facility/FacilityHeatmapService.java)
  - `List<HeatmapCellDTO> getWeeklyHeatmap(String facilityId, LocalDate startDate, LocalDate endDate)` — returns 168 cells (7×24)
  - `List<HeatmapCellDTO> getDailyHeatmap(String facilityId, LocalDate date)` — 24 cells
  - `Map<String,List<HeatmapCellDTO>> getHeatmapByFacilityType(String facilityType, LocalDate startDate, LocalDate endDate)` — not implemented (throws UnsupportedOperation)
  - Helpers: `getStatusFromUtilization(Integer)`, `getColorFromUtilization(Integer)`

- FacilityInsightsService (backend/api/src/main/java/com/sliitreserve/api/services/facility/FacilityInsightsService.java)
  - `AvailabilityStatusDTO getFacilityInsights(String facilityIdStr)` — builds a rich insight payload using `BookingRepository` and `UtilizationSnapshotRepository`
  - Internal helpers: `calculateStatus`, `calculateMinutesUntilFree`, `getNextBookingTime`, `calculateTrendDirection`, `calculatePercentChange`, `getBestBookingSlots`, `getDayName`

Repositories (selected methods)

- FacilityRepository (backend/api/src/main/java/com/sliitreserve/api/repositories/facility/FacilityRepository.java)
  - `Optional<Facility> findByFacilityCode(String)`
  - `boolean existsByFacilityCode(String)`
  - `List<Facility> findByStatus(FacilityStatus)`
  - `List<Facility> findByType(FacilityType)`
  - `List<Facility> findByTypeAndStatus(FacilityType, FacilityStatus)`
  - `List<Facility> findByBuilding(String)`
  - `List<Facility> findByBuildingAndFloor(String building, String floor)`
  - `List<Facility> findByCapacityGreaterThanEqual(Integer)`
  - `List<Facility> findActiveWithMinCapacity(Integer minCapacity, FacilityStatus status)` — custom @Query
  - `List<Facility> findByNameContainingIgnoreCase(String)`
  - `List<Facility> findActiveByNamePattern(String namePattern, FacilityStatus status)` — custom @Query
  - `List<Facility> findByLocationContainingIgnoreCase(String)`
  - `List<Facility> findAllActiveOrderByCapacity(FacilityStatus status)`
  - `long countByType(FacilityType)`, `countByStatus(FacilityStatus)`, `countByTypeAndStatus(FacilityType, FacilityStatus)`

- UtilizationSnapshotRepository (backend/api/src/main/java/com/sliitreserve/api/repositories/facility/UtilizationSnapshotRepository.java)
  - `List<UtilizationSnapshot> findByFacility_IdAndSnapshotDateBetweenOrderBySnapshotDateAsc(UUID, LocalDate, LocalDate)`
  - `List<UtilizationSnapshot> findUnderutilizedInRange(LocalDate start, LocalDate end)`
  - `Optional<UtilizationSnapshot> findFirstByFacility_IdOrderBySnapshotDateDesc(UUID)`
  - `Integer getAverageUtilization(String facilityId, Integer days)` — native query
  - `Map<String,Integer> getWeeklyHeatmapData(String facilityId)` and overload with date range
  - `Map<String,Integer> getDailyHeatmapData(String facilityId, LocalDate date)`
  - `Optional<Integer> getUtilizationAtTime(String facilityId, LocalDateTime dateTime)`
  - `Integer countUnderutilizedDays(String facilityId, Integer threshold, Integer days)`
  - Additional analytic native queries for timeslots

Entities (core fields and subtype attributes)

- Facility (backend/api/src/main/java/com/sliitreserve/api/entities/facility/Facility.java)
  - Fields: `UUID id`, `String facilityCode`, `String name`, `FacilityType type`, `Integer capacity`, `String location`, `String building`, `String floor`, `FacilityStatus status`, `LocalTime availabilityStart`, `LocalTime availabilityEnd`, `LocalDateTime createdAt`, `LocalDateTime updatedAt`
  - Enums: `FacilityType {LECTURE_HALL, LAB, MEETING_ROOM, AUDITORIUM, EQUIPMENT, SPORTS, SPORTS_FACILITY}`
             `FacilityStatus {ACTIVE, MAINTENANCE, OUT_OF_SERVICE}`
  - Aliases / API accessor methods: `getLocationDescription()`, `getAvailabilityStartTime()`, `getAvailabilityEndTime()`

- Subtypes and notable fields:
  - `LectureHall`: `avEquipment: Set<String>`, `wheelchairAccessible: Boolean`
  - `Lab`: `labType: String`, `softwareList: Set<String>`, `safetyEquipment: Set<String>`
  - `MeetingRoom`: `avEnabled: Boolean`, `cateringAllowed: Boolean`
  - `Auditorium`: `stageType: String`, `soundSystem: String`
  - `Equipment`: `brand: String`, `model: String`, `serialNumber: String`, `maintenanceSchedule: String`
  - `SportsFacility`: `sportsType: String`, `equipmentAvailable: Set<String>`

DTOs (summary of request/response shapes)

- `FacilityRequestDTO` (create/update): `facilityCode`, `name`, `type` (FacilityType), `capacity`, `building`, `floor`, `locationDescription`, `availabilityStartTime` (LocalTime), `availabilityEndTime` (LocalTime), `status` (FacilityStatus), `subtypeAttributes: Map<String,Object>`

- `FacilityResponseDTO`: `id`, `facilityCode`, `name`, `type`, `capacity`, `building`, `floor`, `locationDescription`, `status`, `availabilityStartTime`, `availabilityEndTime`, `subtypeAttributes`, `createdAt`, `updatedAt`

- `FacilitySuggestionRequestDTO`: `type`, `capacity`, `start` (LocalDateTime), `end` (LocalDateTime), `preferredBuilding`
- `FacilitySuggestionDTO`: `facilityId`, `name`, `type`, `capacity`, `building`, `status`, `operational`, `capacityDelta`
- `FacilityUtilizationDTO`: `facilityId`, `rangeStart`, `rangeEnd`, `totalAvailableHours`, `totalBookedHours`, `utilizationPercentage`
- `UnderutilizedFacilityDTO`: `facilityId`, `facilityName`, `utilizationPercentage`, `persistentForSevenDays`, `consecutiveUnderutilizedDays`, `status`
- `HeatmapCellDTO`: `day_of_week`, `day_name`, `hour`, `time_label`, `avg_utilization_percent`, `data_points`, `status`, `color`
- `AvailabilityStatusDTO`: `facility_id`, `facility_name`, `facility_type`, `building`, `capacity`, `current_status`, `spots_available`, `minutes_until_free`, `next_booking_time`, `avg_utilization_30day`, `avg_utilization_7day`, `trend_direction`, `trend_percent_change`, `best_booking_slots`

Integration boundaries and stubs

- `BookingIntegrationService` (interface) — integration boundary used by optimization/analytics
  - `double getBookedHours(UUID facilityId, LocalDateTime start, LocalDateTime end)`
  - `List<BookingDTO> getBookingsForFacility(UUID facilityId, LocalDateTime start, LocalDateTime end)`
  - Stub: `StubBookingIntegrationService` returns zero/empty list (backend/api/src/main/java/com/sliitreserve/api/services/integration/StubBookingIntegrationService.java)

- `MaintenanceIntegrationService` (interface)
  - `boolean isFacilityUnderMaintenance(UUID facilityId, LocalDateTime start, LocalDateTime end)`
  - Stub: `StubMaintenanceIntegrationService` returns `false`

Factories & Mappers

- `FacilityFactory` — `createFacility(FacilityRequestDTO)` and `applySubtypeAttributes(Facility, Map)` (factory defaults and subtype instantiation)
- `FacilityMapper` — `toResponseDTO(Facility)`, `toEntity(FacilityRequestDTO)`, `updateEntity(FacilityRequestDTO, Facility)`, `extractSubtypeAttributes(Facility)`

Repository queries and analytics

- `UtilizationSnapshotRepository` contains native queries for averages and heatmap aggregation used by heatmap/insights/optimization

Tests and contract files (examples)

- Unit/contract tests under `backend/api/src/test/java/.../facility/`:
  - `FacilityServiceTest.java`, `FacilityServiceExpandedTest.java`, `FacilityContractTest.java` (useful to see expected behaviour / contracts)

Notes / Behavioural details

- Availability validation: `availabilityStart` must be before `availabilityEnd` (entity pre-persist/pre-update checks and `FacilityService.validateTimeRange`)
- Facility code generation: `FacilityFactory.resolveFacilityCode(...)` creates a random code when not provided
- Type normalization: `SPORTS_FACILITY` is normalized to `SPORTS` in some paths
- Authorization: controller methods are annotated with `@PreAuthorize` roles (ADMIN for create/update/delete, mixed roles for insights/heatmap)

Important source files (quick links)

- [FacilityController.java](backend/api/src/main/java/com/sliitreserve/api/controllers/facilities/FacilityController.java)
- [FacilityHeatmapController.java](backend/api/src/main/java/com/sliitreserve/api/controllers/facilities/FacilityHeatmapController.java)
- [FacilityInsightsController.java](backend/api/src/main/java/com/sliitreserve/api/controllers/facilities/FacilityInsightsController.java)
- [FacilityService.java](backend/api/src/main/java/com/sliitreserve/api/services/facility/FacilityService.java)
- [FacilityOptimizationService.java](backend/api/src/main/java/com/sliitreserve/api/services/facility/FacilityOptimizationService.java)
- [FacilityHeatmapService.java](backend/api/src/main/java/com/sliitreserve/api/services/facility/FacilityHeatmapService.java)
- [FacilityInsightsService.java](backend/api/src/main/java/com/sliitreserve/api/services/facility/FacilityInsightsService.java)
- [FacilityRepository.java](backend/api/src/main/java/com/sliitreserve/api/repositories/facility/FacilityRepository.java)
- [UtilizationSnapshotRepository.java](backend/api/src/main/java/com/sliitreserve/api/repositories/facility/UtilizationSnapshotRepository.java)
- [Facility.java](backend/api/src/main/java/com/sliitreserve/api/entities/facility/Facility.java)
- [FacilityFactory.java](backend/api/src/main/java/com/sliitreserve/api/factories/FacilityFactory.java)
- [FacilityMapper.java](backend/api/src/main/java/com/sliitreserve/api/util/mapping/FacilityMapper.java)
