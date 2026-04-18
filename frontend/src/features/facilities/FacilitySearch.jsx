import React, { useEffect, useState, useContext } from "react";
import { AuthContext } from "../../contexts/AuthContext";
import { apiClient } from "../../services/apiClient";
import { fetchFacilities, searchFacilities } from "./api";
import QuotaPolicySummary from "../bookings/QuotaPolicySummary";

/**
 * FacilitySearch Component
 * Provides facility discovery with filters for type, capacity, location, and building.
 */
export default function FacilitySearch({ onFacilitySelect, onResultsChange, initialResults, layout = "stack" }) {
  const { user } = useContext(AuthContext);
  const userRole = user?.roles?.[0] || "USER";
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
      <div className="bg-transparent space-y-6">
        {/* Quota Policy Summary at Top */}
        <div>
          <QuotaPolicySummary userRole={userRole} compact={false} />
        </div>

        <div className="grid grid-cols-1 md:grid-cols-[320px_1fr] gap-8">
          {/* Left: Filters (sticky) */}
          <div className="bg-white rounded-[2rem] shadow-[0_4px_24px_rgb(0,0,0,0.02)] p-6 md:p-8 border border-slate-100 sticky top-24 self-start">
            <h2 className="text-2xl font-bold tracking-tight text-slate-900 mb-6">Filters</h2>

            <form onSubmit={search} className="space-y-6">
              <div className="space-y-5">
                <div>
                  <label className="block text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-2">Facility Type</label>
                  <select value={type} onChange={(e) => setType(e.target.value)} className="w-full rounded-xl border-none bg-slate-50 px-4 py-3 text-sm font-bold text-slate-900 focus:bg-white focus:ring-4 focus:ring-slate-100 transition-all outline-none">
                    <option value="">All Types</option>
                    {facilityTypes.map((t) => (
                      <option key={t} value={t}>{t.replace(/_/g, " ")}</option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="block text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-2">Minimum Capacity</label>
                  <input type="number" min="1" value={minCapacity} onChange={(e) => setMinCapacity(e.target.value)} placeholder="e.g. 50" className="w-full rounded-xl border-none bg-slate-50 px-4 py-3 text-sm font-bold text-slate-900 focus:bg-white focus:ring-4 focus:ring-slate-100 transition-all outline-none" />
                </div>

                <div>
                  <label className="block text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-2">Location</label>
                  <input type="text" value={location} onChange={(e) => setLocation(e.target.value)} placeholder="e.g. Ground Floor" className="w-full rounded-xl border-none bg-slate-50 px-4 py-3 text-sm font-bold text-slate-900 focus:bg-white focus:ring-4 focus:ring-slate-100 transition-all outline-none" />
                </div>

                <div>
                  <label className="block text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-2">Building</label>
                  <input type="text" value={building} onChange={(e) => setBuilding(e.target.value)} placeholder="e.g. Building A" className="w-full rounded-xl border-none bg-slate-50 px-4 py-3 text-sm font-bold text-slate-900 focus:bg-white focus:ring-4 focus:ring-slate-100 transition-all outline-none" />
                </div>
              </div>

              {error && (
                <div className="rounded-2xl bg-red-50 p-4 border border-red-100">
                  <p className="text-sm font-bold text-red-600">{error}</p>
                </div>
              )}

              <div className="flex flex-col gap-3 pt-6 border-t border-slate-100">
                <button type="submit" disabled={loading} className="w-full px-4 py-3.5 bg-[#49BBBB] hover:bg-[#3CA0A0] text-white font-bold rounded-xl shadow-[0_4px_14px_rgba(73,187,187,0.3)] transition-all transform hover:-translate-y-0.5 active:translate-y-0 disabled:opacity-50 disabled:cursor-not-allowed">
                  {loading ? "Searching..." : "Search Facilities"}
                </button>
                <button type="button" onClick={handleClear} className="w-full px-4 py-3.5 bg-white border border-slate-200 hover:bg-slate-50 text-slate-600 font-bold rounded-xl shadow-[0_2px_8px_rgb(0,0,0,0.02)] transition-all">
                  Clear Filters
                </button>
              </div>
            </form>
          </div>

          {/* Right: Results (main focus) */}
          <div className="flex flex-col">
              <div className="bg-white rounded-[2rem] shadow-[0_4px_24px_rgb(0,0,0,0.02)] border border-slate-100 flex flex-col h-full overflow-hidden">
                <div className="p-6 md:p-8 border-b border-slate-100 flex items-center justify-between">
                  <div>
                    <h3 className="text-2xl font-bold text-slate-900">Facilities</h3>
                    {results.length > 0 && (
                      <p className="text-sm font-medium text-slate-500 mt-1">Select a facility to review and book.</p>
                    )}
                  </div>
                  <div className="px-4 py-2 bg-slate-50 rounded-xl border border-slate-100">
                    <span className="text-sm font-bold text-slate-900">{results.length} found</span>
                  </div>
                </div>

                <div className="p-6 md:p-8">
                {results.length === 0 ? (
                  <div className="text-center py-16">
                     <div className="w-20 h-20 bg-slate-50 rounded-full flex items-center justify-center mx-auto mb-4 border border-slate-100">
                       <svg className="w-8 h-8 text-slate-300" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" /></svg>
                     </div>
                    <p className="text-slate-500 font-medium">No facilities match your search criteria.</p>
                  </div>
                ) : (
                  <div className="grid grid-cols-1 xl:grid-cols-2 gap-4">
                    {results.map((facility, idx) => (
                      <div key={facility.id ?? facility.facilityId ?? facility.name ?? idx} className="group border border-slate-100 rounded-2xl p-5 hover:shadow-[0_8px_30px_rgb(0,0,0,0.04)] hover:border-slate-200 transition-all bg-white flex flex-col">
                        <div className="flex justify-between items-start mb-4">
                          <div>
                            <div className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md text-[10px] font-bold uppercase tracking-wider border mb-3 bg-slate-50 text-slate-600 border-slate-200">
                              {facility.type?.replace(/_/g, " ") || 'Unknown'}
                            </div>
                            <h4 className="text-lg font-bold text-slate-900 group-hover:text-[#49BBBB] transition-colors line-clamp-1">{facility.name}</h4>
                          </div>
                          <span className={`inline-flex px-2 py-1 rounded-md text-[10px] font-bold uppercase tracking-wider ${facility.status === "ACTIVE" ? "bg-green-50 text-green-700" : "bg-red-50 text-red-700"}`}>
                            {facility.status}
                          </span>
                        </div>

                        <div className="grid grid-cols-2 gap-3 mb-6 bg-[#f8f9fa] rounded-xl p-3 flex-grow">
                          <div>
                            <p className="text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-1">Capacity</p>
                            <p className="text-sm font-bold text-slate-800">{facility.capacity}</p>
                          </div>
                          <div>
                            <p className="text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-1">Building</p>
                            <p className="text-sm font-bold text-slate-800 truncate">{facility.building}</p>
                          </div>
                          <div>
                            <p className="text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-1">Floor</p>
                            <p className="text-sm font-bold text-slate-800 truncate">{facility.floor}</p>
                          </div>
                          <div>
                            <p className="text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-1">Location</p>
                            <p className="text-sm font-bold text-slate-800 truncate">{facility.location}</p>
                          </div>
                        </div>

                        <button type="button" onClick={() => handleSelectFacility(facility)} className="w-full mt-auto px-5 py-3 bg-[#49BBBB] hover:bg-[#3CA0A0] text-white font-bold rounded-xl transition-all duration-200 active:scale-95 shadow-[0_4px_14px_rgba(73,187,187,0.3)]">
                          Select
                        </button>
                      </div>
                    ))}
                  </div>
                )}
              </div>

              <div className="p-6 border-t border-slate-100 flex flex-col sm:flex-row items-center justify-between gap-4 mt-auto">
                <div className="text-sm font-bold text-slate-400 uppercase tracking-wider">Showing {startIndex}-{endIndex} of {total}</div>

                <nav className="inline-flex items-center space-x-1" role="navigation" aria-label="Pagination">
                  <button onClick={() => page > 0 && loadPage(page - 1, size)} disabled={page <= 0} className="inline-flex items-center justify-center h-10 w-10 rounded-xl bg-white border border-slate-200 hover:bg-slate-50 disabled:opacity-50 transition-all font-bold text-slate-600" aria-label="Previous page">
                    ‹
                  </button>

                  {(() => {
                    const pages = getPageRange(page, totalPages, 5);
                    const nodes = [];
                    if (pages.length === 0) return null;
                    if (pages[0] > 0) {
                      nodes.push(
                        <button key="p0" onClick={() => loadPage(0, size)} className="h-10 min-w-[40px] px-2 rounded-xl bg-white border border-slate-200 text-sm font-bold text-slate-600 hover:bg-slate-50 transition-all">1</button>,
                      );
                      if (pages[0] > 1) nodes.push(<span key="e1" className="px-2 text-slate-400 font-bold">…</span>);
                    }

                    pages.forEach((p) => {
                      nodes.push(
                        <button key={p} onClick={() => loadPage(p, size)} className={`h-10 min-w-[40px] px-2 rounded-xl text-sm font-bold transition-all ${p === page ? 'bg-[#49BBBB] border border-[#49BBBB] text-white shadow-md' : 'bg-white border border-slate-200 text-slate-600 hover:bg-slate-50'}`}>
                          {p + 1}
                        </button>,
                      );
                    });

                    if (pages[pages.length - 1] < totalPages - 1) {
                      if (pages[pages.length - 1] < totalPages - 2) nodes.push(<span key="e2" className="px-2 text-slate-400 font-bold">…</span>);
                      nodes.push(<button key="plast" onClick={() => loadPage(totalPages - 1, size)} className="h-10 min-w-[40px] px-2 rounded-xl bg-white border border-slate-200 text-sm font-bold text-slate-600 hover:bg-slate-50 transition-all">{totalPages}</button>);
                    }

                    return nodes;
                  })()}

                  <button onClick={() => (page + 1) < totalPages && loadPage(page + 1, size)} disabled={(page + 1) >= totalPages} className="inline-flex items-center justify-center h-10 w-10 rounded-xl bg-white border border-slate-200 hover:bg-slate-50 disabled:opacity-50 transition-all font-bold text-slate-600" aria-label="Next page">
                    ›
                  </button>
                </nav>
              </div>
            </div>
          </div>
        </div>
      </div>
    );
  }

  // fallback to original stacked layout modernized
  return (
    <div className="space-y-6">
      {/* Quota Policy Summary at Top */}
      <div>
        <QuotaPolicySummary userRole={userRole} compact={false} />
      </div>

      <div className="bg-white rounded-[2rem] shadow-[0_4px_24px_rgb(0,0,0,0.02)] p-6 md:p-8 border border-slate-100">
        <h2 className="text-2xl font-bold mb-6 tracking-tight text-slate-900">Search Facilities</h2>

        <form onSubmit={search} className="space-y-6">
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
            <div>
              <label className="block text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-2">Facility Type</label>
              <select value={type} onChange={(e) => setType(e.target.value)} className="w-full rounded-xl border-none bg-slate-50 px-4 py-3 text-sm font-bold text-slate-900 focus:bg-white focus:ring-4 focus:ring-slate-100 transition-all outline-none">
                <option value="">All Types</option>
                {facilityTypes.map((t) => (
                  <option key={t} value={t}>{t.replace(/_/g, " ")}</option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-2">Minimum Capacity</label>
              <input type="number" min="1" value={minCapacity} onChange={(e) => setMinCapacity(e.target.value)} placeholder="e.g. 50" className="w-full rounded-xl border-none bg-slate-50 px-4 py-3 text-sm font-bold text-slate-900 focus:bg-white focus:ring-4 focus:ring-slate-100 transition-all outline-none" />
            </div>

            <div>
              <label className="block text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-2">Location</label>
              <input type="text" value={location} onChange={(e) => setLocation(e.target.value)} placeholder="e.g. Ground Floor" className="w-full rounded-xl border-none bg-slate-50 px-4 py-3 text-sm font-bold text-slate-900 focus:bg-white focus:ring-4 focus:ring-slate-100 transition-all outline-none" />
            </div>

            <div>
              <label className="block text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-2">Building</label>
              <input type="text" value={building} onChange={(e) => setBuilding(e.target.value)} placeholder="e.g. Building A" className="w-full rounded-xl border-none bg-slate-50 px-4 py-3 text-sm font-bold text-slate-900 focus:bg-white focus:ring-4 focus:ring-slate-100 transition-all outline-none" />
            </div>
          </div>

          {error && (
            <div className="rounded-2xl bg-red-50 p-4 border border-red-100">
              <p className="text-sm font-bold text-red-600">{error}</p>
            </div>
          )}

          <div className="flex gap-3 pt-4">
            <button type="submit" disabled={loading} className="px-8 py-3.5 bg-[#49BBBB] hover:bg-[#3CA0A0] text-white font-bold rounded-xl shadow-[0_4px_14px_rgba(73,187,187,0.3)] transition-all transform hover:-translate-y-0.5 active:translate-y-0 disabled:opacity-50 disabled:cursor-not-allowed">
              {loading ? "Searching..." : "Search"}
            </button>
            <button type="button" onClick={handleClear} className="px-6 py-3.5 bg-white border border-slate-200 hover:bg-slate-50 text-slate-600 font-bold rounded-xl shadow-[0_2px_8px_rgb(0,0,0,0.02)] transition-all">
              Clear
            </button>
          </div>
        </form>
      </div>

      {(hasSearched || results.length > 0) && (
        <div className="bg-white rounded-[2rem] shadow-[0_4px_24px_rgb(0,0,0,0.02)] border border-slate-100 overflow-hidden">
          <div className="p-6 md:p-8 border-b border-slate-100 flex items-center justify-between">
            <div>
              <h3 className="text-2xl font-bold text-slate-900">Results</h3>
              {results.length > 0 && (
                <p className="text-sm font-medium text-slate-500 mt-1">Select a facility to review and book.</p>
              )}
            </div>
            <div className="px-4 py-2 bg-slate-50 rounded-xl border border-slate-100">
              <span className="text-sm font-bold text-slate-900">{results.length} found</span>
            </div>
          </div>

          <div className="p-6 md:p-8 bg-[#f8f9fa]">
            {results.length === 0 ? (
              <div className="text-center py-16 bg-white rounded-2xl border border-slate-100 shadow-sm">
                <div className="w-20 h-20 bg-slate-50 rounded-full flex items-center justify-center mx-auto mb-4 border border-slate-100">
                  <svg className="w-8 h-8 text-slate-300" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" /></svg>
                </div>
                <p className="text-slate-500 font-medium">No facilities match your search criteria.</p>
              </div>
            ) : (
              <div className="grid grid-cols-1 lg:grid-cols-2 lg:grid-cols-3 gap-6">
                {results.map((facility, idx) => (
                  <div key={facility.id ?? facility.facilityId ?? facility.name ?? idx} className="group border border-slate-100 rounded-2xl p-5 hover:shadow-[0_8px_30px_rgb(0,0,0,0.04)] hover:border-slate-200 transition-all bg-white flex flex-col">
                    <div className="flex flex-col h-full">
                      <div className="flex justify-between items-start mb-4">
                        <div>
                          <div className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md text-[10px] font-bold uppercase tracking-wider border mb-3 bg-slate-50 text-slate-600 border-slate-200">
                            {facility.type?.replace(/_/g, " ") || 'Unknown'}
                          </div>
                          <h4 className="text-lg font-bold text-slate-900 group-hover:text-[#49BBBB] transition-colors line-clamp-1">{facility.name}</h4>
                        </div>
                        <span className={`inline-flex px-2 py-1 rounded-md text-[10px] font-bold uppercase tracking-wider ${facility.status === "ACTIVE" ? "bg-green-50 text-green-700" : "bg-red-50 text-red-700"}`}>
                          {facility.status}
                        </span>
                      </div>

                      <div className="grid grid-cols-2 gap-3 mb-6 bg-[#f8f9fa] rounded-xl p-3 flex-grow">
                        <div>
                          <p className="text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-1">Capacity</p>
                          <p className="text-sm font-bold text-slate-800">{facility.capacity}</p>
                        </div>
                        <div>
                          <p className="text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-1">Building</p>
                          <p className="text-sm font-bold text-slate-800 truncate">{facility.building}</p>
                        </div>
                        <div>
                          <p className="text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-1">Floor</p>
                          <p className="text-sm font-bold text-slate-800 truncate">{facility.floor}</p>
                        </div>
                        <div>
                          <p className="text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-1">Location</p>
                          <p className="text-sm font-bold text-slate-800 truncate">{facility.location}</p>
                        </div>
                      </div>

                      <button type="button" onClick={() => handleSelectFacility(facility)} className="w-full mt-auto px-5 py-3 bg-[#49BBBB] hover:bg-[#3CA0A0] text-white font-bold rounded-xl transition-all duration-200 active:scale-95 shadow-[0_4px_14px_rgba(73,187,187,0.3)]">
                        Select
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

          <div className="p-6 border-t border-slate-100 flex flex-col sm:flex-row items-center justify-between gap-4">
            <div className="text-sm font-bold text-slate-400 uppercase tracking-wider">Showing {startIndex}-{endIndex} of {total}</div>

            <nav className="inline-flex items-center space-x-1" role="navigation" aria-label="Pagination">
              <button onClick={() => page > 0 && loadPage(page - 1, size)} disabled={page <= 0} className="inline-flex items-center justify-center h-10 w-10 rounded-xl bg-white border border-slate-200 hover:bg-slate-50 disabled:opacity-50 transition-all font-bold text-slate-600" aria-label="Previous page">
                ‹
              </button>

              {(() => {
                const pages = getPageRange(page, totalPages, 5);
                const nodes = [];
                if (pages.length === 0) return null;
                if (pages[0] > 0) {
                  nodes.push(
                    <button key="p0b" onClick={() => loadPage(0, size)} className="h-10 min-w-[40px] px-2 rounded-xl bg-white border border-slate-200 text-sm font-bold text-slate-600 hover:bg-slate-50 transition-all">1</button>,
                  );
                  if (pages[0] > 1) nodes.push(<span key="e1b" className="px-2 text-slate-400 font-bold">…</span>);
                }

                pages.forEach((p) => {
                  nodes.push(
                    <button key={p + 'b'} onClick={() => loadPage(p, size)} className={`h-10 min-w-[40px] px-2 rounded-xl text-sm font-bold transition-all ${p === page ? 'bg-[#49BBBB] border border-[#49BBBB] text-white shadow-md' : 'bg-white border border-slate-200 text-slate-600 hover:bg-slate-50'}`}>
                      {p + 1}
                    </button>,
                  );
                });

                if (pages[pages.length - 1] < totalPages - 1) {
                  if (pages[pages.length - 1] < totalPages - 2) nodes.push(<span key="e2b" className="px-2 text-slate-400 font-bold">…</span>);
                  nodes.push(<button key="plastb" onClick={() => loadPage(totalPages - 1, size)} className="h-10 min-w-[40px] px-2 rounded-xl bg-white border border-slate-200 text-sm font-bold text-slate-600 hover:bg-slate-50 transition-all">{totalPages}</button>);
                }

                return nodes;
              })()}

              <button onClick={() => (page + 1) < totalPages && loadPage(page + 1, size)} disabled={(page + 1) >= totalPages} className="inline-flex items-center justify-center h-10 w-10 rounded-xl bg-white border border-slate-200 hover:bg-slate-50 disabled:opacity-50 transition-all font-bold text-slate-600" aria-label="Next page">
                ›
              </button>
            </nav>
          </div>
        </div>
      )}
    </div>
  );
}
