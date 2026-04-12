import React, { useState, useEffect } from 'react';
import { apiClient } from '../../services/apiClient';

/**
 * FacilitySearch Component
 * Provides facility discovery with filters for type, capacity, location, and building.
 * Implements FR-004, FR-005, FR-006 from the specification.
 */
export default function FacilitySearch({ onFacilitySelect }) {
  const [type, setType] = useState('');
  const [minCapacity, setMinCapacity] = useState('');
  const [location, setLocation] = useState('');
  const [building, setBuilding] = useState('');
  const [results, setResults] = useState([]);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);
  const [hasSearched, setHasSearched] = useState(false);

  const facilityTypes = [
    'LECTURE_HALL',
    'LAB',
    'MEETING_ROOM',
    'AUDITORIUM',
    'EQUIPMENT',
    'SPORTS_FACILITY'
  ];

  const search = async (e) => {
    if (e) e.preventDefault();
    setError(null);
    setHasSearched(true);

    // Validate minCapacity
    const minCap = minCapacity === '' ? null : Number(minCapacity);
    if (minCap !== null && (isNaN(minCap) || minCap < 1)) {
      setError('Minimum capacity must be a positive number');
      setResults([]);
      return;
    }

    try {
      setLoading(true);
      const params = {};
      
      if (type) params.type = type;
      if (minCap !== null) params.minCapacity = minCap;
      if (location) params.location = location;
      if (building) params.building = building;

      const res = await apiClient.get('/v1/facilities', { params });
      setResults(Array.isArray(res.data) ? res.data : []);
      
      if (Array.isArray(res.data) && res.data.length === 0) {
        setError('No facilities found matching your criteria');
      }
    } catch (requestError) {
      console.error('Facility search error:', requestError);
      setError(requestError.response?.data?.message || 'Failed to search facilities. Please try again.');
      setResults([]);
    } finally {
      setLoading(false);
    }
  };

  const handleSelectFacility = (facility) => {
    if (onFacilitySelect) {
      onFacilitySelect(facility);
    }
  };

  const handleClear = () => {
    setType('');
    setMinCapacity('');
    setLocation('');
    setBuilding('');
    setResults([]);
    setError(null);
    setHasSearched(false);
  };

  return (
    <div className="space-y-6">
      {/* Search Form */}
      <div className="bg-white rounded-lg shadow-md p-6">
        <h2 className="text-2xl font-bold mb-4 text-slate-900">Search Facilities</h2>
        
        <form onSubmit={search} className="space-y-4">
          {/* Filter Grid */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {/* Facility Type Filter */}
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">
                Facility Type
              </label>
              <select
                value={type}
                onChange={(e) => setType(e.target.value)}
                className="w-full px-3 py-2 border border-slate-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
              >
                <option value="">All Types</option>
                {facilityTypes.map((t) => (
                  <option key={t} value={t}>
                    {t.replace(/_/g, ' ')}
                  </option>
                ))}
              </select>
            </div>

            {/* Minimum Capacity Filter */}
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">
                Minimum Capacity
              </label>
              <input
                type="number"
                min="1"
                value={minCapacity}
                onChange={(e) => setMinCapacity(e.target.value)}
                placeholder="e.g., 50"
                className="w-full px-3 py-2 border border-slate-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
              />
            </div>

            {/* Location Filter */}
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">
                Location
              </label>
              <input
                type="text"
                value={location}
                onChange={(e) => setLocation(e.target.value)}
                placeholder="e.g., Ground Floor"
                className="w-full px-3 py-2 border border-slate-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
              />
            </div>

            {/* Building Filter */}
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">
                Building
              </label>
              <input
                type="text"
                value={building}
                onChange={(e) => setBuilding(e.target.value)}
                placeholder="e.g., Building A"
                className="w-full px-3 py-2 border border-slate-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
              />
            </div>
          </div>

          {/* Error Message */}
          {error && (
            <div className="rounded-md bg-red-50 p-4">
              <p className="text-sm font-medium text-red-800">{error}</p>
            </div>
          )}

          {/* Action Buttons */}
          <div className="flex gap-2">
            <button
              type="submit"
              disabled={loading}
              className="px-4 py-2 bg-blue-600 text-white font-medium rounded-md hover:bg-blue-700 disabled:bg-blue-400 disabled:cursor-not-allowed transition-colors"
            >
              {loading ? 'Searching...' : 'Search'}
            </button>
            <button
              type="button"
              onClick={handleClear}
              className="px-4 py-2 bg-slate-200 text-slate-700 font-medium rounded-md hover:bg-slate-300 transition-colors"
            >
              Clear
            </button>
          </div>
        </form>
      </div>

      {/* Search Results */}
      {hasSearched && (
        <div className="bg-white rounded-lg shadow-md p-6">
          <h3 className="text-xl font-bold mb-4 text-slate-900">
            Results ({results.length})
          </h3>

          {results.length === 0 ? (
            <p className="text-slate-600">No facilities found matching your criteria.</p>
          ) : (
            <div className="space-y-3">
              {results.map((facility) => (
                <div
                  key={facility.id}
                  className="border border-slate-200 rounded-lg p-4 hover:shadow-md transition-shadow cursor-pointer"
                  onClick={() => handleSelectFacility(facility)}
                >
                  <div className="flex justify-between items-start">
                    <div className="flex-1">
                      <h4 className="text-lg font-semibold text-slate-900">
                        {facility.name}
                      </h4>
                      
                      <div className="grid grid-cols-2 gap-2 mt-2 text-sm text-slate-600">
                        <div>
                          <span className="font-medium">Type:</span> {facility.type?.replace(/_/g, ' ')}
                        </div>
                        <div>
                          <span className="font-medium">Capacity:</span> {facility.capacity}
                        </div>
                        <div>
                          <span className="font-medium">Building:</span> {facility.building}
                        </div>
                        <div>
                          <span className="font-medium">Floor:</span> {facility.floor}
                        </div>
                        <div>
                          <span className="font-medium">Location:</span> {facility.location}
                        </div>
                        <div>
                          <span className="font-medium">Status:</span>{' '}
                          <span
                            className={
                              facility.status === 'ACTIVE'
                                ? 'text-green-600 font-medium'
                                : 'text-red-600 font-medium'
                            }
                          >
                            {facility.status}
                          </span>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
