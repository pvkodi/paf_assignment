# Frontend Project Overview - Smart Campus Operations Hub

**Document Date:** April 18, 2026  
**Focus:** Frontend Architecture, Features, and Implementation Status

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [Frontend Architecture](#frontend-architecture)
3. [Technology Stack](#technology-stack)
4. [Feature Modules](#feature-modules)
5. [Routes & Navigation](#routes--navigation)
6. [Authentication & Security](#authentication--security)
7. [State Management](#state-management)
8. [Services & API Integration](#services--api-integration)
9. [Component Structure](#component-structure)
10. [Build & Deployment](#build--deployment)
11. [Development Guidelines](#development-guidelines)

---

## Executive Summary

The frontend is a modern React 19 SPA (Single Page Application) built with Vite, featuring role-based access control, OAuth authentication, and comprehensive facility management capabilities.

### Key Statistics
- **Frontend Features:** 9 distinct feature modules
- **Routes:** 20+ protected and public routes
- **Components:** 30+ reusable feature components
- **Build Tool:** Vite (fast dev/build)
- **Styling:** Tailwind CSS v4
- **State Management:** React Context API
- **HTTP Client:** Axios with interceptors
- **Deployment:** Docker containerized

### What's Been Built
✅ Complete authentication system (OAuth + email/password)  
✅ Facility booking management with QR codes  
✅ Admin approval workflows  
✅ Facility search & recommendations  
✅ Ticket management system  
✅ Notification center  
✅ Suspension appeals system  
✅ Analytics & utilization dashboard  
✅ User management (admin)  
✅ Role-based access control  

---

## Frontend Architecture

### Application Structure

```
frontend/
├── src/
│   ├── App.jsx                    # Route definitions & main entry
│   ├── main.jsx                   # React DOM render
│   ├── index.css                  # Global Tailwind styles
│   ├── App.css                    # Global component styles
│   │
│   ├── contexts/
│   │   └── AuthContext.jsx        # Global authentication context
│   │
│   ├── app/
│   │   └── AppShell.jsx           # Layout wrapper (nav, header)
│   │
│   ├── features/                  # Feature-based modules (9 features)
│   │   ├── auth/
│   │   ├── bookings/
│   │   ├── tickets/
│   │   ├── facilities/
│   │   ├── notifications/
│   │   ├── approvals/
│   │   ├── appeals/
│   │   ├── analytics/
│   │   └── admin/
│   │
│   ├── pages/
│   │   ├── Dashboard.jsx
│   │   └── RegistrationPendingPage.jsx
│   │
│   ├── routes/
│   │   ├── pages.jsx              # All route definitions
│   │   └── ProtectedRoute.jsx     # Route protection & RBAC
│   │
│   ├── services/
│   │   ├── apiClient.js           # Axios HTTP client
│   │   ├── authService.js         # OAuth & auth logic
│   │   └── bookingService.js      # Booking API calls
│   │
│   ├── utils/
│   │   └── bookingUtils.js        # Helper functions
│   │
│   └── assets/                    # Static files
│
├── public/                        # Static public files
├── vite.config.js                 # Vite configuration
├── tailwind.config.js             # Tailwind configuration
├── package.json                   # Dependencies & scripts
└── Dockerfile                     # Docker image definition
```

### Design Patterns

**Feature-Based Organization**
- Each feature is self-contained in `/features/[featureName]/`
- Improves scalability and maintainability
- Reduces merge conflicts in team environments

**Service Layer**
- All API calls centralized in `/services/`
- `apiClient.js` provides common HTTP handling
- Authentication (`authService.js`) handles token management
- Business logic (`bookingService.js`) for domain operations

**Context-Based State**
- Global authentication state via `AuthContext`
- `useAuth()` hook provides auth anywhere
- Token management handled automatically
- User profile cached for performance

**Protected Routes**
- `ProtectedRoute` component enforces:
  - Authentication checks
  - Role-based access control
  - Suspension status validation

---

## Technology Stack

### Core Libraries

| Package | Version | Purpose |
|---------|---------|---------|
| `react` | 19.2.4 | UI framework |
| `react-dom` | 19.2.4 | DOM rendering |
| `react-router-dom` | 7.9.6 | Client-side routing |
| `axios` | 1.12.2 | HTTP requests |
| `@react-oauth/google` | 0.12.1 | Google OAuth |

### UI & Styling

| Package | Version | Purpose |
|---------|---------|---------|
| `tailwindcss` | 4.2.2 | Utility-first CSS |
| `@tailwindcss/vite` | 4.2.2 | Tailwind + Vite integration |
| `react-toastify` | 11.0.5 | Toast notifications |

### Features & Utilities

| Package | Version | Purpose |
|---------|---------|---------|
| `qrcode` | 1.5.4 | QR code generation |
| `qrcode.react` | 4.2.0 | React QR component |
| `jspdf` | 4.2.1 | PDF generation |
| `html2canvas` | 1.4.1 | HTML to canvas |

### Development Tools

| Package | Version | Purpose |
|---------|---------|---------|
| `vite` | 8.0.1 | Build tool & dev server |
| `@vitejs/plugin-react` | 6.0.1 | React HMR plugin |
| `eslint` | 9.39.4 | Code linting |

### Build Scripts

```json
{
  "scripts": {
    "dev": "vite",                  # Start dev server (port 5173)
    "build": "vite build",          # Production build
    "lint": "eslint .",             # Run ESLint
    "preview": "vite preview"       # Preview production build
  }
}
```

---

## Feature Modules

### 1. Auth Feature (`/features/auth/`)

**Purpose:** OAuth login with Google and email/password authentication

**Components:**
- Login UI with OAuth button
- OAuth callback handler
- Token exchange with backend
- Session management

**Key Flows:**
1. User clicks "Login with Google"
2. Google OAuth popup appears
3. After consent, callback handler exchanges token
4. Backend returns JWT + refresh token
5. Tokens stored in localStorage
6. User redirected to dashboard

**Implementation Status:** ✅ Complete

---

### 2. Bookings Feature (`/features/bookings/`) - PRIMARY FEATURE

**Purpose:** Comprehensive facility booking management

**Components:**
| Component | Purpose |
|-----------|---------|
| `BookingForm.jsx` | Create/edit bookings with validation |
| `BookingsList.jsx` | User's personal bookings list |
| `AdminBookingsView.jsx` | Admin view of all bookings |
| `BookingApprovalQueue.jsx` | Approval workflow for lecturers/facility managers |
| `BookingDetails.jsx` | Full booking details modal |
| `BookingCalendarView.jsx` | Calendar-based booking view |
| `CheckInComponent.jsx` | Check-in functionality |
| `QRCodeGenerator.jsx` | Generate QR codes for bookings |
| `QRCodePdfModal.jsx` | Export bookings as PDF with QR codes |
| `QuickCheckInPage.jsx` | Fast QR-based check-in |
| `RecurrenceSelector.jsx` | Setup recurring bookings |
| `TimeslotPicker.jsx` | Time selection interface |
| `QuotaPolicySummary.jsx` | Display quota policies |
| `BookingRecommendations.jsx` | Recommend suitable facilities |
| `AdminBookForUserSelector.jsx` | Admin booking on behalf of users |

**Key Features:**
- Create bookings with date/time/facility selection
- Recurrence patterns (daily, weekly, monthly)
- QR code generation for check-in
- PDF export with QR codes
- Admin approval workflow
- Real-time check-in functionality
- Quota policy enforcement
- Facility recommendations

**Data Model:**
```javascript
{
  bookingId: string,
  userId: string,
  facilityId: string,
  startTime: ISO8601,
  endTime: ISO8601,
  status: "PENDING" | "APPROVED" | "CHECKED_IN" | "CANCELLED",
  recurrencePattern: object,
  quota: number,
  approvalStatus: "PENDING_APPROVAL" | "APPROVED" | "REJECTED"
}
```

**Implementation Status:** ✅ Complete with QR codes & PDF export

---

### 3. Facilities Feature (`/features/facilities/`)

**Purpose:** Facility browsing, search, and management

**Components:**
| Component | Purpose |
|-----------|---------|
| `FacilityManagementDashboard.jsx` | Overview of all facilities |
| `FacilityDetailsPage.jsx` | Detailed facility information |
| `FacilitySearch.jsx` | Search/filter facilities |
| `FacilityCard.jsx` | Facility display card (reusable) |
| `FacilitySuggestionsView.jsx` | Recommend facilities to user |
| `UnderutilizedFacilitiesView.jsx` | Admin view of underutilized facilities |

**Key Features:**
- Browse all facilities with details
- Search/filter by name, type, capacity
- View facility availability
- Get recommendations based on booking history
- Admin view of utilization metrics

**Implementation Status:** ✅ Complete

---

### 4. Tickets Feature (`/features/tickets/`)

**Purpose:** Support ticket management system

**Components:**
| Component | Purpose |
|-----------|---------|
| `TicketDashboard.jsx` | Overview of all tickets |
| `TicketDetailView.jsx` | Individual ticket details |
| `TicketEditModal.jsx` | Edit ticket information |
| `TicketStatusUpdate.jsx` | Update ticket status |
| `TicketAssignmentDialog.jsx` | Assign tickets to staff |

**Key Features:**
- Create support tickets
- Track ticket status
- Assign to staff members
- Update ticket information
- Dashboard overview

**Implementation Status:** ✅ Complete

---

### 5. Approvals Feature (`/features/approvals/`)

**Purpose:** Manage booking approvals

**Components:**
- `ApprovalQueue.jsx` - Queue of pending approvals

**Key Features:**
- View pending bookings requiring approval
- Approve/reject bookings
- Add approval comments
- Bulk operations

**Roles:** LECTURER, FACILITY_MANAGER, ADMIN

**Implementation Status:** ✅ Complete

---

### 6. Notifications Feature (`/features/notifications/`)

**Purpose:** Centralized notification management

**Components:**
- `NotificationCenter.jsx` - Display and manage notifications

**Key Features:**
- View all system notifications
- Mark as read/unread
- Toast notifications for real-time updates
- Notification categories

**Implementation Status:** ✅ Complete

---

### 7. Appeals Feature (`/features/appeals/`)

**Purpose:** Suspension appeals management

**Components:**
- `AppealCenter.jsx` - Manage suspension appeals

**Key Features:**
- View suspension status
- Submit appeal requests
- Track appeal progress
- View appeal history

**Implementation Status:** ✅ Complete

---

### 8. Analytics Feature (`/features/analytics/`)

**Purpose:** Facility utilization analytics

**Components:**
- `UtilizationDashboard.jsx` - Analytics and statistics

**Key Features:**
- Facility usage statistics
- Utilization trends
- Peak hours analysis
- Revenue metrics
- Usage reports

**Implementation Status:** ✅ Complete

---

### 9. Admin Feature (`/features/admin/`)

**Purpose:** System administration

**Components:**
- `AdminUserManagementPanel.jsx` - Manage users

**Key Features:**
- View all system users
- Manage user roles
- Suspend/unsuspend users
- View user activity

**Roles:** ADMIN only

**Implementation Status:** ✅ Complete

---

## Routes & Navigation

### Route Map

| Path | Component | Auth Required | Roles | Purpose |
|------|-----------|---------------|-------|---------|
| `/login` | AuthPage | ❌ | - | Public login page |
| `/auth/callback` | OAuth callback | ❌ | - | OAuth redirect handler |
| `/registration-pending` | RegistrationPendingPage | ❌ | - | Pending registration |
| `/` | Dashboard | ✅ | All | Main dashboard |
| `/dashboard` | Dashboard | ✅ | All | Main dashboard |
| `/bookings` | BookingForm | ✅ | LECTURER, STUDENT | Create bookings |
| `/bookings/recommendations` | BookingRecommendations | ✅ | LECTURER, STUDENT | Facility recommendations |
| `/my-bookings` | BookingsList | ✅ | All | Personal bookings |
| `/facilities` | FacilityManagementDashboard | ✅ | All | All facilities |
| `/facilities/:id` | FacilityDetailsPage | ✅ | All | Facility details |
| `/facilities/suggestions` | FacilitySuggestionsView | ✅ | LECTURER, STUDENT | Suggestions |
| `/facilities/underutilized` | UnderutilizedFacilitiesView | ✅ | ADMIN, FACILITY_MANAGER | Admin view |
| `/approvals/bookings` | ApprovalQueue | ✅ | LECTURER, FACILITY_MANAGER, ADMIN | Pending approvals |
| `/admin/bookings` | AdminBookingsView | ✅ | ADMIN, FACILITY_MANAGER | All bookings |
| `/tickets` | TicketDashboard | ✅ | All | Ticket management |
| `/notifications` | NotificationCenter | ✅ | All | Notifications |
| `/appeals` | AppealCenter | ✅ | All | Appeals management |
| `/analytics` | UtilizationDashboard | ✅ | ADMIN, FACILITY_MANAGER | Analytics |
| `/admin/users` | AdminUserManagementPanel | ✅ | ADMIN | User management |

### Navigation Structure

**App Shell (`AppShell.jsx`)** - Persistent layout providing:
- Header with branding
- Navigation menu
- User profile dropdown
- Logout button
- Role-based menu visibility

**Route Protection** - `ProtectedRoute` component:
- Checks if user is authenticated
- Validates user's role against required roles
- Checks suspension status
- Redirects to login if not authenticated
- Prevents access if role doesn't match

---

## Authentication & Security

### Authentication Flow

```
1. User at Login Page
   ↓
2. Choose: "Google OAuth" OR "Email/Password"
   ↓
3a. OAUTH FLOW:
   - Click "Login with Google"
   - Google OAuth popup
   - User grants permission
   - OAuth callback receives authorization code
   
3b. EMAIL/PASSWORD FLOW:
   - Enter email & password
   - Submit form
   ↓
4. Backend Authentication:
   - OAuth flow: Exchange code for tokens
   - Email flow: Validate credentials
   - Return JWT access token + refresh token
   ↓
5. Frontend Stores:
   - Access token → localStorage
   - Refresh token → localStorage
   - User profile → localStorage + context
   ↓
6. Redirect to Dashboard
   - User authenticated in AuthContext
   - All subsequent API calls include JWT
```

### Token Management

**Storage:**
- Access token: localStorage (short-lived)
- Refresh token: localStorage (long-lived)
- User profile: localStorage + Context

**Automatic Refresh:**
- API interceptor catches 401 responses
- Uses refresh token to get new access token
- Retries original request with new token
- If refresh fails, redirects to login

**Logout:**
- Clear tokens from localStorage
- Clear user from context
- Redirect to login page

### Role-Based Access Control (RBAC)

**Available Roles:**
- `STUDENT` - Basic facility booking
- `LECTURER` - Facility booking + approvals
- `FACILITY_MANAGER` - Facility management + approvals
- `ADMIN` - Full system access

**Implementation:**
```javascript
// In ProtectedRoute
<ProtectedRoute 
  requiredRoles={["ADMIN", "FACILITY_MANAGER"]}
  element={<AdminBookingsView />}
/>

// Conditional rendering
if (user.roles.includes("ADMIN")) {
  // Show admin panel
}
```

### Suspension Handling

**Flow:**
1. User is suspended by admin
2. `isSuspended` flag set in user profile
3. `ProtectedRoute` detects suspension
4. Redirects to appeals page
5. User can submit appeal
6. Admin reviews and lifts suspension

---

## State Management

### Authentication Context (`AuthContext.jsx`)

**Global State:**
```javascript
{
  isAuthenticated: boolean,
  user: {
    id: string,
    name: string,
    email: string,
    roles: string[],
    isSuspended: boolean,
    profilePicture?: string
  },
  loading: boolean,
  error: string | null,
  token: string | null
}
```

**Methods Provided:**
```javascript
login(code)                    // OAuth login
loginWithEmailPassword(email, password)  // Email login
logout()                       // Clear auth
refreshToken()                 // Refresh JWT
```

**Hook Usage:**
```javascript
const { user, isAuthenticated, login, logout } = useAuth();
```

### Local Component State

**Pattern:**
- Use React hooks `useState` for UI state
- Fetch data from services
- Update component state
- Re-render with new data

**Example:**
```javascript
const [bookings, setBookings] = useState([]);
const [loading, setLoading] = useState(true);

useEffect(() => {
  bookingService.getBookings()
    .then(setBookings)
    .finally(() => setLoading(false));
}, []);
```

### Server State

- All data fetched via API services
- Cached in component/context state
- No global cache layer (simple approach)
- Services handle all API logic

---

## Services & API Integration

### API Client (`apiClient.js`)

**Base Configuration:**
```javascript
baseURL: "http://localhost:8080/api"
timeout: 10000
withCredentials: true
```

**Request Interceptor:**
- Adds `Authorization: Bearer {token}` header
- Includes credentials for CORS

**Response Interceptor:**
- Catches 401 Unauthorized
- Attempts token refresh
- Retries original request
- Redirects to login if refresh fails

**Error Handling:**
- Consistent error object format
- Logs errors for debugging
- Propagates errors to components

### Auth Service (`authService.js`)

**Methods:**
```javascript
getGoogleLoginUrl()              // Get OAuth consent URL
exchangeCodeForToken(code)       // OAuth token exchange
loginWithEmailPassword(email, password)  // Email/password auth
storeTokens(accessToken, refreshToken)   // Persist tokens
getStoredToken()                 // Retrieve stored token
refreshAccessToken()             // Refresh JWT
logout()                         // Clear auth data
getUserProfile()                 // Fetch user details
validateToken()                  // Check token validity
```

**Token Lifecycle:**
```
1. User logs in
2. tokens received from backend
3. storeTokens() saves to localStorage
4. Tokens used by apiClient automatically
5. On 401: refreshAccessToken() called
6. New token stored
7. Original request retried
8. On logout: tokens cleared
```

### Booking Service (`bookingService.js`)

**Key Methods:**
```javascript
getBookings()                    // Fetch user's bookings
getAdminBookings()               // Fetch all bookings (admin)
createBooking(bookingData)       // Create new booking
cancelBooking(bookingId)         // Cancel booking
checkIn(bookingId)               // Check-in to booking
getApprovalQueue()               // Pending approvals
approveBooking(bookingId)        // Approve booking
rejectBooking(bookingId, reason) // Reject booking
getFacilityAvailability(facilityId, date)  // Check availability
```

**Error Handling:**
- Validation errors returned as messages
- 404 for not found resources
- 409 for conflicts
- Errors displayed in UI via toast

---

## Component Structure

### Feature Components Pattern

Each feature follows modular structure:

```
features/bookings/
├── BookingForm.jsx          # Main component
├── BookingsList.jsx         # List view
├── BookingDetails.jsx       # Detail view
├── QRCodeGenerator.jsx      # Sub-component
├── RecurrenceSelector.jsx   # Sub-component
└── TimeslotPicker.jsx       # Sub-component
```

### Component Patterns

**Feature Component:**
```javascript
export default function BookingForm() {
  const { user } = useAuth();
  const [bookingData, setBookingData] = useState({});
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (data) => {
    setLoading(true);
    try {
      await bookingService.createBooking(data);
      // Success handling
    } catch (error) {
      // Error handling
    }
    setLoading(false);
  };

  return (
    <div>
      {/* JSX */}
    </div>
  );
}
```

**Sub-Component (Reusable):**
```javascript
export default function TimeslotPicker({ selectedTime, onTimeChange }) {
  return (
    <select value={selectedTime} onChange={e => onTimeChange(e.target.value)}>
      {/* Options */}
    </select>
  );
}
```

### Styling

**Approach:** Utility-first CSS with Tailwind

```jsx
<button className="bg-blue-600 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded">
  Book Facility
</button>
```

**Benefits:**
- No CSS file management
- Consistent styling
- Small bundle size
- Easy to customize via tailwind.config.js

---

## Build & Deployment

### Development Server

**Start Development:**
```bash
npm run dev
```

**Output:**
```
VITE v8.0.1 ready in ... ms

➜  Local:   http://localhost:5173/
➜  press h to show help
```

**Features:**
- Hot Module Replacement (HMR)
- Instant component updates
- No page refresh needed
- Error overlay for debugging

### Production Build

**Create Optimized Build:**
```bash
npm run build
```

**Output:**
```
dist/
├── index.html        # Entry point
├── assets/
│   ├── index-{hash}.js
│   ├── index-{hash}.css
│   └── ...
```

**Optimization:**
- Code splitting by routes
- Dead code elimination
- CSS minification
- JavaScript minification
- Asset optimization

### Docker Deployment

**Dockerfile:**
```dockerfile
FROM node:20 AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:latest
COPY nginx.conf /etc/nginx/nginx.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

**Build & Run:**
```bash
docker build -t smart-campus-frontend .
docker run -p 5173:80 smart-campus-frontend
```

### Environment Configuration

**Development (.env.local):**
```
VITE_API_BASE_URL=http://localhost:8080/api
VITE_GOOGLE_CLIENT_ID=your-google-client-id
```

**Production (.env.production):**
```
VITE_API_BASE_URL=https://api.production.com
VITE_GOOGLE_CLIENT_ID=production-client-id
```

---

## Development Guidelines

### Code Organization

**1. Feature-Based Structure**
```
- Group related components in feature folders
- Each feature is independent
- Reduces cognitive load
- Enables parallel development
```

**2. Service Layer**
```
- All API calls in /services/
- No axios calls in components
- Services handle errors
- Consistent response format
```

**3. Context for Global State**
```
- Only auth in global context
- Local state for UI
- Reduces prop drilling
- Improves performance
```

### Naming Conventions

**Components:** PascalCase
```javascript
BookingForm.jsx
FacilityCard.jsx
AdminUserManagementPanel.jsx
```

**Variables/Functions:** camelCase
```javascript
const bookingData = {};
const handleSubmit = () => {};
const getUserRole = (user) => {};
```

**Constants:** UPPER_SNAKE_CASE
```javascript
const API_BASE_URL = "http://localhost:8080/api";
const DEFAULT_TIMEOUT = 10000;
```

### Component Development Workflow

**1. Create Component File:**
```javascript
// features/bookings/BookingForm.jsx
export default function BookingForm() {
  return <div>Content</div>;
}
```

**2. Add Hooks & Logic:**
```javascript
const { user } = useAuth();
const [state, setState] = useState();
useEffect(() => {}, []);
```

**3. Integrate Service:**
```javascript
import bookingService from "../../services/bookingService";

const handleSubmit = async (data) => {
  await bookingService.createBooking(data);
};
```

**4. Add Styling:**
```jsx
<button className="bg-blue-600 hover:bg-blue-700 text-white py-2 px-4 rounded">
  Submit
</button>
```

**5. Test Locally:**
```bash
npm run dev  # Start dev server
# Navigate to route and test
```

### Error Handling Pattern

```javascript
try {
  const result = await bookingService.createBooking(data);
  toast.success("Booking created successfully");
  navigate("/my-bookings");
} catch (error) {
  const message = error.response?.data?.message || error.message;
  toast.error(message);
  setError(message);
}
```

### Performance Best Practices

**1. Code Splitting:**
```javascript
// Lazy load routes
const BookingForm = lazy(() => import("./features/bookings/BookingForm"));
```

**2. Memoization:**
```javascript
const FacilityCard = memo(({ facility, onSelect }) => {
  return <div onClick={() => onSelect(facility)}>{facility.name}</div>;
});
```

**3. Use useCallback:**
```javascript
const handleChange = useCallback((value) => {
  setFilter(value);
}, []);
```

**4. Avoid Unnecessary Re-renders:**
```javascript
// Good: Dependency array
useEffect(() => {
  fetchData();
}, [userId]); // Only when userId changes

// Avoid: Empty or missing dependency array
useEffect(() => {
  fetchData();
}); // Runs every render!
```

### Common Components

**Toast Notifications:**
```javascript
import { toast } from "react-toastify";

toast.success("Operation successful");
toast.error("Something went wrong");
toast.info("Information message");
```

**Loading State:**
```jsx
{loading ? (
  <div className="flex justify-center items-center">
    <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
  </div>
) : (
  <div>{content}</div>
)}
```

**Protected Route Usage:**
```jsx
<Route
  path="/admin/users"
  element={
    <ProtectedRoute 
      requiredRoles={["ADMIN"]}
      element={<AdminUserManagementPanel />}
    />
  }
/>
```

---

## Known Limitations & Future Enhancements

### Current Limitations
- No offline support
- No service worker
- No local caching strategy
- Limited validation feedback
- No real-time updates (WebSocket)

### Future Enhancements
- [ ] Real-time notifications via WebSocket
- [ ] Progressive Web App (PWA) support
- [ ] Offline mode with sync
- [ ] Advanced search/filtering
- [ ] Calendar integration
- [ ] Mobile-responsive improvements
- [ ] Performance monitoring
- [ ] Dark mode support

---

## Quick Reference Commands

```bash
# Development
npm run dev              # Start dev server
npm run lint            # Run linter
npm run build           # Production build
npm run preview         # Preview production build

# Dependencies
npm install             # Install dependencies
npm update              # Update packages
npm audit               # Check security

# Testing & Debugging
# Dev tools in browser (F12)
# - React Developer Tools
# - Redux DevTools (if added)
```

---

## Frontend Contact Points with Backend

### API Endpoints Called

**Authentication:**
```
POST   /api/auth/google/exchange    - OAuth token exchange
POST   /api/auth/login              - Email/password login
POST   /api/auth/refresh            - Token refresh
POST   /api/auth/logout             - Logout
GET    /api/auth/profile            - Fetch user profile
```

**Bookings:**
```
GET    /api/bookings                - List user bookings
GET    /api/bookings/admin          - List all bookings (admin)
POST   /api/bookings                - Create booking
GET    /api/bookings/:id            - Get booking details
PATCH  /api/bookings/:id            - Update booking
DELETE /api/bookings/:id            - Cancel booking
POST   /api/bookings/:id/check-in   - Check-in
GET    /api/bookings/approvals      - Get pending approvals
PATCH  /api/bookings/:id/approve    - Approve booking
PATCH  /api/bookings/:id/reject     - Reject booking
```

**Facilities:**
```
GET    /api/facilities              - List all facilities
GET    /api/facilities/:id          - Get facility details
GET    /api/facilities/:id/availability - Check availability
GET    /api/facilities/search       - Search facilities
GET    /api/facilities/suggestions  - Get recommendations
GET    /api/facilities/underutilized - Underutilized facilities
```

**Other:**
```
GET    /api/notifications           - Get notifications
GET    /api/tickets                 - Get tickets
POST   /api/tickets                 - Create ticket
GET    /api/appeals                 - Get appeals
POST   /api/appeals                 - Submit appeal
GET    /api/admin/users             - List users
PATCH  /api/admin/users/:id/suspend - Suspend user
GET    /api/analytics               - Get analytics data
```

---

## Summary

The Smart Campus Operations Hub frontend is a feature-rich React application providing:

✅ **Robust Authentication** - OAuth + email/password with token management  
✅ **Complete Booking System** - Create, approve, check-in with QR codes  
✅ **Facility Management** - Search, recommend, track utilization  
✅ **Admin Tools** - User management, approval workflows, analytics  
✅ **Scalable Architecture** - Feature-based, service layer, context API  
✅ **Modern Tech Stack** - React 19, Vite, Tailwind CSS  

Ready for enhancement and expansion with new features!
