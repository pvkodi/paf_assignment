/**
 * Type Definitions and DTOs for Facilities and Bookings
 */

/**
 * Facility Types
 */
export const FACILITY_TYPES = {
  LECTURE_HALL: "LECTURE_HALL",
  LAB: "LAB",
  MEETING_ROOM: "MEETING_ROOM",
  AUDITORIUM: "AUDITORIUM",
  EQUIPMENT: "EQUIPMENT",
  SPORTS_FACILITY: "SPORTS_FACILITY",
};

/**
 * Facility Status
 */
export const FACILITY_STATUS = {
  AVAILABLE: "AVAILABLE",
  MAINTENANCE: "MAINTENANCE",
  OUT_OF_SERVICE: "OUT_OF_SERVICE",
};

/**
 * Booking Status
 */
export const BOOKING_STATUS = {
  PENDING_LECTURER_APPROVAL: "PENDING_LECTURER_APPROVAL",
  PENDING_ADMIN_APPROVAL: "PENDING_ADMIN_APPROVAL",
  PENDING_FACILITY_MANAGER_APPROVAL: "PENDING_FACILITY_MANAGER_APPROVAL",
  APPROVED: "APPROVED",
  REJECTED: "REJECTED",
  CANCELLED: "CANCELLED",
  CHECKED_IN: "CHECKED_IN",
  NO_SHOW: "NO_SHOW",
};

/**
 * Check-in Methods
 */
export const CHECK_IN_METHODS = {
  QR_CODE: "QR_CODE",
  MANUAL: "MANUAL",
};

/**
 * Facility Response DTO
 * @typedef {Object} Facility
 * @property {string} id - Facility ID
 * @property {string} name - Facility name
 * @property {string} type - Facility type (from FACILITY_TYPES)
 * @property {string} subtype - Facility subtype
 * @property {number} capacity - Maximum capacity
 * @property {string} location - Location
 * @property {string} building - Building name
 * @property {string} floor - Floor number
 * @property {string} status - Facility status (from FACILITY_STATUS)
 * @property {string} description - Facility description
 * @property {Array<Object>} operatingHours - Operating hours
 * @property {Object} subtypeAttributes - Additional attributes by subtype
 * @property {string} createdAt - Created timestamp
 * @property {string} updatedAt - Updated timestamp
 */

/**
 * Booking Response DTO
 * @typedef {Object} Booking
 * @property {string} id - Booking ID
 * @property {string} facilityId - Facility ID
 * @property {Object} facility - Full facility details
 * @property {string} requestedBy - User ID who requested the booking
 * @property {Object} requestingUser - Full user details of requester
 * @property {string} bookedFor - User ID for whom the booking is made (if different from requester)
 * @property {Object} bookedForUser - Full user details of booked-for user
 * @property {string} date - Booking date (ISO format)
 * @property {string} startTime - Start time (HH:mm)
 * @property {string} endTime - End time (HH:mm)
 * @property {number} attendees - Number of attendees
 * @property {string} purpose - Booking purpose
 * @property {string} status - Booking status (from BOOKING_STATUS)
 * @property {string} recurrenceRule - Recurrence rule (RRULE format)
 * @property {Array<string>} skippedDates - Dates skipped due to public holidays
 * @property {string} checkedInAt - Check-in timestamp
 * @property {string} checkInMethod - How it was checked in (QR_CODE or MANUAL)
 * @property {Array<Object>} approvals - Approval workflow status
 * @property {number} version - Optimistic lock version
 * @property {string} createdAt - Created timestamp
 * @property {string} updatedAt - Updated timestamp
 */

/**
 * Booking Create Request DTO
 * @typedef {Object} BookingCreateRequest
 * @property {string} facilityId - Facility ID
 * @property {string} date - Date (ISO format)
 * @property {string} startTime - Start time (HH:mm)
 * @property {string} endTime - End time (HH:mm)
 * @property {number} attendees - Number of attendees
 * @property {string} purpose - Booking purpose
 * @property {string} recurrenceRule - Recurrence rule (optional)
 * @property {string} bookedFor - User ID for ADMIN bookings (optional)
 */

/**
 * Paginated Response
 * @typedef {Object} PaginatedResponse
 * @property {Array} content - Items in the page
 * @property {number} totalElements - Total number of items
 * @property {number} totalPages - Total number of pages
 * @property {number} number - Current page number
 * @property {number} size - Page size
 * @property {boolean} empty - Is page empty
 * @property {boolean} first - Is this the first page
 * @property {boolean} last - Is this the last page
 */

/**
 * Validation error response
 * @typedef {Object} ValidationError
 * @property {string} field - Field name
 * @property {string} message - Error message
 */

/**
 * API Error Response
 * @typedef {Object} ApiError
 * @property {number} status - HTTP status code
 * @property {string} error - Error type
 * @property {string} message - Error message
 * @property {Array<ValidationError>} validationErrors - Validation errors (if applicable)
 */

/**
 * Format booking time for display
 * @param {string} time - Time in HH:mm format
 * @returns {string} Formatted time
 */
export const formatTime = (time) => {
  if (!time) return "";
  return time;
};

/**
 * Format booking date for display
 * @param {string} date - Date in ISO format
 * @returns {string} Formatted date
 */
export const formatDate = (date) => {
  if (!date) return "";
  return new Date(date).toLocaleDateString("en-US", {
    year: "numeric",
    month: "long",
    day: "numeric",
  });
};

/**
 * Get friendly label for booking status
 * @param {string} status - Booking status
 * @returns {string} Friendly status label
 */
export const getStatusLabel = (status) => {
  const labels = {
    PENDING_LECTURER_APPROVAL: "Pending Lecturer Approval",
    PENDING_ADMIN_APPROVAL: "Pending Admin Approval",
    PENDING_FACILITY_MANAGER_APPROVAL: "Pending Manager Approval",
    APPROVED: "Approved",
    REJECTED: "Rejected",
    CANCELLED: "Cancelled",
    CHECKED_IN: "Checked In",
    NO_SHOW: "No Show",
  };
  return labels[status] || status;
};

/**
 * Get status badge color
 * @param {string} status - Booking status
 * @returns {string} Tailwind color class
 */
export const getStatusColor = (status) => {
  const colors = {
    PENDING_LECTURER_APPROVAL: "bg-yellow-100 text-yellow-800",
    PENDING_ADMIN_APPROVAL: "bg-yellow-100 text-yellow-800",
    PENDING_FACILITY_MANAGER_APPROVAL: "bg-yellow-100 text-yellow-800",
    APPROVED: "bg-green-100 text-green-800",
    REJECTED: "bg-red-100 text-red-800",
    CANCELLED: "bg-gray-100 text-gray-800",
    CHECKED_IN: "bg-blue-100 text-blue-800",
    NO_SHOW: "bg-red-100 text-red-800",
  };
  return colors[status] || "bg-gray-100 text-gray-800";
};

/**
 * Get friendly label for facility type
 * @param {string} type - Facility type
 * @returns {string} Friendly type label
 */
export const getFacilityTypeLabel = (type) => {
  const labels = {
    LECTURE_HALL: "Lecture Hall",
    LAB: "Laboratory",
    MEETING_ROOM: "Meeting Room",
    AUDITORIUM: "Auditorium",
    EQUIPMENT: "Equipment",
    SPORTS_FACILITY: "Sports Facility",
  };
  return labels[type] || type;
};
