# Frontend — Facilities implementation (current state)

Summary
- Repository path: `frontend/src/features/facilities/` (UI components) and `frontend/src/services/` (api client + booking service)
- API base: `apiClient` base URL is `http://localhost:8080/api` (see `frontend/src/services/apiClient.js`) — facilities endpoints are called under `/v1/facilities`

API wrapper functions (frontend/src/features/facilities/api.js)

- `fetchFacilities(params = {})`
  - GET `/api/v1/facilities` (params: page,size, filters)
  - Returns `response.data` (paged or array depending on backend)

- `searchFacilities(params = {})`
  - GET `/api/v1/facilities/search` (filters: type, minCapacity, building, location, status)

- `fetchFacilityById(id)`
  - GET `/api/v1/facilities/{id}`

- `createFacility(payload)`
  - POST `/api/v1/facilities` — payload expected to match `FacilityRequestDTO`

- `updateFacility(id, payload)`
  - PUT `/api/v1/facilities/{id}`

- `markFacilityOutOfService(id)`
  - DELETE `/api/v1/facilities/{id}`

- `fetchFacilityUtilization(id, start, end)`
  - GET `/api/v1/facilities/{id}/utilization` (query params `start`, `end`)

- `fetchUnderutilizedFacilities(end)`
  - GET `/api/v1/facilities/underutilized` (optional `end` param)

- `fetchFacilitySuggestions(payload)`
  - POST `/api/v1/facilities/suggestions` (payload: `type`, `capacity`, `start`, `end`, `preferredBuilding`)

Other frontend service usages
- `frontend/src/services/bookingService.js` also calls:
  - GET `/v1/facilities` (searchFacilities)
  - GET `/v1/facilities/{id}` (getFacilityById)

Key UI components (frontend/src/features/facilities)

- `FacilitySearch.jsx`
  - Provides filters (type, minCapacity, location, building)
  - Uses `fetchFacilities` (default) and `searchFacilities` (when user searches)
  - Handles pagination (expects `content`, `totalElements`, `number`, `size` when paged)
  - Emits `onFacilitySelect` callback when user selects a facility

- `FacilityCard.jsx`
  - Presentational card used across lists (shows name, building, capacity, status, sparkline of utilization)
  - Links to `/facilities/{id}`

- `FacilityDetailsPage.jsx`
  - Uses `fetchFacilityById(id)` to load and render full facility metadata
  - Displays availability window, core info and status

- `FacilitySuggestionsView.jsx`
  - Form to submit suggestion requests (type, capacity, start/end, preferredBuilding)
  - Calls `fetchFacilitySuggestions(payload)` and displays `FacilitySuggestionDTO` results

- `UnderutilizedFacilitiesView.jsx`
  - Calls `fetchUnderutilizedFacilities()` and shows `UnderutilizedFacilityDTO` rows

- `FacilityManagementDashboard.jsx` (admin)
  - CRUD management UI: `fetchFacilities`, `searchFacilities`, `createFacility`, `updateFacility`, `markFacilityOutOfService`
  - Edit/create form maps to `FacilityRequestDTO` shape (availability times saved as `HH:MM:SS` strings)
  - Uses role-checks (`useAuth().hasRole('ADMIN')`) to gate create/update/delete actions

Routes and integration
- `frontend/src/routes/pages.jsx` wires the above components into app routes (`/facilities`, `/facilities/:id`, `/facilities/suggestions`, `/facilities/underutilized` etc.)
- Some analytics components import facilities API (e.g., `frontend/src/features/analytics/UtilizationDashboard.jsx` imports from `../facilities/api`)

API client behavior
- `frontend/src/services/apiClient.js`
  - `axios` instance with `baseURL` set to `VITE_API_URL || http://localhost:8080/api`
  - Request interceptor adds `Authorization: Bearer <token>` from `authService`
  - Response interceptor attempts token refresh on `401` (skips for auth endpoints), otherwise logs out user

Frontend expectations / payload shapes
- Create / Update operations send payloads matching `FacilityRequestDTO` fields (notably `availabilityStartTime` / `availabilityEndTime` are LocalTime strings such as `08:00:00`)
- Paged endpoints are expected to return a page object with `content`, `totalElements`, `number`, `size` (the UI tolerates receiving a plain array too)

Files of interest (quick links)

- [frontend/src/features/facilities/api.js](frontend/src/features/facilities/api.js)
- [frontend/src/features/facilities/FacilitySearch.jsx](frontend/src/features/facilities/FacilitySearch.jsx)
- [frontend/src/features/facilities/FacilityCard.jsx](frontend/src/features/facilities/FacilityCard.jsx)
- [frontend/src/features/facilities/FacilityDetailsPage.jsx](frontend/src/features/facilities/FacilityDetailsPage.jsx)
- [frontend/src/features/facilities/FacilitySuggestionsView.jsx](frontend/src/features/facilities/FacilitySuggestionsView.jsx)
- [frontend/src/features/facilities/UnderutilizedFacilitiesView.jsx](frontend/src/features/facilities/UnderutilizedFacilitiesView.jsx)
- [frontend/src/features/facilities/FacilityManagementDashboard.jsx](frontend/src/features/facilities/FacilityManagementDashboard.jsx)
- [frontend/src/services/apiClient.js](frontend/src/services/apiClient.js)
- [frontend/src/services/bookingService.js](frontend/src/services/bookingService.js)

Notes / next steps you might want
- Verify the contract between backend DTOs and frontend expectations (`FacilityResponseDTO` fields vs UI usage like `location` vs `locationDescription`)
- If you plan to change endpoint prefixes, update `apiClient` and frontend wrapper functions accordingly
- Tests: run backend unit/contract tests under `backend/api/src/test` to validate behavior after any change
