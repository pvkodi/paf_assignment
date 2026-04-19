import React, { useEffect, useState, useContext } from "react";
import { AuthContext } from "../../contexts/AuthContext";
import { apiClient } from "../../services/apiClient";
import {
  fetchFacilities,
  searchFacilities,
  fetchFacilitySuggestions,
} from "./api";
import QuotaPolicySummary from "../bookings/QuotaPolicySummary";

/**
 * FacilitySearch Component
 * Provides facility discovery with filters for type, capacity, location, and building.
 */
export default function FacilitySearch({
  onFacilitySelect,
  onResultsChange,
  initialResults,
  layout = "stack",
}) {
  const { user } = useContext(AuthContext);
  const userRole = user?.roles?.[0] || "USER";
  const [name, setName] = useState("");
  const [type, setType] = useState("");
  const [minCapacity, setMinCapacity] = useState("");
  const [location, setLocation] = useState("");
  const [building, setBuilding] = useState("");

  // Suggestion states
  const [isSuggestionMode, setIsSuggestionMode] = useState(false);
  const [suggestionStart, setSuggestionStart] = useState("");
  const [suggestionEnd, setSuggestionEnd] = useState("");
  const [isSuggesting, setIsSuggesting] = useState(false);

  const [results, setResults] = useState(initialResults || []);
  const [hasSearched, setHasSearched] = useState(false);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [total, setTotal] = useState(0);

  // Pagination helpers
  const handlePayload = (payload, requestedPage = 0, requestedSize = size) => {
    const data = Array.isArray(payload?.content)
      ? payload.content
      : Array.isArray(payload)
        ? payload
        : [];
    setResults(data);
    setTotal(Number(payload?.totalElements ?? data.length));
    setPage(Number(payload?.number ?? requestedPage));
    setSize(Number(payload?.size ?? requestedSize));
    if (onResultsChange) onResultsChange(data);
  };

  const loadPage = async (
    nextPage = 0,
    nextSize = size,
    forceSearch = false,
  ) => {
    try {
      setLoading(true);
      setError(null);

      const baseParams = { page: nextPage, size: nextSize };

      const payload =
        forceSearch || hasSearched
          ? await searchFacilities({
              ...baseParams,
              ...(name ? { name } : {}),
              ...(type ? { type } : {}),
              ...(minCapacity !== ""
                ? { minCapacity: Number(minCapacity) }
                : {}),
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
    if (
      initialResults &&
      Array.isArray(initialResults) &&
      initialResults.length > 0
    ) {
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
      setError(
        requestError?.response?.data?.message ||
          "Failed to search facilities. Please try again.",
      );
      setResults([]);
      if (onResultsChange) onResultsChange([]);
    } finally {
      setIsSuggestionMode(false);
    }
  };

  const handleSuggest = async (e) => {
    if (e) e.preventDefault();
    if (!suggestionStart || !suggestionEnd) {
      setError("Please select both Start and End times for smart suggestions.");
      return;
    }
    setError(null);

    try {
      setIsSuggesting(true);
      setLoading(true);

      const payload = {
        type: type || "LECTURE_HALL",
        capacity: minCapacity === "" ? 1 : Number(minCapacity),
        start: new Date(suggestionStart).toISOString().slice(0, 19),
        end: new Date(suggestionEnd).toISOString().slice(0, 19),
        preferredBuilding: building || null,
      };

      const data = await fetchFacilitySuggestions(payload);

      // Map suggestion results to look like facility objects
      const mappedResults = (data || []).map((s) => ({
        id: s.facilityId,
        facilityId: s.facilityId, // keep both for safety
        name: s.name,
        type: s.type,
        capacity: s.capacity,
        building: s.building || "Campus Main",
        floor: "N/A", // Not in suggestion DTO
        location: "N/A", // Not in suggestion DTO
        status: "ACTIVE", // Suggester only returns active/available ones
        isSmartMatch: true,
        utilizationScore: s.utilizationScore,
        timetableStatus: s.timetableStatus,
        suggestedDate: suggestionStart.split("T")[0],
        suggestedStartTime: suggestionStart.split("T")[1],
        suggestedEndTime: suggestionEnd.split("T")[1],
      }));

      setResults(mappedResults);
      setTotal(mappedResults.length);
      setPage(0);
      setHasSearched(true);
      if (onResultsChange) onResultsChange(mappedResults);
    } catch (err) {
      console.error("Suggestion error:", err);
      setError(
        err?.response?.data?.message ||
          "The optimization engine failed to find suggestions.",
      );
      setResults([]);
    } finally {
      setIsSuggesting(false);
      setLoading(false);
    }
  };

  const handleClear = () => {
    setName("");
    setType("");
    setMinCapacity("");
    setLocation("");
    setBuilding("");
    setSuggestionStart("");
    setSuggestionEnd("");
    setIsSuggestionMode(false);
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
        {/* Quota Policy Summary at Top */}
        <div className="mb-6">
          <QuotaPolicySummary userRole={userRole} compact={false} />
        </div>

        <div className="grid grid-cols-1 md:grid-cols-[320px_1fr] gap-6">
          {/* Left: Filters (sticky) */}
          <div className="bg-white rounded-2xl shadow-sm p-6 sticky top-24 self-start">
            <h2 className="text-2xl font-bold tracking-tight mb-4 text-[#0f172a]">
              Filters
            </h2>

            <form onSubmit={search} className="space-y-4">
              <div className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-[#334155] mb-1">
                    Facility Name
                  </label>
                  <input
                    type="text"
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    placeholder="Search facility by name..."
                    className="w-full px-3 py-2 border border-[#e2e8f0] rounded-xl shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-[#334155] mb-1">
                    Facility Type
                  </label>
                  <select
                    value={type}
                    onChange={(e) => setType(e.target.value)}
                    className="w-full px-3 py-2 border border-[#e2e8f0] rounded-xl shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                  >
                    <option value="">All Types</option>
                    {facilityTypes.map((t) => (
                      <option key={t} value={t}>
                        {t.replace(/_/g, " ")}
                      </option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="block text-sm font-medium text-[#334155] mb-1">
                    Minimum Capacity
                  </label>
                  <input
                    type="number"
                    min="1"
                    value={minCapacity}
                    onChange={(e) => setMinCapacity(e.target.value)}
                    placeholder="e.g., 50"
                    className="w-full px-3 py-2 border border-[#e2e8f0] rounded-xl shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-[#334155] mb-1">
                    Location
                  </label>
                  <input
                    type="text"
                    value={location}
                    onChange={(e) => setLocation(e.target.value)}
                    placeholder="e.g., Ground Floor"
                    className="w-full px-3 py-2 border border-[#e2e8f0] rounded-xl shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-[#334155] mb-1">
                    Building
                  </label>
                  <input
                    type="text"
                    value={building}
                    onChange={(e) => setBuilding(e.target.value)}
                    placeholder="e.g., Building A"
                    className="w-full px-3 py-2 border border-[#e2e8f0] rounded-xl shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                  />
                </div>

                <div className="pt-2">
                  <button
                    type="button"
                    onClick={() => setIsSuggestionMode(!isSuggestionMode)}
                    className={`w-full flex items-center justify-center gap-2 px-4 py-2 rounded-xl font-bold text-sm transition-all duration-300 border-2 ${
                      isSuggestionMode
                        ? "bg-indigo-50 border-indigo-200 text-indigo-700"
                        : "bg-white border-dashed border-[#e2e8f0] text-[#64748b] hover:border-indigo-400 hover:text-indigo-600"
                    }`}
                  >
                    <span>
                      {isSuggestionMode
                        ? "✨ Suggestion Mode On"
                        : "✨ Try Smart Suggestions"}
                    </span>
                  </button>
                </div>

                {isSuggestionMode && (
                  <div className="space-y-4 p-4 bg-indigo-50/50 rounded-xl border border-indigo-100 animate-in slide-in-from-top-2 duration-300">
                    <p className="text-[10px] font-black text-indigo-600 uppercase tracking-widest">
                      Suggestion Parameters
                    </p>
                    <div>
                      <label className="block text-[10px] font-bold text-[#64748b] uppercase mb-1">
                        From
                      </label>
                      <input
                        type="datetime-local"
                        value={suggestionStart}
                        onChange={(e) => setSuggestionStart(e.target.value)}
                        className="w-full px-3 py-2 border border-indigo-200 rounded-xl text-xs shadow-sm bg-white"
                      />
                    </div>
                    <div>
                      <label className="block text-[10px] font-bold text-[#64748b] uppercase mb-1">
                        Until
                      </label>
                      <input
                        type="datetime-local"
                        value={suggestionEnd}
                        onChange={(e) => setSuggestionEnd(e.target.value)}
                        className="w-full px-3 py-2 border border-indigo-200 rounded-xl text-xs shadow-sm bg-white"
                      />
                    </div>
                    <button
                      type="button"
                      onClick={handleSuggest}
                      disabled={loading || isSuggesting}
                      className="w-full py-2 bg-gradient-to-r from-indigo-600 to-violet-600 text-white rounded-xl text-sm font-black shadow-lg shadow-indigo-100 hover:scale-[1.02] active:scale-95 transition-all"
                    >
                      {isSuggesting ? "Analyzing..." : "Find Optimized Match"}
                    </button>
                  </div>
                )}
              </div>

              {error && (
                <div className="rounded-xl bg-red-50 p-4">
                  <p className="text-sm font-medium text-red-800">{error}</p>
                </div>
              )}

              <div className="flex gap-2">
                <button
                  type="submit"
                  disabled={loading}
                  className="flex-1 px-4 py-2.5 bg-[#0f172a] hover:bg-[#1e293b] text-white font-semibold rounded-xl transition-all duration-200 active:scale-95 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  {loading ? "Searching..." : "🔍 Search"}
                </button>
                <button
                  type="button"
                  onClick={handleClear}
                  className="px-6 py-2.5 bg-[#f1f5f9] hover:bg-slate-200 text-[#334155] font-semibold rounded-xl transition-all duration-200 active:scale-95"
                >
                  Clear
                </button>
              </div>
            </form>
          </div>

          {/* Right: Results (main focus) - scrollable area */}
          <div className="flex flex-col">
            <div className="bg-white rounded-2xl shadow-sm">
              <div className="p-6">
                <h3 className="text-2xl font-bold tracking-tight mb-2 text-[#0f172a]">
                  Facilities ({results.length})
                </h3>
                {results.length > 0 && (
                  <p className="text-sm text-[#475569] mb-4">
                    Click <span className="font-medium">Select</span> to create
                    a booking.
                  </p>
                )}

                {results.length === 0 ? (
                  <p className="text-[#475569]">
                    No facilities found matching your criteria.
                  </p>
                ) : (
                  <div className="space-y-3">
                    {results.map((facility, idx) => (
                      <div
                        key={
                          facility.id ??
                          facility.facilityId ??
                          facility.name ??
                          idx
                        }
                        className="border border-[#e2e8f0] rounded-2xl p-4 hover:shadow-sm transition-shadow"
                      >
                        <div className="flex justify-between items-start gap-4">
                          <div className="flex-1">
                            <div className="flex items-center gap-2">
                              <h4 className="text-lg font-semibold text-[#0f172a]">
                                {facility.name}
                              </h4>
                              {facility.isSmartMatch && (
                                <span className="bg-indigo-600 text-white text-[9px] font-black px-2 py-0.5 rounded-full flex items-center animate-pulse tracking-widest uppercase">
                                  ✨ Smart Match
                                </span>
                              )}
                            </div>
                            <div className="grid grid-cols-2 gap-x-6 gap-y-1 mt-2 text-[13px] text-[#475569]">
                              <div>
                                <span className="font-medium">Type:</span>{" "}
                                {facility.type?.replace(/_/g, " ")}
                              </div>
                              <div>
                                <span className="font-medium">Capacity:</span>{" "}
                                <span className="text-[#0f172a] font-black">
                                  {facility.capacity}
                                </span>
                              </div>
                              <div>
                                <span className="font-medium">Building:</span>{" "}
                                {facility.building}
                              </div>
                              {facility.isSmartMatch ? (
                                <div className="flex items-center gap-1">
                                  <span className="font-medium">
                                    Free Period:
                                  </span>
                                  <span className="text-emerald-600 font-bold">
                                    {facility.timetableStatus}
                                  </span>
                                </div>
                              ) : (
                                <div>
                                  <span className="font-medium">Floor:</span>{" "}
                                  {facility.floor}
                                </div>
                              )}
                              <div>
                                <span className="font-medium">Location:</span>{" "}
                                {facility.location}
                              </div>
                              <div>
                                <span className="font-medium">Status:</span>{" "}
                                <span
                                  className={
                                    facility.status === "ACTIVE"
                                      ? "text-green-600 font-medium"
                                      : "text-red-600 font-medium"
                                  }
                                >
                                  {facility.status}
                                </span>
                              </div>
                            </div>

                            {facility.isSmartMatch && (
                              <div className="mt-3 bg-[#f8fafc] rounded-2xl p-2 border border-[#f1f5f9] flex items-center justify-between">
                                <div className="flex items-center gap-2">
                                  <div className="w-24 h-1.5 bg-gray-200 rounded-full overflow-hidden">
                                    <div
                                      className="h-full bg-indigo-500"
                                      style={{
                                        width: `${facility.utilizationScore}%`,
                                      }}
                                    />
                                  </div>
                                  <span className="text-[10px] text-[#94a3b8] font-bold uppercase">
                                    {Math.round(facility.utilizationScore)}%
                                    Opt.
                                  </span>
                                </div>
                                <span className="text-[10px] text-indigo-500 font-black italic">
                                  ✓ Suggested for load balance
                                </span>
                              </div>
                            )}
                          </div>

                          <button
                            type="button"
                            onClick={() => handleSelectFacility(facility)}
                            className="px-5 py-2.5 bg-[#0f172a] hover:bg-[#1e293b] text-white font-semibold rounded-xl transition-all duration-200 active:scale-95 whitespace-nowrap self-center"
                          >
                            Select
                          </button>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>

              <div className="p-4 flex items-center justify-between">
                <div className="text-sm text-[#475569]">
                  Showing {startIndex}-{endIndex} of {total}
                </div>

                <nav
                  className="inline-flex items-center space-x-2"
                  role="navigation"
                  aria-label="Pagination"
                >
                  <button
                    onClick={() => page > 0 && loadPage(page - 1, size)}
                    disabled={page <= 0}
                    className="inline-flex items-center justify-center h-8 w-8 rounded-xl bg-white border border-[#e2e8f0] hover:bg-[#f8fafc] disabled:opacity-50"
                    aria-label="Previous page"
                  >
                    <span className="text-[#334155] text-lg">‹</span>
                  </button>

                  {(() => {
                    const pages = getPageRange(page, totalPages, 5);
                    const nodes = [];
                    if (pages.length === 0) return null;
                    if (pages[0] > 0) {
                      nodes.push(
                        <button
                          key="p0"
                          onClick={() => loadPage(0, size)}
                          className="h-8 min-w-[36px] px-2 rounded-xl bg-white border border-[#e2e8f0] text-sm hover:bg-[#f8fafc]"
                        >
                          1
                        </button>,
                      );
                      if (pages[0] > 1)
                        nodes.push(
                          <span key="e1" className="px-2 text-[#94a3b8]">
                            …
                          </span>,
                        );
                    }

                    pages.forEach((p) => {
                      nodes.push(
                        <button
                          key={p}
                          onClick={() => loadPage(p, size)}
                          className={`h-8 min-w-[36px] px-2 rounded-xl text-sm ${p === page ? "bg-[#0f172a] text-white shadow" : "bg-white border border-[#e2e8f0] hover:bg-[#f8fafc]"}`}
                        >
                          {p + 1}
                        </button>,
                      );
                    });

                    if (pages[pages.length - 1] < totalPages - 1) {
                      if (pages[pages.length - 1] < totalPages - 2)
                        nodes.push(
                          <span key="e2" className="px-2 text-[#94a3b8]">
                            …
                          </span>,
                        );
                      nodes.push(
                        <button
                          key="plast"
                          onClick={() => loadPage(totalPages - 1, size)}
                          className="h-8 min-w-[36px] px-2 rounded-xl bg-white border border-[#e2e8f0] text-sm hover:bg-[#f8fafc]"
                        >
                          {totalPages}
                        </button>,
                      );
                    }

                    return nodes;
                  })()}

                  <button
                    onClick={() =>
                      page + 1 < totalPages && loadPage(page + 1, size)
                    }
                    disabled={page + 1 >= totalPages}
                    className="inline-flex items-center justify-center h-8 w-8 rounded-xl bg-white border border-[#e2e8f0] hover:bg-[#f8fafc] disabled:opacity-50"
                    aria-label="Next page"
                  >
                    <span className="text-[#334155] text-lg">›</span>
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
      {/* Quota Policy Summary at Top */}
      <div className="mb-6">
        <QuotaPolicySummary userRole={userRole} compact={false} />
      </div>

      <div className="bg-white rounded-2xl shadow-sm p-6">
        <h2 className="text-2xl font-bold tracking-tight mb-4 text-[#0f172a]">
          Search Facilities
        </h2>

        <form onSubmit={search} className="space-y-4">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-[#334155] mb-1">
                Facility Type
              </label>
              <select
                value={type}
                onChange={(e) => setType(e.target.value)}
                className="w-full px-3 py-2 border border-[#e2e8f0] rounded-xl shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
              >
                <option value="">All Types</option>
                {facilityTypes.map((t) => (
                  <option key={t} value={t}>
                    {t.replace(/_/g, " ")}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-sm font-medium text-[#334155] mb-1">
                Minimum Capacity
              </label>
              <input
                type="number"
                min="1"
                value={minCapacity}
                onChange={(e) => setMinCapacity(e.target.value)}
                placeholder="e.g., 50"
                className="w-full px-3 py-2 border border-[#e2e8f0] rounded-xl shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-[#334155] mb-1">
                Location
              </label>
              <input
                type="text"
                value={location}
                onChange={(e) => setLocation(e.target.value)}
                placeholder="e.g., Ground Floor"
                className="w-full px-3 py-2 border border-[#e2e8f0] rounded-xl shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-[#334155] mb-1">
                Building
              </label>
              <input
                type="text"
                value={building}
                onChange={(e) => setBuilding(e.target.value)}
                placeholder="e.g., Building A"
                className="w-full px-3 py-2 border border-[#e2e8f0] rounded-xl shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
              />
            </div>

            <div className="md:col-span-2 pt-2 flex flex-col gap-4">
              <button
                type="button"
                onClick={() => setIsSuggestionMode(!isSuggestionMode)}
                className={`flex items-center justify-center gap-2 px-4 py-2 rounded-xl font-bold text-sm transition-all border-2 ${
                  isSuggestionMode
                    ? "bg-indigo-50 border-indigo-200 text-indigo-700"
                    : "bg-white border-dashed border-[#e2e8f0] text-[#64748b] hover:border-indigo-400 hover:text-indigo-600"
                }`}
              >
                <span>
                  {isSuggestionMode
                    ? "✨ Smart Suggestions: Active"
                    : "✨ Tap for Smart Suggestions"}
                </span>
              </button>

              {isSuggestionMode && (
                <div className="grid grid-cols-1 md:grid-cols-3 gap-4 p-4 bg-indigo-50 border border-indigo-100 rounded-xl animate-in slide-in-from-top-2">
                  <div>
                    <label className="block text-[10px] font-black text-indigo-600 uppercase mb-1">
                      From
                    </label>
                    <input
                      type="datetime-local"
                      value={suggestionStart}
                      onChange={(e) => setSuggestionStart(e.target.value)}
                      className="w-full px-3 py-2 border border-indigo-200 rounded-xl shadow-sm text-sm"
                    />
                  </div>
                  <div>
                    <label className="block text-[10px] font-black text-indigo-600 uppercase mb-1">
                      Until
                    </label>
                    <input
                      type="datetime-local"
                      value={suggestionEnd}
                      onChange={(e) => setSuggestionEnd(e.target.value)}
                      className="w-full px-3 py-2 border border-indigo-200 rounded-xl shadow-sm text-sm"
                    />
                  </div>
                  <div className="flex items-end">
                    <button
                      type="button"
                      onClick={handleSuggest}
                      disabled={loading}
                      className="w-full py-2 bg-indigo-600 text-white rounded-xl font-bold shadow-lg hover:bg-indigo-700 transition"
                    >
                      Get Matches
                    </button>
                  </div>
                </div>
              )}
            </div>
          </div>

          {error && (
            <div className="rounded-xl bg-red-50 p-4">
              <p className="text-sm font-medium text-red-800">{error}</p>
            </div>
          )}

          <div className="flex gap-2">
            <button
              type="submit"
              disabled={loading}
              className="flex-1 px-4 py-2.5 bg-gradient-to-r from-[#0f172a] to-[#0f172a] hover:from-[#1e293b] hover:to-[#1e293b] text-white font-semibold rounded-xl shadow-sm hover:shadow-lg transition-all duration-200 active:scale-95 disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:from-[#0f172a] disabled:hover:to-[#0f172a]"
            >
              {loading ? "Searching..." : "🔍 Search"}
            </button>
            <button
              type="button"
              onClick={handleClear}
              className="px-6 py-2.5 bg-[#f1f5f9] hover:bg-slate-200 text-[#334155] font-semibold rounded-xl border border-[#e2e8f0] shadow-sm hover:shadow-sm transition-all duration-200 active:scale-95"
            >
              Clear
            </button>
          </div>
        </form>
      </div>

      {(hasSearched || results.length > 0) && (
        <div className="bg-white rounded-2xl shadow-sm">
          <div className="p-6">
            <h3 className="text-xl font-bold mb-2 text-[#0f172a]">
              Results ({results.length})
            </h3>

            {results.length > 0 && (
              <p className="text-sm text-[#475569] mb-4">
                Click the <span className="font-medium">"Select"</span> button
                for a facility to proceed with booking on the right.
              </p>
            )}
          </div>

          <div className="p-6">
            {results.length === 0 ? (
              <p className="text-[#475569]">
                No facilities found matching your criteria.
              </p>
            ) : (
              <div className="space-y-3">
                {results.map((facility, idx) => (
                  <div
                    key={
                      facility.id ?? facility.facilityId ?? facility.name ?? idx
                    }
                    className="border border-[#e2e8f0] rounded-2xl p-4 hover:shadow-sm transition-shadow"
                  >
                    <div className="flex justify-between items-start gap-4">
                      <div className="flex-1">
                        <h4 className="text-lg font-semibold text-[#0f172a]">
                          {facility.name}
                        </h4>
                        <div className="grid grid-cols-2 gap-2 mt-2 text-sm text-[#475569]">
                          <div>
                            <span className="font-medium">Type:</span>{" "}
                            {facility.type?.replace(/_/g, " ")}
                          </div>
                          <div>
                            <span className="font-medium">Capacity:</span>{" "}
                            {facility.capacity}
                          </div>
                          <div>
                            <span className="font-medium">Building:</span>{" "}
                            {facility.building}
                          </div>
                          <div>
                            <span className="font-medium">Floor:</span>{" "}
                            {facility.floor}
                          </div>
                          <div>
                            <span className="font-medium">Location:</span>{" "}
                            {facility.location}
                          </div>
                          <div>
                            <span className="font-medium">Status:</span>{" "}
                            <span
                              className={
                                facility.status === "ACTIVE"
                                  ? "text-green-600 font-medium"
                                  : "text-red-600 font-medium"
                              }
                            >
                              {facility.status}
                            </span>
                          </div>
                        </div>
                      </div>

                      <button
                        type="button"
                        onClick={() => handleSelectFacility(facility)}
                        className="px-5 py-2.5 bg-[#0f172a] hover:bg-[#1e293b] text-white font-semibold rounded-xl transition-all duration-200 active:scale-95 whitespace-nowrap self-center"
                      >
                        Select
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

          <div className="p-4 flex items-center justify-between">
            <div className="text-sm text-[#475569]">
              Showing {startIndex}-{endIndex} of {total}
            </div>

            <nav
              className="inline-flex items-center space-x-2"
              role="navigation"
              aria-label="Pagination"
            >
              <button
                onClick={() => page > 0 && loadPage(page - 1, size)}
                disabled={page <= 0}
                className="inline-flex items-center justify-center h-8 w-8 rounded-xl bg-white border border-[#e2e8f0] hover:bg-[#f8fafc] disabled:opacity-50"
                aria-label="Previous page"
              >
                <span className="text-[#334155] text-lg">‹</span>
              </button>

              {(() => {
                const pages = getPageRange(page, totalPages, 5);
                const nodes = [];
                if (pages.length === 0) return null;
                if (pages[0] > 0) {
                  nodes.push(
                    <button
                      key="p0b"
                      onClick={() => loadPage(0, size)}
                      className="h-8 min-w-[36px] px-2 rounded-xl bg-white border border-[#e2e8f0] text-sm hover:bg-[#f8fafc]"
                    >
                      1
                    </button>,
                  );
                  if (pages[0] > 1)
                    nodes.push(
                      <span key="e1b" className="px-2 text-[#94a3b8]">
                        …
                      </span>,
                    );
                }

                pages.forEach((p) => {
                  nodes.push(
                    <button
                      key={p + "b"}
                      onClick={() => loadPage(p, size)}
                      className={`h-8 min-w-[36px] px-2 rounded-xl text-sm ${p === page ? "bg-[#0f172a] text-white shadow" : "bg-white border border-[#e2e8f0] hover:bg-[#f8fafc]"}`}
                    >
                      {p + 1}
                    </button>,
                  );
                });

                if (pages[pages.length - 1] < totalPages - 1) {
                  if (pages[pages.length - 1] < totalPages - 2)
                    nodes.push(
                      <span key="e2b" className="px-2 text-[#94a3b8]">
                        …
                      </span>,
                    );
                  nodes.push(
                    <button
                      key="plastb"
                      onClick={() => loadPage(totalPages - 1, size)}
                      className="h-8 min-w-[36px] px-2 rounded-xl bg-white border border-[#e2e8f0] text-sm hover:bg-[#f8fafc]"
                    >
                      {totalPages}
                    </button>,
                  );
                }

                return nodes;
              })()}

              <button
                onClick={() =>
                  page + 1 < totalPages && loadPage(page + 1, size)
                }
                disabled={page + 1 >= totalPages}
                className="inline-flex items-center justify-center h-8 w-8 rounded-xl bg-white border border-[#e2e8f0] hover:bg-[#f8fafc] disabled:opacity-50"
                aria-label="Next page"
              >
                <span className="text-[#334155] text-lg">›</span>
              </button>
            </nav>
          </div>
        </div>
      )}
    </div>
  );
}
