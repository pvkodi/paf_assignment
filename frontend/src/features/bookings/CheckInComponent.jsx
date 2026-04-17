import React, { useState, useRef, useEffect } from "react";
import { apiClient } from "../../services/apiClient";

/**
 * CheckInComponent
 * Allows users to check in to approved bookings via QR code or manual entry.
 * Enhanced with WiFi and GPS geofencing verification.
 */
export default function CheckInComponent({
  bookingId,
  onCheckInSuccess,
  onClose,
}) {
  const [method, setMethod] = useState("QR"); // QR or MANUAL
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);
  const [geofencingData, setGeofencingData] = useState(null);
  const [geofencingError, setGeofencingError] = useState(null);
  const qrInputRef = useRef(null);

  /**
   * Get user's current GPS coordinates (requires location permission).
   * Returns: { latitude, longitude }
   */
  const detectGPS = async () => {
    return new Promise((resolve, reject) => {
      if (!navigator.geolocation) {
        reject(new Error("Geolocation API not supported"));
      }

      navigator.geolocation.getCurrentPosition(
        (position) => {
          resolve({
            latitude: position.coords.latitude,
            longitude: position.coords.longitude,
          });
        },
        (error) => {
          reject(error);
        },
        { timeout: 10000, enableHighAccuracy: true },
      );
    });
  };

  /**
   * Collect geofencing data (GPS only - WiFi detection not available in browsers).
   */
  const collectGeofencingData = async () => {
    try {
      setGeofencingError(null);
      const gps = await detectGPS();

      setGeofencingData({
        latitude: gps.latitude,
        longitude: gps.longitude,
      });

      return gps;
    } catch (err) {
      const errorMsg = `GPS data collection failed: ${err.message}`;
      console.error(errorMsg);
      setGeofencingError(errorMsg);
      return null;
    }
  };

  const handleCheckIn = async (checkInMethod) => {
    try {
      setLoading(true);
      setError(null);
      setSuccess(null);

      // Collect GPS data only (WiFi detection not available in browsers)
      const gps = await collectGeofencingData();
      if (!gps) {
        setError(
          "Failed to collect GPS location. Please enable location services.",
        );
        return;
      }

      const payload = {
        method: checkInMethod,
        latitude: gps.latitude,
        longitude: gps.longitude,
      };

      console.log("📤 Sending check-in request with GPS payload:", payload);

      // Use geofencing-enabled endpoint
      await apiClient.post(
        `/v1/bookings/${bookingId}/check-in/with-geofencing`,
        payload,
      );

      setSuccess(`✅ Check-in successful! Your attendance has been recorded.`);
      setTimeout(() => {
        if (onCheckInSuccess) {
          onCheckInSuccess();
        }
      }, 2000);
    } catch (err) {
      console.error("Check-in error:", err);

      // Parse geofencing-specific errors
      if (err.response?.data?.message) {
        if (err.response.data.message.includes("GEOFENCE_GPS_OUT_OF_RANGE")) {
          setError(
            `❌ Location Out of Range\n` +
              `You are too far from the facility.\n` +
              `${err.response.data.message}`,
          );
        } else {
          setError(
            err.response?.data?.message ||
              "Failed to record check-in. Please try again or contact support.",
          );
        }
      } else {
        setError(
          err.response?.data?.message ||
            "Failed to record check-in. Please try again or contact support.",
        );
      }
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

  useEffect(() => {
    // Request geofencing permissions on component mount
    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
        () => console.log("Location permission granted"),
        () => console.warn("Location permission denied"),
      );
    }
  }, []);

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
          <p className="text-sm font-medium text-red-800 whitespace-pre-line">
            {error}
          </p>
        </div>
      )}

      {geofencingData && (
        <div className="rounded-md bg-blue-50 border border-blue-200 p-4 mb-4">
          <p className="text-xs font-semibold text-blue-900 mb-2">
            GPS Location Detected:
          </p>
          <p className="text-xs text-blue-800">
            📍 GPS: {geofencingData.latitude?.toFixed(4)},{" "}
            {geofencingData.longitude?.toFixed(4)}
          </p>
        </div>
      )}

      {/* Method Selection */}
      <div className="space-y-3 mb-6">
        <label className="block text-sm font-medium text-slate-700">
          Check-in Method
        </label>
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
            Scan the QR code displayed in the facility or on your booking
            confirmation
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
            {loading
              ? "Verifying Location & Recording..."
              : "Manual QR Check-in"}
          </button>
        </div>
      )}

      {/* Manual Entry */}
      {method === "MANUAL" && (
        <div className="bg-slate-50 border border-slate-200 rounded-lg p-6 mb-6">
          <p className="text-sm text-slate-600 mb-4">
            Your check-in will be manually recorded by staff. Confirm to
            proceed.
          </p>
          <button
            type="button"
            onClick={() => handleCheckIn("MANUAL")}
            disabled={loading}
            className="w-full px-4 py-2 bg-green-600 text-white rounded-md hover:bg-green-700 disabled:bg-green-400 disabled:cursor-not-allowed transition-colors font-medium"
          >
            {loading
              ? "Verifying Location & Recording..."
              : "Confirm Manual Check-in"}
          </button>
        </div>
      )}

      {/* Info Box */}
      <div className="bg-amber-50 border border-amber-200 rounded-lg p-3 mb-6">
        <p className="text-xs text-amber-800">
          <span className="font-semibold">ℹ️ Geofencing:</span> Your GPS
          location is verified to ensure you're at the facility. Location
          services must be enabled.
        </p>
      </div>

      {/* Close button */}
      <button
        type="button"
        onClick={onClose}
        className="w-full px-4 py-2 bg-slate-100 text-slate-700 rounded-md hover:bg-slate-200 transition-colors font-medium"
      >
        Close
      </button>
    </div>
  );
}
