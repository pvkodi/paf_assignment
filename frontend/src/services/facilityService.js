import apiClient from "./apiClient";

/**
 * Facility Service
 * Handles API interactions for facility search and discovery
 */
export const facilityService = {
  /**
   * Search facilities with optional filters
   * @param {Object} filters - Search filters
   * @param {string} filters.type - Facility type (LECTURE_HALL, LAB, MEETING_ROOM, AUDITORIUM, EQUIPMENT, SPORTS_FACILITY)
   * @param {number} filters.minCapacity - Minimum capacity
   * @param {string} filters.location - Location filter
   * @param {string} filters.building - Building filter
   * @returns {Promise<Array>} Array of facility objects
   */
  async searchFacilities(filters = {}) {
    try {
      const params = new URLSearchParams();

      if (filters.type) params.append("type", filters.type);
      if (filters.minCapacity)
        params.append("minCapacity", filters.minCapacity);
      if (filters.location) params.append("location", filters.location);
      if (filters.building) params.append("building", filters.building);

      const response = await apiClient.get(
        `/v1/facilities?${params.toString()}`,
      );
      return response.data;
    } catch (error) {
      console.error("Failed to search facilities:", error);
      throw error;
    }
  },

  /**
   * Get facility details by ID
   * @param {string} facilityId - Facility ID
   * @returns {Promise<Object>} Facility object
   */
  async getFacilityById(facilityId) {
    try {
      const response = await apiClient.get(`/v1/facilities/${facilityId}`);
      return response.data;
    } catch (error) {
      console.error("Failed to get facility:", error);
      throw error;
    }
  },

  /**
   * Get facility availability for a specific date range
   * @param {string} facilityId - Facility ID
   * @param {string} startDate - Start date (ISO format)
   * @param {string} endDate - End date (ISO format)
   * @returns {Promise<Object>} Availability data
   */
  async getFacilityAvailability(facilityId, startDate, endDate) {
    try {
      const response = await apiClient.get(
        `/v1/facilities/${facilityId}/availability`,
        {
          params: {
            startDate,
            endDate,
          },
        },
      );
      return response.data;
    } catch (error) {
      console.error("Failed to get facility availability:", error);
      throw error;
    }
  },
};

export default facilityService;
