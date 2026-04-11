import React, { useState } from 'react';
import { apiClient } from '../../services/apiClient';

export default function FacilitySearch() {
  const [query, setQuery] = useState('');
  const [results, setResults] = useState([]);
  const [minCapacity, setMinCapacity] = useState('');
  const [building, setBuilding] = useState('');
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);

  const search = async () => {
    setError(null);
    const minCap = minCapacity === '' ? null : Number(minCapacity);
    if (minCap !== null && (isNaN(minCap) || minCap < 0)) {
      setError('Minimum capacity must be a non-negative number');
      return;
    }

    const params = {};
      if (query && query.trim().length > 0) params.name = query.trim()
    if (minCap !== null) params.minCapacity = minCap;
    if (building) params.building = building;

    try {
      setLoading(true);
      const res = await apiClient.get('/v1/facilities', { params });
      setResults(Array.isArray(res.data) ? res.data : []);
    } catch (requestError) {
      setError('Failed to load facilities. Please try again.');
      setResults([]);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <h3>Facility Search</h3>
      <div className="mb-4">
        <label className="block">Building</label>
        <input value={building} onChange={(e) => setBuilding(e.target.value)} className="input" />
        <label className="block mt-2">Minimum capacity</label>
        <input value={minCapacity} onChange={(e) => setMinCapacity(e.target.value)} className="input" />
        {error && <div className="text-red-600">{error}</div>}
        <div className="mt-2">
          <button onClick={search} className="btn" disabled={loading}>
            {loading ? 'Searching...' : 'Search'}
          </button>
        </div>
      </div>
      <form onSubmit={e => { e.preventDefault(); search(); }}>
        <input value={query} onChange={e => setQuery(e.target.value)} placeholder="Search by name" />
        <button type="submit">Search</button>
      </form>
      <ul>
        {results.map(f => (
          <li key={f.facilityCode} className="py-1">
            <strong>{f.name}</strong> - {f.facilityCode} - {f.location} - capacity {f.capacity}
          </li>
        ))}
      </ul>
    </div>
  );
}
