/**
 * Booking Service
 * Provides booking-related API calls
 */
import { apiClient } from "./apiClient";

export const bookingService = {
  /**
   * Create a new booking
   */
  createBooking: async (bookingData) => {
    const response = await apiClient.post("/v1/bookings", bookingData);
    return response.data;
  },

  /**
   * Get user's bookings with optional filtering
   */
  getUserBookings: async (filters = {}) => {
    const response = await apiClient.get("/v1/bookings", { params: filters });
    return Array.isArray(response.data) ? response.data : [];
  },

  /**
   * Get booking details by ID
   */
  getBookingById: async (bookingId) => {
    const response = await apiClient.get(`/v1/bookings/${bookingId}`);
    return response.data;
  },

  /**
   * Cancel a booking
   */
  cancelBooking: async (bookingId) => {
    const response = await apiClient.post(`/v1/bookings/${bookingId}/cancel`);
    return response.data;
  },

  /**
   * Approve a booking step
   */
  approveBooking: async (bookingId, note = null) => {
    const response = await apiClient.post(`/v1/bookings/${bookingId}/approve`, { note });
    return response.data;
  },

  /**
   * Reject a booking
   */
  rejectBooking: async (bookingId, note = null) => {
    const response = await apiClient.post(`/v1/bookings/${bookingId}/reject`, { note });
    return response.data;
  },

  /**
   * Get bookings pending approval for current user
   */
  getPendingApprovals: async () => {
    const response = await apiClient.get("/v1/bookings/pending-approvals");
    return Array.isArray(response.data) ? response.data : [];
  },

  /**
   * Get user's quota status
   */
  getQuotaStatus: async () => {
    const response = await apiClient.get("/v1/bookings/quota-status");
    return response.data;
  },

  /**
   * Check in to a booking
   */
  checkIn: async (bookingId, method = "MANUAL") => {
    const response = await apiClient.post(`/v1/bookings/${bookingId}/check-in`, { method });
    return response.data;
  },

  /**
   * Get check-in records for a booking
   */
  getCheckInRecords: async (bookingId) => {
    const response = await apiClient.get(`/v1/bookings/${bookingId}/check-in-records`);
    return Array.isArray(response.data) ? response.data : [];
  },

  /**
   * Get available facilities
   */
  searchFacilities: async (filters = {}) => {
    const response = await apiClient.get("/v1/facilities", { params: filters });
    return Array.isArray(response.data) ? response.data : [];
  },

  /**
   * Get facility by ID
   */
  getFacilityById: async (facilityId) => {
    const response = await apiClient.get(`/v1/facilities/${facilityId}`);
    return response.data;
  },

  /**
   * Check for booking availability (overlap detection)
   */
  checkAvailability: async (facilityId, bookingDate, startTime, endTime) => {
    const response = await apiClient.get("/v1/bookings/availability", {
      params: { facilityId, bookingDate, startTime, endTime },
    });
    return response.data;
  },

  /**
   * Get users (for admin bookedFor selector)
   */
  getUsers: async (filters = {}) => {
    const response = await apiClient.get("/v1/users", { params: filters });
    return Array.isArray(response.data) ? response.data : [];
  },

  /**
   * Expand recurring booking rule to actual dates
   */
  expandRecurrence: async (rrule, startDate) => {
    const response = await apiClient.post("/v1/bookings/expand-recurrence", {
      rrule,
      startDate,
    });
    return Array.isArray(response.data) ? response.data : [];
  },

  /**
   * Get public holidays
   */
  getPublicHolidays: async (year) => {
    const response = await apiClient.get("/v1/public-holidays", { params: { year } });
    return Array.isArray(response.data) ? response.data : [];
  },
};
