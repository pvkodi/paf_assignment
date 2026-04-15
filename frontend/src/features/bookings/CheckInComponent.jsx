import React, { useState, useRef } from "react";
import { apiClient } from "../../services/apiClient";

/**
 * CheckInComponent
 * Allows users to check in to approved bookings via QR code or manual entry.
 */
export default function CheckInComponent({ bookingId, onCheckInSuccess, onClose }) {
  const [method, setMethod] = useState("QR"); // QR or MANUAL
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);
  const qrInputRef = useRef(null);

  const handleCheckIn = async (checkInMethod) => {
    try {
      setLoading(true);
      setError(null);
      setSuccess(null);

      const payload = {
        method: checkInMethod,
      };

      await apiClient.post(`/v1/bookings/${bookingId}/check-in`, payload);

      setSuccess(`✅ Check-in successful! Your attendance has been recorded.`);
      setTimeout(() => {
        if (onCheckInSuccess) {
          onCheckInSuccess();
        }
      }, 2000);
    } catch (err) {
      console.error("Check-in error:", err);
      setError(
        err.response?.data?.message ||
          "Failed to record check-in. Please try again or contact support.",
      );
    } finally {
      setLoading(false);
    }
  };

  const handleQRScan = (e) => {
    // Simulate QR scan - in real implementation, use QR code reader library
    const qrData = e.target.value;
    if (qrData && qrData.includes(bookingId)) {
      handleCheckIn("QR");
    }
  };

  return (
    <div className="bg-white rounded-lg shadow-lg p-6 max-w-md mx-auto">
      <h2 className="text-2xl font-bold text-slate-900 mb-4">Check In</h2>

      {success && (
        <div className="rounded-md bg-green-50 p-4 mb-4">
          <p className="text-sm font-medium text-green-800">{success}</p>
        </div>
      )}

      {error && (
        <div className="rounded-md bg-red-50 p-4 mb-4">
          <p className="text-sm font-medium text-red-800">{error}</p>
        </div>
      )}

      {/* Method Selection */}
      <div className="space-y-3 mb-6">
        <label className="block text-sm font-medium text-slate-700">Check-in Method</label>
        <div className="flex gap-2">
          <button
            type="button"
            onClick={() => setMethod("QR")}
            className={`flex-1 px-4 py-2 rounded-md font-medium transition-colors ${
              method === "QR"
                ? "bg-blue-600 text-white"
                : "bg-slate-200 text-slate-700 hover:bg-slate-300"
            }`}
          >
            Scan QR Code
          </button>
          <button
            type="button"
            onClick={() => setMethod("MANUAL")}
            className={`flex-1 px-4 py-2 rounded-md font-medium transition-colors ${
              method === "MANUAL"
                ? "bg-blue-600 text-white"
                : "bg-slate-200 text-slate-700 hover:bg-slate-300"
            }`}
          >
            Manual Entry
          </button>
        </div>
      </div>

      {/* QR Code Input */}
      {method === "QR" && (
        <div className="bg-blue-50 border border-blue-200 rounded-lg p-6 text-center mb-6">
          <div className="mb-4">
            <div className="inline-block p-4 bg-white rounded-lg border-2 border-dashed border-blue-300">
              <p className="text-3xl">📱</p>
            </div>
          </div>
          <p className="text-sm text-blue-900 font-medium mb-3">
            Scan the QR code displayed in the facility or on your booking confirmation
          </p>
          <input
            ref={qrInputRef}
            type="text"
            onChange={handleQRScan}
            placeholder="QR code will appear here (hidden)"
            className="sr-only"
            autoFocus
          />
          <button
            type="button"
            onClick={() => handleCheckIn("QR")}
            disabled={loading}
            className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:bg-blue-400 disabled:cursor-not-allowed transition-colors font-medium"
          >
            {loading ? "Recording..." : "Manual QR Check-in"}
          </button>
        </div>
      )}

      {/* Manual Entry */}
      {method === "MANUAL" && (
        <div className="bg-slate-50 border border-slate-200 rounded-lg p-6 mb-6">
          <p className="text-sm text-slate-600 mb-4">
            Your check-in will be manually recorded by staff. Confirm to proceed.
          </p>
          <button
            type="button"
            onClick={() => handleCheckIn("MANUAL")}
            disabled={loading}
            className="w-full px-4 py-2 bg-green-600 text-white rounded-md hover:bg-green-700 disabled:bg-green-400 disabled:cursor-not-allowed transition-colors font-medium"
          >
            {loading ? "Recording..." : "Confirm Manual Check-in"}
          </button>
        </div>
      )}

      {/* Info Box */}
      <div className="bg-amber-50 border border-amber-200 rounded-lg p-3 mb-6">
        <p className="text-xs text-amber-800">
          <span className="font-semibold">ℹ️ Note:</span> Check-in must be completed within the
          booking time window. Late check-ins may be marked as no-shows.
        </p>
      </div>

      {/* Actions */}
      <div className="flex gap-2">
        <button
          onClick={onClose}
          className="flex-1 px-4 py-2 bg-slate-200 text-slate-700 rounded-md hover:bg-slate-300 transition-colors font-medium"
        >
          Cancel
        </button>
      </div>
    </div>
  );
}
