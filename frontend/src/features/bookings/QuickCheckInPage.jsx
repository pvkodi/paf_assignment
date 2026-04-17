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
 *
 * Flow:
 * 1. User scans QR code
 * 2. Browser opens /check-in/booking/[bookingId]
 * 3. If not logged in → Redirects to /login?redirect=/check-in/booking/[bookingId]
 * 4. After login → Returns to this page
 * 5. Booking details load
 * 6. GPS collection auto-starts
 * 7. Check-in records
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
      <div className="flex items-center justify-center min-h-screen bg-gradient-to-b from-blue-50 to-slate-100">
        <div className="text-center">
          <div className="animate-spin rounded-full h-16 w-16 border-b-2 border-blue-600 mx-auto mb-4"></div>
          <p className="text-slate-600">Loading booking...</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex items-center justify-center min-h-screen bg-gradient-to-b from-red-50 to-slate-100 p-4">
        <div className="bg-white rounded-lg shadow-lg p-8 max-w-md text-center">
          <div className="text-5xl mb-4">❌</div>
          <h2 className="text-2xl font-bold text-red-900 mb-2">
            Check-In Failed
          </h2>
          <p className="text-red-700 mb-6">{error}</p>
          <button
            onClick={() => navigate("/my-bookings")}
            className="px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition font-medium"
          >
            ← Back to My Bookings
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="flex items-center justify-center min-h-screen bg-gradient-to-b from-green-50 to-slate-100 p-4">
      <div className="w-full max-w-md">
        {/* Header */}
        <div className="mb-6 text-center">
          <div className="text-5xl mb-2">📱</div>
          <h1 className="text-3xl font-bold text-slate-900">Quick Check-In</h1>
          <p className="text-slate-600 mt-2">
            {booking?.facility?.name || "Facility"}
          </p>
          {booking?.bookingDate && (
            <p className="text-sm text-slate-500 mt-1">
              {booking.startTime} - {booking.endTime}
            </p>
          )}
        </div>

        {/* Booking Info Card */}
        {booking && (
          <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 mb-6">
            <div className="grid grid-cols-2 gap-4 text-sm">
              <div>
                <span className="font-medium text-blue-900">Facility</span>
                <p className="text-blue-700">{booking.facility?.name}</p>
              </div>
              <div>
                <span className="font-medium text-blue-900">Time</span>
                <p className="text-blue-700">
                  {booking.startTime} - {booking.endTime}
                </p>
              </div>
              <div className="col-span-2">
                <span className="font-medium text-blue-900">Purpose</span>
                <p className="text-blue-700 truncate">{booking.purpose}</p>
              </div>
            </div>
          </div>
        )}

        {/* Check-In Component */}
        <div className="bg-white rounded-lg shadow-lg p-6">
          <CheckInComponent
            bookingId={bookingId}
            onCheckInSuccess={handleCheckInSuccess}
            onClose={() => navigate("/my-bookings")}
          />
        </div>

        {/* Help Text */}
        <div className="mt-6 text-center text-xs text-slate-600">
          <p>
            💡 Make sure location services are enabled on your device for GPS
            verification
          </p>
        </div>
      </div>
    </div>
  );
}
