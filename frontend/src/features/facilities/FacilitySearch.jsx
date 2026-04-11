import React, { useState, useEffect } from "react";
import facilityService from "../../services/facilityService";
import { FACILITY_TYPES, formatDate, getFacilityTypeLabel } from "../../types";

/**
 * FacilitySearch Component
 * Allows users to search and filter facilities for booking
 * Features:
 * - Auto-loads all facilities on mount
 * - Filter by type, capacity, location, building
 * - Display results with facility cards in a grid
 * - Selection/navigation to booking form
 */
function FacilitySearch({ onFacilitySelected }) {
  const [filters, setFilters] = useState({
    type: "",
    minCapacity: "",
    location: "",
    building: "",
  });
  const [facilities, setFacilities] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [hasSearched, setHasSearched] = useState(false);

  /**
   * Load all facilities on component mount
   */
  useEffect(() => {
    const loadAllFacilities = async () => {
      setLoading(true);
      setError(null);
      try {
        const results = await facilityService.searchFacilities({});
        setFacilities(results);
        setHasSearched(true);
      } catch (err) {
        setError(err.response?.data?.message || "Failed to load facilities");
        setFacilities([]);
      } finally {
        setLoading(false);
      }
    };

    loadAllFacilities();
  }, []);

  /**
   * Handle filter change
   */
  const handleFilterChange = (e) => {
    const { name, value } = e.target;
    setFilters((prev) => ({
      ...prev,
      [name]: value,
    }));
  };

  /**
   * Search facilities based on current filters
   */
  const handleSearch = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError(null);

    try {
      const cleanedFilters = {};
      if (filters.type) cleanedFilters.type = filters.type;
      if (filters.minCapacity)
        cleanedFilters.minCapacity = parseInt(filters.minCapacity);
      if (filters.location) cleanedFilters.location = filters.location;
      if (filters.building) cleanedFilters.building = filters.building;

      const results = await facilityService.searchFacilities(cleanedFilters);
      setFacilities(results);
    } catch (err) {
      setError(err.response?.data?.message || "Failed to search facilities");
      setFacilities([]);
    } finally {
      setLoading(false);
    }
  };

  /**
   * Reset all filters and reload all facilities
   */
  const handleReset = async () => {
    setFilters({
      type: "",
      minCapacity: "",
      location: "",
      building: "",
    });
    setError(null);
    setLoading(true);

    try {
      const results = await facilityService.searchFacilities({});
      setFacilities(results);
    } catch (err) {
      setError(err.response?.data?.message || "Failed to load facilities");
      setFacilities([]);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="space-y-6">
      {/* Search Form */}
      <div className="bg-white rounded-lg shadow p-6">
        <h3 className="text-lg font-semibold mb-4">Filter Facilities</h3>
        <form onSubmit={handleSearch} className="space-y-4">
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
            {/* Type Filter */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Facility Type
              </label>
              <select
                name="type"
                value={filters.type}
                onChange={handleFilterChange}
                className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
              >
                <option value="">All Types</option>
                {Object.entries(FACILITY_TYPES).map(([key, value]) => (
                  <option key={value} value={value}>
                    {getFacilityTypeLabel(value)}
                  </option>
                ))}
              </select>
            </div>

            {/* Capacity Filter */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Minimum Capacity
              </label>
              <input
                type="number"
                name="minCapacity"
                value={filters.minCapacity}
                onChange={handleFilterChange}
                placeholder="e.g., 30"
                min="1"
                className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
              />
            </div>

            {/* Location Filter */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Location
              </label>
              <input
                type="text"
                name="location"
                value={filters.location}
                onChange={handleFilterChange}
                placeholder="e.g., Main Campus"
                className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
              />
            </div>

            {/* Building Filter */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Building
              </label>
              <input
                type="text"
                name="building"
                value={filters.building}
                onChange={handleFilterChange}
                placeholder="e.g., Building A"
                className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
              />
            </div>
          </div>

          {/* Action Buttons */}
          <div className="flex gap-2">
            <button
              type="submit"
              disabled={loading}
              className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:bg-gray-400 disabled:cursor-not-allowed transition"
            >
              {loading ? "Searching..." : "Search"}
            </button>
            <button
              type="button"
              onClick={handleReset}
              disabled={loading}
              className="px-4 py-2 bg-gray-200 text-gray-800 rounded-md hover:bg-gray-300 disabled:bg-gray-100 disabled:cursor-not-allowed transition"
            >
              Reset Filters
            </button>
          </div>
        </form>
      </div>

      {/* Error Message */}
      {error && (
        <div className="bg-red-50 border border-red-200 rounded-md p-4">
          <p className="text-red-800">{error}</p>
        </div>
      )}

      {/* Results Section */}
      {hasSearched && (
        <div className="space-y-4">
          {loading ? (
            <div className="text-center py-12">
              <div className="inline-block">
                <div className="w-8 h-8 border-4 border-blue-600 border-t-transparent rounded-full animate-spin"></div>
              </div>
              <p className="text-gray-600 mt-3">Loading facilities...</p>
            </div>
          ) : facilities.length > 0 ? (
            <div>
              <h3 className="text-lg font-semibold mb-4 text-gray-900">
                Available Facilities{" "}
                <span className="text-gray-500 font-normal">
                  ({facilities.length})
                </span>
              </h3>
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
                {facilities.map((facility) => (
                  <FacilityCard
                    key={facility.id}
                    facility={facility}
                    onSelect={() => onFacilitySelected(facility)}
                  />
                ))}
              </div>
            </div>
          ) : (
            <div className="bg-gray-50 rounded-md p-12 text-center border border-gray-200">
              <svg
                className="mx-auto h-12 w-12 text-gray-400 mb-3"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M9.172 16.172a4 4 0 015.656 0M9 10h.01M15 10h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
                />
              </svg>
              <p className="text-gray-600 text-lg">
                No facilities found matching your criteria. Try adjusting your
                filters.
              </p>
            </div>
          )}
        </div>
      )}
    </div>
  );
}

/**
 * FacilityCard Component
 * Displays a single facility in a card format
 * Shows facility details and allows selection for booking
 */
function FacilityCard({ facility, onSelect }) {
  return (
    <div className="bg-white rounded-lg shadow-md hover:shadow-xl transition-all duration-200 border border-gray-100 overflow-hidden flex flex-col h-full">
      {/* Card Header with Status */}
      <div className="bg-gradient-to-r from-blue-50 to-blue-100 px-6 py-4 border-b border-blue-200">
        <div className="flex items-start justify-between gap-3">
          <div className="flex-1">
            <h4 className="text-base font-semibold text-gray-900 mb-1">
              {facility.name}
            </h4>
            <p className="text-sm text-blue-700 font-medium">
              {getFacilityTypeLabel(facility.type)}
            </p>
          </div>
          <span className="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-medium bg-green-100 text-green-800">
            {facility.status === "ACTIVE" ? "✓ Available" : "Not Available"}
          </span>
        </div>
      </div>

      {/* Card Body */}
      <div className="p-6 flex-1 flex flex-col">
        {/* Details Grid */}
        <div className="space-y-3 mb-6 flex-1">
          {/* Capacity */}
          <div className="flex items-center justify-between">
            <span className="text-sm text-gray-600 flex items-center gap-2">
              <svg
                className="w-4 h-4 text-gray-400"
                fill="currentColor"
                viewBox="0 0 20 20"
              >
                <path d="M13 6a3 3 0 11-6 0 3 3 0 016 0zM18 8a2 2 0 11-4 0 2 2 0 014 0zm-2-7a1 1 0 11-2 0 1 1 0 012 0zM6 12a2 2 0 11-4 0 2 2 0 014 0z" />
              </svg>
              Capacity
            </span>
            <span className="font-semibold text-gray-900">
              {facility.capacity} people
            </span>
          </div>

          {/* Building */}
          {facility.building && (
            <div className="flex items-center justify-between">
              <span className="text-sm text-gray-600 flex items-center gap-2">
                <svg
                  className="w-4 h-4 text-gray-400"
                  fill="currentColor"
                  viewBox="0 0 20 20"
                >
                  <path d="M10.707 2.293a1 1 0 00-1.414 0l-7 7a1 1 0 001.414 1.414L4 10.414V17a1 1 0 001 1h2a1 1 0 001-1v-2a1 1 0 011-1h2a1 1 0 011 1v2a1 1 0 001 1h2a1 1 0 001-1v-6.586l.293.293a1 1 0 001.414-1.414l-7-7z" />
                </svg>
                Building
              </span>
              <span className="font-semibold text-gray-900">
                {facility.building}
              </span>
            </div>
          )}

          {/* Location */}
          {facility.location && (
            <div className="flex items-center justify-between">
              <span className="text-sm text-gray-600 flex items-center gap-2">
                <svg
                  className="w-4 h-4 text-gray-400"
                  fill="currentColor"
                  viewBox="0 0 20 20"
                >
                  <path
                    fillRule="evenodd"
                    d="M5.05 4.05a7 7 0 119.9 9.9L10 18.9l-4.95-4.95a7 7 0 010-9.9zM10 11a2 2 0 100-4 2 2 0 000 4z"
                    clipRule="evenodd"
                  />
                </svg>
                Location
              </span>
              <span className="font-semibold text-gray-900">
                {facility.location}
              </span>
            </div>
          )}

          {/* Floor */}
          {facility.floor && (
            <div className="flex items-center justify-between">
              <span className="text-sm text-gray-600">Floor</span>
              <span className="font-semibold text-gray-900">
                {facility.floor}
              </span>
            </div>
          )}
        </div>

        {/* Select Button */}
        <button
          onClick={onSelect}
          className="w-full px-4 py-2.5 bg-blue-600 text-white font-medium rounded-lg hover:bg-blue-700 transition-colors duration-200 flex items-center justify-center gap-2"
        >
          <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
            <path d="M5.5 13a3.5 3.5 0 01-.369-6.98 4 4 0 117.753-1.3A4.5 4.5 0 1113.5 13H11V9.413l1.293 1.293a1 1 0 001.414-1.414l-3-3a1 1 0 00-1.414 0l-3 3a1 1 0 001.414 1.414L9 9.414V13H5.5z" />
          </svg>
          Select & Book
        </button>
      </div>
    </div>
  );
}

export default FacilitySearch;
