import React, { useState, useEffect } from "react";
import { apiClient } from "../../services/apiClient";
import BookingDetails from "./BookingDetails";
import CheckInComponent from "./CheckInComponent";
import {
  getStatusColorClasses,
  formatBookingDate,
  canCancelBooking,
  canCheckInBooking,
} from "../../utils/bookingUtils";

/**
 * BookingsList Component - Modern Minimalist Design
 * Redesigned to match a premium, clean dashboard aesthetic.
 */
export default function BookingsList() {
  const [bookings, setBookings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [selectedBookingId, setSelectedBookingId] = useState(null);
  const [checkInBookingId, setCheckInBookingId] = useState(null);
  const [actionLoading, setActionLoading] = useState(null);
  const [actionError, setActionError] = useState(null);
  const [filter, setFilter] = useState("ALL");

  useEffect(() => {
    fetchBookings();
  }, []);

  const fetchBookings = async () => {
    try {
      setLoading(true);
      const response = await apiClient.get("/v1/bookings");
      setBookings(Array.isArray(response.data) ? response.data : []);
      setError(null);
    } catch (err) {
      console.error("Failed to fetch bookings:", err);
      setError(err.response?.data?.message || "Failed to load bookings");
      setBookings([]);
    } finally {
      setLoading(false);
    }
  };

  const handleCancelBooking = async (bookingId) => {
    if (!window.confirm("Are you sure you want to cancel this booking?")) {
      return;
    }

    try {
      setActionLoading(bookingId);
      setActionError(null);
      await apiClient.post(`/v1/bookings/${bookingId}/cancel`);
      await fetchBookings();
    } catch (err) {
      console.error("Failed to cancel booking:", err);
      setActionError(err.response?.data?.message || "Failed to cancel booking");
    } finally {
      setActionLoading(null);
    }
  };

  const filteredBookings =
    filter === "ALL" ? bookings : bookings.filter((b) => b.status === filter);

  // Group bookings by status to display them in sections like the reference
  const groupedBookings = {
    PENDING: filteredBookings.filter(b => b.status === 'PENDING'),
    APPROVED: filteredBookings.filter(b => b.status === 'APPROVED'),
    COMPLETED: filteredBookings.filter(b => b.status === 'COMPLETED' || b.status === 'CHECKED_IN'),
    OTHER: filteredBookings.filter(b => ['REJECTED', 'CANCELLED'].includes(b.status))
  };

  const getStatusPill = (status) => {
    switch (status) {
      case "APPROVED":
      case "CHECKED_IN":
      case "COMPLETED":
        return (
          <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md text-xs font-medium bg-[#e8f5e9] text-[#2e7d32]">
            <span className="w-1.5 h-1.5 rounded-full bg-[#2e7d32]"></span>
            Approved
          </span>
        );
      case "PENDING":
        return (
          <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md text-xs font-medium bg-[#fff3e0] text-[#ef6c00]">
            <span className="w-1.5 h-1.5 rounded-full bg-[#ef6c00]"></span>
            Pending
          </span>
        );
      case "REJECTED":
        return (
          <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md text-xs font-medium bg-[#ffebee] text-[#c62828]">
            <span className="w-1.5 h-1.5 rounded-full bg-[#c62828]"></span>
            Rejected
          </span>
        );
      case "CANCELLED":
        return (
          <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md text-xs font-medium bg-[#f1f5f9] text-[#475569]">
            <span className="w-1.5 h-1.5 rounded-full bg-[#475569]"></span>
            Cancelled
          </span>
        );
      default:
        return (
          <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md text-xs font-medium bg-gray-100 text-gray-700">
            {status}
          </span>
        );
    }
  };

  const getTypePill = (type) => {
    // Generate a soft color based on type string length or just use a default modern pill
    return (
      <span className="inline-flex items-center px-2 py-1 rounded border border-[#f3e8ff] bg-[#faf5ff] text-[#9333ea] text-xs font-medium">
        <svg className="w-3 h-3 mr-1 opacity-70" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 002-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10"></path></svg>
        {type?.replace(/_/g, " ") || "Space"}
      </span>
    );
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-[#94a3b8] font-medium flex items-center gap-2">
          <svg className="animate-spin h-5 w-5" viewBox="0 0 24 24">
            <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" fill="none"></circle>
            <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
          </svg>
          Loading your bookings...
        </div>
      </div>
    );
  }

  const renderSection = (title, count, items, defaultOpen = true) => {
    if (items.length === 0) return null;
    return (
      <div className="mb-6">
        <div className="flex items-center gap-2 mb-3 px-2">
          <button className="text-[#94a3b8] hover:text-[#0f172a] transition-colors">
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 9l-7 7-7-7"></path></svg>
          </button>
          <h2 className="text-sm font-semibold text-[#0f172a]">{title}</h2>
          <span className="flex items-center justify-center bg-[#f1f5f9] text-[#64748b] text-[10px] font-bold h-5 w-5 rounded-full">
            {count}
          </span>
          <div className="ml-auto">
            <button className="text-[#94a3b8] hover:text-[#0f172a] transition-colors">
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 4v16m8-8H4"></path></svg>
            </button>
          </div>
        </div>

        <div className="bg-white border border-[#e2e8f0] rounded-xl overflow-hidden shadow-sm">
          <table className="w-full text-left text-sm whitespace-nowrap">
            <thead className="bg-[#f8fafc] text-[#64748b] font-medium border-b border-[#e2e8f0]">
              <tr>
                <th className="px-4 py-3 w-10 text-center">
                  <input type="checkbox" className="rounded border-gray-300 text-indigo-600 focus:ring-indigo-500" />
                </th>
                <th className="px-4 py-3 font-medium">Facility Name</th>
                <th className="px-4 py-3 font-medium">Description</th>
                <th className="px-4 py-3 font-medium">Date & Time</th>
                <th className="px-4 py-3 font-medium">Type</th>
                <th className="px-4 py-3 font-medium">Status</th>
                <th className="px-4 py-3 w-10 text-center"></th>
              </tr>
            </thead>
            <tbody className="divide-y divide-[#e2e8f0]">
              {items.map((booking) => (
                <React.Fragment key={booking.id}>
                  <tr className="hover:bg-[#f8fafc] transition-colors group cursor-pointer" onClick={() => setSelectedBookingId(selectedBookingId === booking.id ? null : booking.id)}>
                    <td className="px-4 py-4 text-center" onClick={(e) => e.stopPropagation()}>
                      <input type="checkbox" className="rounded border-gray-300 text-indigo-600 focus:ring-indigo-500" />
                    </td>
                    <td className="px-4 py-4">
                      <div className="flex items-center gap-2">
                        <div className="w-6 h-6 rounded-md bg-[#f1f5f9] border border-[#e2e8f0] flex items-center justify-center text-[#64748b]">
                          <svg className="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4"></path></svg>
                        </div>
                        <span className="font-medium text-[#0f172a]">{booking.facility?.name || "Unknown Facility"}</span>
                      </div>
                    </td>
                    <td className="px-4 py-4">
                      <span className="text-[#64748b] truncate max-w-[200px] inline-block">{booking.purpose || "No description"}</span>
                    </td>
                    <td className="px-4 py-4">
                      <div className="flex items-center gap-1.5 text-[#475569]">
                        <svg className="w-4 h-4 text-[#94a3b8]" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"></path></svg>
                        {formatBookingDate(booking.bookingDate)} <span className="text-[#94a3b8] mx-1">•</span> {booking.start_time}
                      </div>
                    </td>
                    <td className="px-4 py-4">
                      {getTypePill(booking.facility?.type)}
                    </td>
                    <td className="px-4 py-4">
                      {getStatusPill(booking.status)}
                    </td>
                    <td className="px-4 py-4 text-center">
                      <button 
                        className="text-[#94a3b8] hover:text-[#0f172a] opacity-0 group-hover:opacity-100 transition-opacity"
                        onClick={(e) => {
                          e.stopPropagation();
                          setSelectedBookingId(selectedBookingId === booking.id ? null : booking.id);
                        }}
                      >
                        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M5 12h.01M12 12h.01M19 12h.01M6 12a1 1 0 11-2 0 1 1 0 012 0zm7 0a1 1 0 11-2 0 1 1 0 012 0zm7 0a1 1 0 11-2 0 1 1 0 012 0z"></path></svg>
                      </button>
                    </td>
                  </tr>
                  
                  {/* Expanded Details Row */}
                  {selectedBookingId === booking.id && (
                    <tr className="bg-[#f8fafc] border-b border-[#e2e8f0]">
                      <td colSpan="7" className="p-0">
                        <div className="p-6">
                          <BookingDetails
                            bookingId={booking.id}
                            onClose={() => setSelectedBookingId(null)}
                            onUpdate={fetchBookings}
                          />
                          
                          {/* Quick Actions in expanded view */}
                          <div className="mt-4 pt-4 border-t border-[#e2e8f0] flex justify-end gap-3">
                            {canCheckInBooking(booking) && (
                              <button
                                onClick={() => setCheckInBookingId(booking.id)}
                                className="px-4 py-2 text-sm font-medium text-white bg-[#0f172a] hover:bg-[#1e293b] rounded-lg transition-all"
                              >
                                Check In
                              </button>
                            )}
                            {canCancelBooking(booking) && (
                              <button
                                onClick={() => handleCancelBooking(booking.id)}
                                disabled={actionLoading === booking.id}
                                className="px-4 py-2 text-sm font-medium text-[#ef4444] border border-[#ef4444] hover:bg-[#fef2f2] rounded-lg transition-all"
                              >
                                {actionLoading === booking.id ? "Cancelling..." : "Cancel Booking"}
                              </button>
                            )}
                          </div>
                        </div>
                      </td>
                    </tr>
                  )}
                </React.Fragment>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    );
  };

  return (
    <div className="w-full font-sans">
      {/* Top Header Section */}
      <div className="mb-8">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-indigo-600 text-white flex items-center justify-center font-bold text-lg shadow-sm">
            B
          </div>
          <div>
            <h1 className="text-2xl font-bold text-[#0f172a] tracking-tight">My Bookings</h1>
            <p className="text-sm text-[#64748b]">Manage and track your facility reservations</p>
          </div>
        </div>
      </div>

      {/* Tabs and Controls */}
      <div className="flex items-center gap-6 overflow-x-auto mb-6 border-b border-[#e2e8f0] pb-px">
        {["ALL", "PENDING", "APPROVED", "REJECTED", "CANCELLED"].map((status) => {
          const isActive = filter === status;
          return (
            <button
              key={status}
              onClick={() => setFilter(status)}
              className={`pb-3 text-sm font-medium border-b-2 whitespace-nowrap transition-colors ${
                isActive
                  ? "text-[#0f172a] border-[#0f172a]"
                  : "text-[#64748b] border-transparent hover:text-[#475569]"
              }`}
            >
              {status === "ALL" ? "All Bookings" : status.charAt(0) + status.slice(1).toLowerCase()}
            </button>
          );
        })}
      </div>

      {/* Errors */}
      {error && (
        <div className="mb-6 rounded-lg bg-red-50 border border-red-200 p-4 flex items-center gap-3">
          <svg className="w-5 h-5 text-red-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
          <p className="text-sm font-medium text-red-800">{error}</p>
        </div>
      )}
      {actionError && (
        <div className="mb-6 rounded-lg bg-red-50 border border-red-200 p-4 flex items-center gap-3">
          <svg className="w-5 h-5 text-red-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
          <p className="text-sm font-medium text-red-800">{actionError}</p>
        </div>
      )}

      {/* Lists */}
      {filteredBookings.length === 0 ? (
        <div className="text-center py-16 bg-white border border-[#e2e8f0] rounded-xl border-dashed">
          <div className="w-16 h-16 bg-[#f1f5f9] rounded-full flex items-center justify-center mx-auto mb-4">
            <svg className="w-8 h-8 text-[#94a3b8]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.5" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
            </svg>
          </div>
          <h3 className="text-lg font-semibold text-[#0f172a] mb-1">No bookings found</h3>
          <p className="text-[#64748b] text-sm">
            {filter === "ALL" ? "You haven't made any bookings yet." : `You have no ${filter.toLowerCase()} bookings.`}
          </p>
        </div>
      ) : filter === "ALL" ? (
        <div className="space-y-2">
          {renderSection("Pending", groupedBookings.PENDING.length, groupedBookings.PENDING)}
          {renderSection("Approved", groupedBookings.APPROVED.length, groupedBookings.APPROVED)}
          {renderSection("Completed", groupedBookings.COMPLETED.length, groupedBookings.COMPLETED)}
          {renderSection("Others", groupedBookings.OTHER.length, groupedBookings.OTHER)}
        </div>
      ) : (
        <div className="space-y-2">
          {renderSection(
            filter.charAt(0) + filter.slice(1).toLowerCase(), 
            filteredBookings.length, 
            filteredBookings
          )}
        </div>
      )}

      {/* Check In Modal */}
      {checkInBookingId && (
        <div className="fixed inset-0 bg-[#0f172a]/40 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl shadow-xl w-full max-w-md overflow-hidden animate-in fade-in zoom-in-95 duration-200">
            <div className="px-6 py-4 border-b border-[#e2e8f0] flex justify-between items-center">
              <h2 className="text-lg font-semibold text-[#0f172a]">Check In Booking</h2>
              <button onClick={() => setCheckInBookingId(null)} className="text-[#94a3b8] hover:text-[#0f172a]">
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12"></path></svg>
              </button>
            </div>
            <div className="p-6">
              <CheckInComponent
                bookingId={checkInBookingId}
                onCheckInSuccess={() => {
                  setCheckInBookingId(null);
                  fetchBookings();
                }}
                onClose={() => setCheckInBookingId(null)}
              />
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
