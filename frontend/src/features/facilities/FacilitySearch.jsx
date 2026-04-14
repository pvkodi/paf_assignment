import React, { useState } from 'react';
import { apiClient } from '../../services/apiClient';

/**
 * FacilitySearch Component
 * Provides facility discovery with filters for type, capacity, location, and building.
 * Implements FR-004, FR-005, FR-006 from the specification.
 */
export default function FacilitySearch({ onResultsChange, initialResults }) {
  const [type, setType] = useState('');
  const [minCapacity, setMinCapacity] = useState('');
  const [location, setLocation] = useState('');
  const [building, setBuilding] = useState('');
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);

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

    // Validate minCapacity
    const minCap = minCapacity === '' ? null : Number(minCapacity);
    if (minCap !== null && (isNaN(minCap) || minCap < 1)) {
      setError('Minimum capacity must be a positive number');
      if (onResultsChange) onResultsChange([]);
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
      const data = Array.isArray(res.data) ? res.data : [];
      
      if (onResultsChange) {
        onResultsChange(data);
      }
      
      if (data.length === 0) {
        setError('No facilities found matching your criteria');
      }
    } catch (requestError) {
      console.error('Facility search error:', requestError);
      setError(requestError.response?.data?.message || 'Failed to search facilities. Please try again.');
      if (onResultsChange) onResultsChange([]);
    } finally {
      setLoading(false);
    }
  };

  const handleClear = () => {
    setType('');
    setMinCapacity('');
    setLocation('');
    setBuilding('');
    setError(null);
    if (onResultsChange) onResultsChange(initialResults || []);
  };

  return (
    <div className="bg-white rounded-md shadow-sm border border-slate-100 p-5 space-y-5">
      <div>
        <h3 className="text-lg font-semibold text-slate-900 mb-1">Filters</h3>
        <p className="text-xs text-slate-500">Narrow your search</p>
      </div>
      
      <form onSubmit={search} className="space-y-5">
        {/* Facility Type Filter */}
        <div>
          <label className="block text-sm font-medium text-slate-700 mb-2">Type</label>
          <select
            value={type}
            onChange={(e) => setType(e.target.value)}
            className="w-full px-3 py-2 border border-slate-300 rounded text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
          >
            <option value="">All Types</option>
            {facilityTypes.map((t) => (
              <option key={t} value={t}>{t.replace(/_/g, ' ')}</option>
            ))}
          </select>
        </div>

        {/* Minimum Capacity Filter */}
        <div>
          <label className="block text-sm font-medium text-slate-700 mb-2">Min Capacity</label>
          <input
            type="number"
            min="1"
            value={minCapacity}
            onChange={(e) => setMinCapacity(e.target.value)}
            placeholder="e.g., 50"
            className="w-full px-3 py-2 border border-slate-300 rounded text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
          />
        </div>

        {/* Location Filter */}
        <div>
          <label className="block text-sm font-medium text-slate-700 mb-2">Location</label>
          <input
            type="text"
            value={location}
            onChange={(e) => setLocation(e.target.value)}
            placeholder="e.g., Ground Floor"
            className="w-full px-3 py-2 border border-slate-300 rounded text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
          />
        </div>

        {/* Building Filter */}
        <div>
          <label className="block text-sm font-medium text-slate-700 mb-2">Building</label>
          <input
            type="text"
            value={building}
            onChange={(e) => setBuilding(e.target.value)}
            placeholder="e.g., Building A"
            className="w-full px-3 py-2 border border-slate-300 rounded text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
          />
        </div>

        {/* Error Message */}
        {error && (
          <div className="rounded-md bg-red-50 border border-red-100 p-3">
            <p className="text-xs text-red-700">{error}</p>
          </div>
        )}

        {/* Action Buttons */}
        <div className="flex flex-col gap-3 pt-2">
          <button
            type="submit"
            disabled={loading}
            className="w-full px-3 py-2.5 bg-indigo-600 text-white text-sm font-medium rounded hover:bg-indigo-700 disabled:opacity-50 transition"
          >
            {loading ? 'Search...' : 'Search'}
          </button>
          <button
            type="button"
            onClick={handleClear}
            className="w-full px-3 py-2.5 bg-slate-100 text-slate-700 text-sm font-medium rounded hover:bg-slate-200 transition"
          >
            Clear
          </button>
        </div>
      </form>
    </div>
  );
}
