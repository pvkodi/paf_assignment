import React, { useState } from 'react';
import axios from 'axios';

export default function FacilitySearch() {
  const [query, setQuery] = useState('');
  const [results, setResults] = useState([]);

  async function doSearch(e) {
    e.preventDefault();
    try {
      const res = await axios.get(`/api/facilities?name=${encodeURIComponent(query)}`);
      setResults(res.data || []);
    } catch (err) {
      console.error('Search failed', err);
    }
  }

  return (
    <div>
      <h3>Facility Search</h3>
      <form onSubmit={doSearch}>
        <input value={query} onChange={e => setQuery(e.target.value)} placeholder="Search by name" />
        <button type="submit">Search</button>
      </form>
      <ul>
        {results.map(f => (
          <li key={f.id}>{f.name} — {f.capacity} — {f.location}</li>
        ))}
      </ul>
    </div>
  );
}
