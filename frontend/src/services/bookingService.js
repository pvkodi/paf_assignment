import apiClient from "./apiClient";

/**
 * Booking Service
 * Handles API interactions for booking creation, retrieval, and management
 */
export const bookingService = {
  /**
   * Get pending bookings for current user's approval role
   * @param {Object} options - Query options
   * @param {number} options.page - Page number (default 0)
   * @param {number} options.size - Page size (default 20)
   * @returns {Promise<Object>} Paginated booking list
   */
  async getPendingBookings(options = { page: 0, size: 20 }) {
    try {
      const response = await apiClient.get("/v1/bookings", {
        params: {
          page: options.page,
          size: options.size,
        },
      });
      return response.data;
    } catch (error) {
      console.error("Failed to get pending bookings:", error);
      throw error;
    }
  },

  /**
   * Get user's own booking requests (both pending and approved)
   * @param {Object} options - Query options
   * @param {number} options.page - Page number (default 0)
   * @param {number} options.size - Page size (default 20)
   * @returns {Promise<Object>} Paginated booking list
   */
  async getUserBookings(options = { page: 0, size: 20 }) {
    try {
      const response = await apiClient.get("/v1/bookings/mine", {
        params: {
          page: options.page,
          size: options.size,
        },
      });
      return response.data;
    } catch (error) {
      console.error("Failed to get user bookings:", error);
      throw error;
    }
  },

  /**
   * Get booking details by ID
   * @param {string} bookingId - Booking ID
   * @returns {Promise<Object>} Booking object
   */
  async getBookingById(bookingId) {
    try {
      const response = await apiClient.get(`/v1/bookings/${bookingId}`);
      return response.data;
    } catch (error) {
      console.error("Failed to get booking:", error);
      throw error;
    }
  },

  /**
   * Create a new booking request
   * @param {Object} bookingData - Booking creation data
   * @param {string} bookingData.facilityId - Facility ID
   * @param {string} bookingData.date - Date (ISO format)
   * @param {string} bookingData.startTime - Start time (HH:mm)
   * @param {string} bookingData.endTime - End time (HH:mm)
   * @param {string} bookingData.purpose - Booking purpose
   * @param {number} bookingData.attendees - Number of attendees
   * @param {string} bookingData.recurrenceRule - Recurrence rule (optional, RRULE format)
   * @param {string} bookingData.bookedFor - User ID to book for (ADMIN only)
   * @returns {Promise<Object>} Created booking object
   */
  async createBooking(bookingData) {
    try {
      const response = await apiClient.post("/v1/bookings", bookingData);
      return response.data;
    } catch (error) {
      console.error("Failed to create booking:", error);
      throw error;
    }
  },

  /**
   * Approve a booking request (LECTURER or ADMIN role)
   * @param {string} bookingId - Booking ID
   * @returns {Promise<Object>} Updated booking object
   */
  async approveBooking(bookingId) {
    try {
      const response = await apiClient.post(
        `/v1/bookings/${bookingId}/approve`,
      );
      return response.data;
    } catch (error) {
      console.error("Failed to approve booking:", error);
      throw error;
    }
  },

  /**
   * Reject a booking request (LECTURER or ADMIN role)
   * @param {string} bookingId - Booking ID
   * @param {string} reason - Rejection reason
   * @returns {Promise<Object>} Updated booking object
   */
  async rejectBooking(bookingId, reason) {
    try {
      const response = await apiClient.post(
        `/v1/bookings/${bookingId}/reject`,
        {
          reason,
        },
      );
      return response.data;
    } catch (error) {
      console.error("Failed to reject booking:", error);
      throw error;
    }
  },

  /**
   * Check in to a booking
   * @param {string} bookingId - Booking ID
   * @param {string} checkInMethod - Check-in method (QR_CODE or MANUAL)
   * @param {string} qrData - QR code data (optional)
   * @returns {Promise<Object>} Updated booking object
   */
  async checkInBooking(bookingId, checkInMethod = "MANUAL", qrData = null) {
    try {
      const payload = {
        method: checkInMethod,
      };
      if (qrData) {
        payload.qrData = qrData;
      }
      const response = await apiClient.post(
        `/v1/bookings/${bookingId}/check-in`,
        payload,
      );
      return response.data;
    } catch (error) {
      console.error("Failed to check in booking:", error);
      throw error;
    }
  },

  /**
   * Cancel a booking
   * @param {string} bookingId - Booking ID
   * @param {string} reason - Cancellation reason
   * @returns {Promise<Object>} Updated booking object
   */
  async cancelBooking(bookingId, reason) {
    try {
      const response = await apiClient.post(
        `/v1/bookings/${bookingId}/cancel`,
        {
          reason,
        },
      );
      return response.data;
    } catch (error) {
      console.error("Failed to cancel booking:", error);
      throw error;
    }
  },
};

export default bookingService;
