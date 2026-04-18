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
 * BookingsList Component - Professional Senior-Level Design
 * Displays all bookings for the current user with their status, details, and approval workflow.
 * Optimized for clarity, information hierarchy, and user experience.
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

  const getStatusIcon = (status) => {
    switch (status) {
      case "APPROVED":
        return "✓";
      case "PENDING":
        return "◷";
      case "REJECTED":
        return "✕";
      case "CANCELLED":
        return "—";
      default:
        return "•";
    }
  };

  const getStatusBgColor = (status) => {
    switch (status) {
      case "APPROVED":
        return "bg-green-100/50 text-green-700 border-green-200";
      case "PENDING":
        return "bg-orange-50 text-[#49BBBB] border-orange-200";
      case "REJECTED":
        return "bg-red-50 text-red-700 border-red-200";
      case "CANCELLED":
        return "bg-slate-50 text-slate-600 border-slate-200";
      default:
        return "bg-white text-slate-700 border-slate-200";
    }
  };

  const getStatusTextColor = (status) => {
    switch (status) {
      case "APPROVED":
        return "text-green-600";
      case "PENDING":
        return "text-[#49BBBB]";
      case "REJECTED":
        return "text-red-600";
      case "CANCELLED":
        return "text-slate-500";
      default:
        return "text-slate-700";
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-slate-500">Loading bookings...</div>
      </div>
    );
  }

  return (
    <div className="space-y-8 bg-[#f8f9fa] min-h-screen p-6 md:p-8 rounded-3xl">
      {/* Header with Summary - Minimalist */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-6 pb-2">
        <div>
          <h1 className="text-4xl font-bold tracking-tight text-slate-900">My Bookings</h1>
          <p className="text-slate-500 mt-2 text-sm font-medium">
            Monitor and manage your facility reservations
          </p>
        </div>
        
        {/* Status Filter Pill Container - Modern Design */}
        <div className="bg-white p-1.5 rounded-full inline-flex border border-slate-100 shadow-[0_2px_10px_rgb(0,0,0,0.02)] overflow-x-auto whitespace-nowrap scrollbar-hide">
          {["ALL", "PENDING", "APPROVED", "CANCELLED"].map((status) => {
            const isActive = filter === status;
            const count = status === "ALL" ? bookings.length : bookings.filter((b) => b.status === status).length;

            return (
              <button
                key={status}
                onClick={() => setFilter(status)}
                className={`px-5 py-2.5 rounded-full text-sm font-semibold transition-all duration-300 ${
                  isActive
                    ? "bg-slate-900 text-white shadow-md transform scale-105"
                    : "text-slate-500 hover:text-slate-800 hover:bg-slate-50"
                }`}
              >
                {status.charAt(0) + status.slice(1).toLowerCase()} 
                <span className={`ml-2 px-2 py-0.5 rounded-full text-xs ${isActive ? 'bg-white/20 text-white' : 'bg-slate-100 text-slate-500'}`}>
                  {count}
                </span>
              </button>
            );
          })}
        </div>
      </div>

      {/* Error Messages */}
      {error && (
        <div className="rounded-2xl bg-red-50/50 border border-red-100 p-5 backdrop-blur-sm">
          <p className="text-sm font-semibold text-red-800">{error}</p>
        </div>
      )}

      {actionError && (
        <div className="rounded-2xl bg-red-50/50 border border-red-100 p-5 backdrop-blur-sm">
          <p className="text-sm font-semibold text-red-800">{actionError}</p>
        </div>
      )}

      {/* Bookings List Layout */}
      {filteredBookings.length === 0 ? (
        <div className="text-center py-20 bg-white rounded-3xl border border-slate-100 shadow-sm">
          <div className="w-20 h-20 bg-slate-50 rounded-full flex items-center justify-center mx-auto mb-4 border border-slate-100">
            <svg className="w-8 h-8 text-slate-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
            </svg>
          </div>
          <p className="text-slate-800 font-bold text-lg mb-1">No bookings found</p>
          <p className="text-slate-500 text-sm font-medium">
            {filter === "ALL" ? "You haven't made any bookings yet." : `You have no ${filter.toLowerCase()} bookings.`}
          </p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {filteredBookings.map((booking) => (
            <div
              key={booking.id}
              className="group bg-white rounded-[1.5rem] border border-slate-100 p-6 shadow-[0_4px_24px_rgb(0,0,0,0.02)] hover:shadow-[0_8px_32px_rgb(0,0,0,0.06)] transition-all duration-300 transform hover:-translate-y-1 overflow-hidden relative"
            >
              {/* Very subtle color pop at the top edge based on status */}
              <div className={`absolute top-0 left-0 w-full h-1 ${getStatusTextColor(booking.status).replace('text-', 'bg-')}`}></div>
              
              {/* Card Header */}
              <div className="flex justify-between items-start mb-5">
                <div className={`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs font-bold uppercase tracking-wider border ${getStatusBgColor(booking.status)}`}>
                  <span className="text-[10px]">{getStatusIcon(booking.status)}</span>
                  {booking.status}
                </div>
                {/* Expand Toggle */}
                <button 
                  onClick={() => setSelectedBookingId(selectedBookingId === booking.id ? null : booking.id)}
                  className="w-8 h-8 rounded-full bg-slate-50 text-slate-400 hover:bg-slate-100 hover:text-slate-800 flex items-center justify-center transition-colors border border-slate-100"
                >
                  <svg className={`w-4 h-4 transform transition-transform duration-300 ${selectedBookingId === booking.id ? 'rotate-180' : ''}`} fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
                  </svg>
                </button>
              </div>

              {/* Title & Facility */}
              <div className="mb-6">
                <h3 className="text-xl font-bold text-slate-900 mb-1 group-hover:text-slate-800 transition-colors">
                  {booking.facility?.name || "Unknown Facility"}
                </h3>
                <p className="text-slate-500 text-sm font-medium flex items-center gap-2">
                  <svg className="w-4 h-4 text-slate-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" /><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" /></svg>
                  {booking.facility?.building || "N/A"}, Fl {booking.facility?.floor || "-"}
                </p>
              </div>

              {/* Time & Date Block */}
              <div className="bg-slate-50 rounded-2xl p-4 mb-6 border border-slate-100 relative overflow-hidden">
                <div className="flex justify-between items-center relative z-10">
                  <div>
                    <p className="text-xs font-bold text-slate-400 uppercase tracking-wider mb-1">Date</p>
                    <p className="text-sm font-bold text-slate-800">{formatBookingDate(booking.bookingDate)}</p>
                  </div>
                  <div className="text-right">
                    <p className="text-xs font-bold text-slate-400 uppercase tracking-wider mb-1">Time</p>
                    <p className="text-sm font-bold text-slate-800">{booking.start_time} - {booking.end_time}</p>
                  </div>
                </div>
              </div>

              {/* Minimal Actions */}
              <div className="flex gap-3">
                {canCheckInBooking(booking) && (
                  <button
                    onClick={() => setCheckInBookingId(booking.id)}
                    className="flex-1 bg-[#49BBBB] hover:bg-[#3CA0A0] text-white px-4 py-3 rounded-xl text-sm font-bold transition-colors shadow-[0_4px_14px_rgba(73,187,187,0.3)] shadow-[#49BBBB]/20"
                  >
                    Check In Now
                  </button>
                )}
                
                {canCancelBooking(booking) && (
                  <button
                    onClick={() => handleCancelBooking(booking.id)}
                    disabled={actionLoading === booking.id}
                    className={`flex-1 bg-white hover:bg-slate-50 border border-slate-200 text-slate-600 hover:text-red-500 px-4 py-3 rounded-xl text-sm font-extrabold transition-colors disabled:opacity-50 ${!canCheckInBooking(booking) ? 'w-full' : ''}`}
                  >
                    {actionLoading === booking.id ? "Loading..." : "Cancel"}
                  </button>
                )}
                
                {!canCheckInBooking(booking) && !canCancelBooking(booking) && (
                   <button
                   onClick={() => setSelectedBookingId(selectedBookingId === booking.id ? null : booking.id)}
                   className="w-full bg-[#49BBBB] hover:bg-[#3CA0A0] text-white px-4 py-3 rounded-xl text-sm font-bold transition-all shadow-[0_4px_14px_rgba(73,187,187,0.3)]"
                 >
                   {selectedBookingId === booking.id ? "Hide Details" : "View Details"}
                 </button>
                )}
              </div>

              {/* Collapsible Details */}
              <div className={`mt-4 transform transition-all duration-300 origin-top ${selectedBookingId === booking.id ? 'opacity-100 scale-y-100 h-auto' : 'opacity-0 scale-y-0 h-0 hidden'}`}>
                <div className="pt-4 border-t border-slate-100">
                  <div className="grid grid-cols-2 gap-4 mb-2">
                    <div>
                      <p className="text-xs font-bold text-slate-400 uppercase tracking-wider mb-1">Attendees</p>
                      <p className="text-sm font-bold text-slate-800">{booking.attendees} People</p>
                    </div>
                    <div>
                      <p className="text-xs font-bold text-slate-400 uppercase tracking-wider mb-1">Purpose</p>
                      <p className="text-sm font-bold text-slate-800 truncate">{booking.purpose || "N/A"}</p>
                    </div>
                  </div>
                  <BookingDetails
                    bookingId={booking.id}
                    onClose={() => setSelectedBookingId(null)}
                    onUpdate={fetchBookings}
                    compactMode={true}
                  />
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Check In Modal */}
      {checkInBookingId && (
        <div className="fixed inset-0 bg-black bg-opacity-50 z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-lg shadow-lg p-6 max-w-md w-full">
            <h2 className="text-xl font-semibold text-slate-900 mb-4">Check In Booking</h2>
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
      )}
    </div>
  );
}
