import React, { useEffect, useState } from "react";
import { apiClient } from "../../services/apiClient";
import { fetchFacilities, searchFacilities } from "./api";

/**
 * FacilitySearch Component
 * Provides facility discovery with filters for type, capacity, location, and building.
 */
export default function FacilitySearch({ onFacilitySelect, onResultsChange, initialResults, layout = "stack" }) {
  const [type, setType] = useState("");
  const [minCapacity, setMinCapacity] = useState("");
  const [location, setLocation] = useState("");
  const [building, setBuilding] = useState("");
  const [results, setResults] = useState(initialResults || []);
  const [hasSearched, setHasSearched] = useState(false);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [total, setTotal] = useState(0);

  // Pagination helpers
  const handlePayload = (payload, requestedPage = 0, requestedSize = size) => {
    const data = Array.isArray(payload?.content) ? payload.content : Array.isArray(payload) ? payload : [];
    setResults(data);
    setTotal(Number(payload?.totalElements ?? data.length));
    setPage(Number(payload?.number ?? requestedPage));
    setSize(Number(payload?.size ?? requestedSize));
    if (onResultsChange) onResultsChange(data);
  };

  const loadPage = async (nextPage = 0, nextSize = size, forceSearch = false) => {
    try {
      setLoading(true);
      setError(null);

      const baseParams = { page: nextPage, size: nextSize };

      const payload = (forceSearch || hasSearched)
        ? await searchFacilities({
            ...baseParams,
            ...(type ? { type } : {}),
            ...(minCapacity !== "" ? { minCapacity: Number(minCapacity) } : {}),
            ...(location ? { location } : {}),
            ...(building ? { building } : {}),
          })
        : await fetchFacilities(baseParams);

      handlePayload(payload, nextPage, nextSize);
    } catch (e) {
      console.error("Failed to load facilities page:", e);
      setError("Failed to load facilities");
      setResults([]);
      setTotal(0);
    } finally {
      setLoading(false);
    }
  };

  // Load default facilities when the component mounts (show page 0)
  useEffect(() => {
    if (initialResults && Array.isArray(initialResults) && initialResults.length > 0) {
      setResults(initialResults);
      setTotal(initialResults.length);
      setPage(0);
      setSize(initialResults.length);
      return;
    }

    let cancelled = false;

    const doLoad = async () => {
      try {
        setLoading(true);
        const payload = await fetchFacilities({ page: 0, size });
        if (!cancelled) {
          handlePayload(payload, 0, size);
        }
      } catch (e) {
        if (!cancelled) setError("Failed to load facilities");
      } finally {
        if (!cancelled) setLoading(false);
      }
    };

    void doLoad();

    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [initialResults, onResultsChange]);

  const facilityTypes = [
    "LECTURE_HALL",
    "LAB",
    "MEETING_ROOM",
    "AUDITORIUM",
    "EQUIPMENT",
    "SPORTS_FACILITY",
  ];

  const search = async (e) => {
    if (e) e.preventDefault();
    setError(null);

    const minCap = minCapacity === "" ? null : Number(minCapacity);
    if (minCap !== null && (isNaN(minCap) || minCap < 1)) {
      setError("Minimum capacity must be a positive number");
      setResults([]);
      if (onResultsChange) onResultsChange([]);
      return;
    }

    try {
      setHasSearched(true);
      await loadPage(0, size, true);
    } catch (requestError) {
      console.warn("Facility search error:", requestError);
      setError(requestError?.response?.data?.message || "Failed to search facilities. Please try again.");
      setResults([]);
      if (onResultsChange) onResultsChange([]);
    }
  };

  const handleClear = () => {
    setType("");
    setMinCapacity("");
    setLocation("");
    setBuilding("");
    setResults(initialResults || []);
    setHasSearched(false);
    setError(null);
    setPage(0);
    setSize(10);
    setTotal(initialResults ? initialResults.length : 0);
    if (onResultsChange) onResultsChange(initialResults || []);
    if (!initialResults) void loadPage(0, 10, false);
  };

  const handleSelectFacility = (f) => {
    if (onFacilitySelect) onFacilitySelect(f);
  };

  const totalPages = Math.max(1, Math.ceil(total / size));
  const startIndex = total === 0 ? 0 : page * size + 1;
  const endIndex = Math.min(total, (page + 1) * size);

  const getPageRange = (current, totalP, maxButtons = 5) => {
    const n = Math.max(0, Number(totalP) || 0);
    if (n <= 1) return n === 1 ? [0] : [];
    const m = Math.max(1, Math.floor(maxButtons));
    if (n <= m) return Array.from({ length: n }, (_, i) => i);

    const half = Math.floor(m / 2);
    let start = Math.max(0, current - half);
    let end = start + m - 1;
    if (end > n - 1) {
      end = n - 1;
      start = Math.max(0, end - m + 1);
    }
    return Array.from({ length: end - start + 1 }, (_, i) => start + i);
  };

  // Layout: 'stack' - original vertical stacking; 'columns' - left filters, right results
  if (layout === "columns") {
    return (
      <div className="bg-transparent">
        <div className="grid grid-cols-1 md:grid-cols-[320px_1fr] gap-6">
          {/* Left: Filters (sticky) */}
          <div className="bg-white rounded-lg shadow-md p-6 sticky top-24 self-start">
            <h2 className="text-2xl font-bold mb-4 text-slate-900">Filters</h2>

            <form onSubmit={search} className="space-y-4">
              <div className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-slate-700 mb-1">Facility Type</label>
                  <select value={type} onChange={(e) => setType(e.target.value)} className="w-full px-3 py-2 border border-slate-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500">
                    <option value="">All Types</option>
                    {facilityTypes.map((t) => (
                      <option key={t} value={t}>{t.replace(/_/g, " ")}</option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="block text-sm font-medium text-slate-700 mb-1">Minimum Capacity</label>
                  <input type="number" min="1" value={minCapacity} onChange={(e) => setMinCapacity(e.target.value)} placeholder="e.g., 50" className="w-full px-3 py-2 border border-slate-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500" />
                </div>

                <div>
                  <label className="block text-sm font-medium text-slate-700 mb-1">Location</label>
                  <input type="text" value={location} onChange={(e) => setLocation(e.target.value)} placeholder="e.g., Ground Floor" className="w-full px-3 py-2 border border-slate-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500" />
                </div>

                <div>
                  <label className="block text-sm font-medium text-slate-700 mb-1">Building</label>
                  <input type="text" value={building} onChange={(e) => setBuilding(e.target.value)} placeholder="e.g., Building A" className="w-full px-3 py-2 border border-slate-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500" />
                </div>
              </div>

              {error && (
                <div className="rounded-md bg-red-50 p-4">
                  <p className="text-sm font-medium text-red-800">{error}</p>
                </div>
              )}

              <div className="flex gap-2">
                <button type="submit" disabled={loading} className="px-4 py-2 bg-blue-600 text-white font-medium rounded-md hover:bg-blue-700 disabled:bg-blue-400 disabled:cursor-not-allowed transition-colors">{loading ? "Searching..." : "Search"}</button>
                <button type="button" onClick={handleClear} className="px-4 py-2 bg-slate-200 text-slate-700 font-medium rounded-md hover:bg-slate-300 transition-colors">Clear</button>
              </div>
            </form>
          </div>

          {/* Right: Results (main focus) - scrollable area */}
          <div className="flex flex-col">
              <div className="bg-white rounded-lg shadow-md">
                <div className="p-6">
                <h3 className="text-2xl font-bold mb-2 text-slate-900">Facilities ({results.length})</h3>
                {results.length > 0 && (
                  <p className="text-sm text-slate-600 mb-4">Click <span className="font-medium">Select</span> to create a booking.</p>
                )}

                {results.length === 0 ? (
                  <p className="text-slate-600">No facilities found matching your criteria.</p>
                ) : (
                  <div className="space-y-3">
                    {results.map((facility, idx) => (
                      <div key={facility.id ?? facility.facilityId ?? facility.name ?? idx} className="border border-slate-200 rounded-lg p-4 hover:shadow-md transition-shadow">
                        <div className="flex justify-between items-start gap-4">
                          <div className="flex-1">
                            <h4 className="text-lg font-semibold text-slate-900">{facility.name}</h4>
                            <div className="grid grid-cols-2 gap-2 mt-2 text-sm text-slate-600">
                              <div><span className="font-medium">Type:</span> {facility.type?.replace(/_/g, " ")}</div>
                              <div><span className="font-medium">Capacity:</span> {facility.capacity}</div>
                              <div><span className="font-medium">Building:</span> {facility.building}</div>
                              <div><span className="font-medium">Floor:</span> {facility.floor}</div>
                              <div><span className="font-medium">Location:</span> {facility.location}</div>
                              <div><span className="font-medium">Status:</span> <span className={facility.status === "ACTIVE" ? "text-green-600 font-medium" : "text-red-600 font-medium"}>{facility.status}</span></div>
                            </div>
                          </div>

                          <button type="button" onClick={() => handleSelectFacility(facility)} className="px-4 py-2 bg-blue-600 text-white font-medium rounded-md hover:bg-blue-700 transition-colors whitespace-nowrap self-center">Select</button>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>

              <div className="p-4 flex items-center justify-between">
                <div className="text-sm text-slate-600">Showing {startIndex}-{endIndex} of {total}</div>

                <nav className="inline-flex items-center space-x-2" role="navigation" aria-label="Pagination">
                  <button onClick={() => page > 0 && loadPage(page - 1, size)} disabled={page <= 0} className="inline-flex items-center justify-center h-8 w-8 rounded-md bg-white border border-slate-200 hover:bg-slate-50 disabled:opacity-50" aria-label="Previous page">
                    <span className="text-slate-700 text-lg">‹</span>
                  </button>

                  {(() => {
                    const pages = getPageRange(page, totalPages, 5);
                    const nodes = [];
                    if (pages.length === 0) return null;
                    if (pages[0] > 0) {
                      nodes.push(
                        <button key="p0" onClick={() => loadPage(0, size)} className="h-8 min-w-[36px] px-2 rounded-md bg-white border border-slate-200 text-sm hover:bg-slate-50">1</button>,
                      );
                      if (pages[0] > 1) nodes.push(<span key="e1" className="px-2 text-slate-400">…</span>);
                    }

                    pages.forEach((p) => {
                      nodes.push(
                        <button key={p} onClick={() => loadPage(p, size)} className={`h-8 min-w-[36px] px-2 rounded-md text-sm ${p === page ? 'bg-slate-900 text-white shadow' : 'bg-white border border-slate-200 hover:bg-slate-50'}`}>
                          {p + 1}
                        </button>,
                      );
                    });

                    if (pages[pages.length - 1] < totalPages - 1) {
                      if (pages[pages.length - 1] < totalPages - 2) nodes.push(<span key="e2" className="px-2 text-slate-400">…</span>);
                      nodes.push(<button key="plast" onClick={() => loadPage(totalPages - 1, size)} className="h-8 min-w-[36px] px-2 rounded-md bg-white border border-slate-200 text-sm hover:bg-slate-50">{totalPages}</button>);
                    }

                    return nodes;
                  })()}

                  <button onClick={() => (page + 1) < totalPages && loadPage(page + 1, size)} disabled={(page + 1) >= totalPages} className="inline-flex items-center justify-center h-8 w-8 rounded-md bg-white border border-slate-200 hover:bg-slate-50 disabled:opacity-50" aria-label="Next page">
                    <span className="text-slate-700 text-lg">›</span>
                  </button>
                </nav>
              </div>
            </div>
          </div>
        </div>
      </div>
    );
  }

  // fallback to original stacked layout
  return (
    <div className="space-y-6">
      <div className="bg-white rounded-lg shadow-md p-6">
        <h2 className="text-2xl font-bold mb-4 text-slate-900">Search Facilities</h2>

        <form onSubmit={search} className="space-y-4">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">Facility Type</label>
              <select value={type} onChange={(e) => setType(e.target.value)} className="w-full px-3 py-2 border border-slate-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500">
                <option value="">All Types</option>
                {facilityTypes.map((t) => (
                  <option key={t} value={t}>{t.replace(/_/g, " ")}</option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">Minimum Capacity</label>
              <input type="number" min="1" value={minCapacity} onChange={(e) => setMinCapacity(e.target.value)} placeholder="e.g., 50" className="w-full px-3 py-2 border border-slate-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500" />
            </div>

            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">Location</label>
              <input type="text" value={location} onChange={(e) => setLocation(e.target.value)} placeholder="e.g., Ground Floor" className="w-full px-3 py-2 border border-slate-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500" />
            </div>

            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">Building</label>
              <input type="text" value={building} onChange={(e) => setBuilding(e.target.value)} placeholder="e.g., Building A" className="w-full px-3 py-2 border border-slate-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500" />
            </div>
          </div>

          {error && (
            <div className="rounded-md bg-red-50 p-4">
              <p className="text-sm font-medium text-red-800">{error}</p>
            </div>
          )}

          <div className="flex gap-2">
            <button type="submit" disabled={loading} className="px-4 py-2 bg-blue-600 text-white font-medium rounded-md hover:bg-blue-700 disabled:bg-blue-400 disabled:cursor-not-allowed transition-colors">{loading ? "Searching..." : "Search"}</button>
            <button type="button" onClick={handleClear} className="px-4 py-2 bg-slate-200 text-slate-700 font-medium rounded-md hover:bg-slate-300 transition-colors">Clear</button>
          </div>
        </form>
      </div>

      {(hasSearched || results.length > 0) && (
        <div className="bg-white rounded-lg shadow-md">
          <div className="p-6">
            <h3 className="text-xl font-bold mb-2 text-slate-900">Results ({results.length})</h3>

            {results.length > 0 && (
              <p className="text-sm text-slate-600 mb-4">Click the <span className="font-medium">"Select"</span> button for a facility to proceed with booking on the right.</p>
            )}
          </div>

          <div className="p-6">
            {results.length === 0 ? (
              <p className="text-slate-600">No facilities found matching your criteria.</p>
            ) : (
              <div className="space-y-3">
                {results.map((facility, idx) => (
                  <div key={facility.id ?? facility.facilityId ?? facility.name ?? idx} className="border border-slate-200 rounded-lg p-4 hover:shadow-md transition-shadow">
                    <div className="flex justify-between items-start gap-4">
                      <div className="flex-1">
                        <h4 className="text-lg font-semibold text-slate-900">{facility.name}</h4>
                        <div className="grid grid-cols-2 gap-2 mt-2 text-sm text-slate-600">
                          <div><span className="font-medium">Type:</span> {facility.type?.replace(/_/g, " ")}</div>
                          <div><span className="font-medium">Capacity:</span> {facility.capacity}</div>
                          <div><span className="font-medium">Building:</span> {facility.building}</div>
                          <div><span className="font-medium">Floor:</span> {facility.floor}</div>
                          <div><span className="font-medium">Location:</span> {facility.location}</div>
                          <div><span className="font-medium">Status:</span> <span className={facility.status === "ACTIVE" ? "text-green-600 font-medium" : "text-red-600 font-medium"}>{facility.status}</span></div>
                        </div>
                      </div>

                      <button type="button" onClick={() => handleSelectFacility(facility)} className="px-4 py-2 bg-blue-600 text-white font-medium rounded-md hover:bg-blue-700 transition-colors whitespace-nowrap self-center">Select</button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

          <div className="p-4 flex items-center justify-between">
            <div className="text-sm text-slate-600">Showing {startIndex}-{endIndex} of {total}</div>

            <nav className="inline-flex items-center space-x-2" role="navigation" aria-label="Pagination">
              <button onClick={() => page > 0 && loadPage(page - 1, size)} disabled={page <= 0} className="inline-flex items-center justify-center h-8 w-8 rounded-md bg-white border border-slate-200 hover:bg-slate-50 disabled:opacity-50" aria-label="Previous page">
                <span className="text-slate-700 text-lg">‹</span>
              </button>

              {(() => {
                const pages = getPageRange(page, totalPages, 5);
                const nodes = [];
                if (pages.length === 0) return null;
                if (pages[0] > 0) {
                  nodes.push(
                    <button key="p0b" onClick={() => loadPage(0, size)} className="h-8 min-w-[36px] px-2 rounded-md bg-white border border-slate-200 text-sm hover:bg-slate-50">1</button>,
                  );
                  if (pages[0] > 1) nodes.push(<span key="e1b" className="px-2 text-slate-400">…</span>);
                }

                pages.forEach((p) => {
                  nodes.push(
                    <button key={p + 'b'} onClick={() => loadPage(p, size)} className={`h-8 min-w-[36px] px-2 rounded-md text-sm ${p === page ? 'bg-slate-900 text-white shadow' : 'bg-white border border-slate-200 hover:bg-slate-50'}`}>
                      {p + 1}
                    </button>,
                  );
                });

                if (pages[pages.length - 1] < totalPages - 1) {
                  if (pages[pages.length - 1] < totalPages - 2) nodes.push(<span key="e2b" className="px-2 text-slate-400">…</span>);
                  nodes.push(<button key="plastb" onClick={() => loadPage(totalPages - 1, size)} className="h-8 min-w-[36px] px-2 rounded-md bg-white border border-slate-200 text-sm hover:bg-slate-50">{totalPages}</button>);
                }

                return nodes;
              })()}

              <button onClick={() => (page + 1) < totalPages && loadPage(page + 1, size)} disabled={(page + 1) >= totalPages} className="inline-flex items-center justify-center h-8 w-8 rounded-md bg-white border border-slate-200 hover:bg-slate-50 disabled:opacity-50" aria-label="Next page">
                <span className="text-slate-700 text-lg">›</span>
              </button>
            </nav>
          </div>
        </div>
      )}
    </div>
  );
}
