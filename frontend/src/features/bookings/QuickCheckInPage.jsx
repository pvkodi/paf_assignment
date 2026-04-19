import React, { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { apiClient } from "../../services/apiClient";
import CheckInComponent from "./CheckInComponent";

/**
 * Quick Check-In Page Component
 *
 * Accessible via QR code URL: /check-in/booking/:bookingId
 * Displays check-in UI for a specific booking
 * Auto-redirects to login if not authenticated
 */
export default function QuickCheckInPage() {
  const { bookingId } = useParams();
  const navigate = useNavigate();
  const [booking, setBooking] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    fetchBooking();
  }, [bookingId]);

  const fetchBooking = async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await apiClient.get(`/v1/bookings/${bookingId}`);
      setBooking(response.data);
    } catch (err) {
      console.error("Failed to load booking:", err);
      setError(
        err.response?.data?.message ||
          "Failed to load booking. The booking may not exist or you may not have permission to access it.",
      );
    } finally {
      setLoading(false);
    }
  };

  const handleCheckInSuccess = () => {
    // Redirect to bookings list or success page after check-in
    setTimeout(() => {
      navigate("/my-bookings");
    }, 2000);
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen bg-[#f8fafc]">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-2 border-indigo-200 border-t-indigo-600 mx-auto mb-4"></div>
          <p className="text-[#64748b] font-medium">Loading booking...</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex items-center justify-center min-h-screen bg-[#f8fafc] p-4">
        <div className="bg-white rounded-2xl border border-[#e2e8f0] shadow-sm p-8 max-w-md text-center">
          <div className="w-16 h-16 bg-[#fef2f2] rounded-full flex items-center justify-center mx-auto mb-4">
            <svg className="w-8 h-8 text-[#ef4444]" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
          </div>
          <h2 className="text-2xl font-bold text-[#0f172a] mb-2 tracking-tight">
            Check-In Failed
          </h2>
          <p className="text-[#64748b] mb-6 leading-relaxed">{error}</p>
          <button
            onClick={() => navigate("/my-bookings")}
            className="px-6 py-2.5 bg-[#0f172a] text-white rounded-xl hover:bg-[#1e293b] transition-all font-semibold shadow-sm w-full"
          >
            ← Back to My Bookings
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="flex items-center justify-center min-h-screen bg-[#f8fafc] p-4 py-12">
      <div className="w-full max-w-md">
        {/* Header */}
        <div className="mb-8 text-center">
          <div className="w-16 h-16 bg-white border border-[#e2e8f0] rounded-2xl flex items-center justify-center mx-auto mb-4 shadow-sm">
            <svg className="w-8 h-8 text-[#6366f1]" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.5" d="M12 4v1m6 11h2m-6 0h-2v4m0-11v3m0 0h.01M12 12h4.01M16 20h4M4 12h4m12 0h.01M5 8h2a1 1 0 001-1V5a1 1 0 00-1-1H5a1 1 0 00-1 1v2a1 1 0 001 1zm14 0h2a1 1 0 001-1V5a1 1 0 00-1-1h-2a1 1 0 00-1 1v2a1 1 0 001 1zM5 20h2a1 1 0 001-1v-2a1 1 0 00-1-1H5a1 1 0 00-1 1v2a1 1 0 001 1z"></path></svg>
          </div>
          <h1 className="text-3xl font-bold text-[#0f172a] tracking-tight">Quick Check-In</h1>
          <p className="text-[#475569] mt-2 font-medium">
            {booking?.facility?.name || "Facility"}
          </p>
          {booking?.bookingDate && (
            <p className="text-sm text-[#64748b] mt-1 font-mono">
              {booking.startTime} - {booking.endTime}
            </p>
          )}
        </div>

        {/* Booking Info Card */}
        {booking && (
          <div className="bg-white border border-[#e2e8f0] rounded-2xl p-5 mb-6 shadow-sm">
            <div className="grid grid-cols-2 gap-y-4 gap-x-6 text-sm">
              <div>
                <span className="text-[10px] font-bold text-[#94a3b8] uppercase tracking-wider block mb-1">Facility</span>
                <p className="text-[#0f172a] font-semibold">{booking.facility?.name}</p>
              </div>
              <div>
                <span className="text-[10px] font-bold text-[#94a3b8] uppercase tracking-wider block mb-1">Time</span>
                <p className="text-[#0f172a] font-semibold">
                  {booking.startTime} - {booking.endTime}
                </p>
              </div>
              <div className="col-span-2">
                <span className="text-[10px] font-bold text-[#94a3b8] uppercase tracking-wider block mb-1">Purpose</span>
                <p className="text-[#475569] leading-relaxed bg-[#f8fafc] p-3 rounded-xl border border-[#e2e8f0]">{booking.purpose}</p>
              </div>
            </div>
          </div>
        )}

        {/* Check-In Component */}
        <div className="bg-white rounded-2xl shadow-sm border border-[#e2e8f0] p-1">
          <CheckInComponent
            bookingId={bookingId}
            onCheckInSuccess={handleCheckInSuccess}
            onClose={() => navigate("/my-bookings")}
          />
        </div>

        {/* Help Text */}
        <div className="mt-8 text-center">
          <div className="inline-flex items-center gap-2 px-4 py-2 rounded-full bg-white border border-[#e2e8f0] shadow-sm text-xs text-[#64748b]">
            <svg className="w-4 h-4 text-[#f59e0b]" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
            Location services must be enabled for GPS verification
          </div>
        </div>
      </div>
    </div>
  );
}
