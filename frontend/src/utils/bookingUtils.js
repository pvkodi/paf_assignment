/**
 * Booking Utility Functions
 * Helper functions for booking operations, date/time formatting, and recurrence handling.
 */

/**
 * Format date in a user-friendly way
 */
export const formatBookingDate = (dateString) => {
  if (!dateString) return "-";

  const date = new Date(dateString);
  if (isNaN(date.getTime())) return "-";

  return date.toLocaleDateString("en-US", {
    year: "numeric",
    month: "short",
    day: "numeric",
  });
};

/**
 * Format date and time together
 */
export const formatBookingDateTime = (dateString) => {
  if (!dateString) return "-";

  const date = new Date(dateString);
  if (isNaN(date.getTime())) return "-";

  return date.toLocaleString("en-US", {
    year: "numeric",
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
};

/**
 * Get status badge color classes
 */
export const getStatusColorClasses = (status) => {
  const colorMap = {
    APPROVED: "bg-green-100 text-green-800 border-green-300",
    PENDING: "bg-yellow-100 text-yellow-800 border-yellow-300",
    REJECTED: "bg-red-100 text-red-800 border-red-300",
    CANCELLED: "bg-slate-100 text-slate-800 border-slate-300",
  };
  return colorMap[status] || "bg-slate-100 text-slate-800 border-slate-300";
};

/**
 * Get decision badge color classes
 */
export const getDecisionColorClasses = (decision) => {
  const colorMap = {
    APPROVED: "bg-green-100 text-green-700 border-green-300",
    REJECTED: "bg-red-100 text-red-700 border-red-300",
    PENDING: "bg-yellow-100 text-yellow-700 border-yellow-300",
  };
  return colorMap[decision] || "bg-slate-100 text-slate-700 border-slate-300";
};

/**
 * Check if booking can be cancelled
 */
export const canCancelBooking = (booking) => {
  return booking.status === "APPROVED";
};

/**
 * Check if booking can be checked in
 */
export const canCheckInBooking = (booking) => {
  // For testing: allow check-in for all APPROVED bookings
  // In production, implement stricter time window checks
  return booking.status === "APPROVED";

  // Original time-window logic (commented for testing):
  // const now = new Date();
  // const bookingDateTime = new Date(
  //   `${booking.bookingDate}T${booking.startTime}`,
  // );
  // const endDateTime = new Date(`${booking.bookingDate}T${booking.endTime}`);
  // const startWindow = new Date(bookingDateTime.getTime() - 30 * 60 * 1000);
  // const endWindow = new Date(endDateTime.getTime() + 15 * 60 * 1000);
  // return now >= startWindow && now <= endWindow;
};

/**
 * Parse RRULE and get human-readable description
 */
export const parseRRuleDescription = (rrule) => {
  if (!rrule) return null;

  const parts = {};
  rrule.split(";").forEach((part) => {
    const [key, value] = part.split("=");
    if (key && value) {
      parts[key] = value;
    }
  });

  const freq = parts.FREQ || "NONE";
  const interval = parts.INTERVAL || "1";
  const count = parts.COUNT || "?";
  const byday = parts.BYDAY;

  let description = "";

  switch (freq) {
    case "DAILY":
      description = `Every ${interval} day(s)`;
      break;
    case "WEEKLY":
      if (byday) {
        const dayMap = {
          Mo: "Monday",
          Tu: "Tuesday",
          We: "Wednesday",
          Th: "Thursday",
          Fr: "Friday",
          Sa: "Saturday",
          Su: "Sunday",
        };
        const days = byday
          .split(",")
          .map((d) => dayMap[d] || d)
          .join(", ");
        description = `Every ${interval} week(s) on ${days}`;
      } else {
        description = `Every ${interval} week(s)`;
      }
      break;
    case "MONTHLY":
      description = `Every ${interval} month(s)`;
      break;
    default:
      description = "Custom recurrence";
  }

  return `${description} (${count} occurrences)`;
};

/**
 * Format time in HH:MM format
 */
export const formatTime = (timeString) => {
  if (!timeString) return "-";
  return timeString.substring(0, 5); // HH:MM
};

/**
 * Get approver role badge color
 */
export const getApproverRoleColor = (role) => {
  const colorMap = {
    LECTURER: "bg-purple-100 text-purple-700 border-purple-300",
    FACILITY_MANAGER: "bg-blue-100 text-blue-700 border-blue-300",
    ADMIN: "bg-red-100 text-red-700 border-red-300",
  };
  return colorMap[role] || "bg-slate-100 text-slate-700 border-slate-300";
};

/**
 * Check if user is approaching quota limit
 */
export const isApproachingQuotaLimit = (current, max) => {
  if (!max) return false; // Unlimited
  return current >= max * 0.8;
};

/**
 * Get remaining quota
 */
export const getRemainingQuota = (current, max) => {
  if (!max) return null; // Unlimited
  return Math.max(0, max - current);
};

/**
 * Format facility type for display
 */
export const formatFacilityType = (type) => {
  if (!type) return "Unknown";
  return type.replace(/_/g, " ");
};

/**
 * Get availability status color
 */
export const getAvailabilityColor = (utilization) => {
  if (utilization < 30) return "bg-red-100 text-red-700";
  if (utilization < 70) return "bg-yellow-100 text-yellow-700";
  return "bg-green-100 text-green-700";
};

/**
 * Check if booking is in the past
 */
export const isBookingInPast = (bookingDate, endTime) => {
  const now = new Date();
  const bookingEnd = new Date(`${bookingDate}T${endTime}`);
  return bookingEnd < now;
};

/**
 * Get days until booking
 */
export const getDaysUntilBooking = (bookingDate) => {
  const booking = new Date(bookingDate);
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  booking.setHours(0, 0, 0, 0);
  const diffTime = booking.getTime() - today.getTime();
  const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
  return diffDays;
};

/**
 * Format relative time (e.g., "in 2 days", "today")
 */
export const formatRelativeTime = (bookingDate) => {
  const days = getDaysUntilBooking(bookingDate);

  if (days === 0) return "Today";
  if (days === 1) return "Tomorrow";
  if (days < 0) return `${Math.abs(days)} days ago`;
  if (days <= 7) return `In ${days} days`;
  if (days <= 30) return `In ${Math.floor(days / 7)} weeks`;
  return `In ${Math.floor(days / 30)} months`;
};
