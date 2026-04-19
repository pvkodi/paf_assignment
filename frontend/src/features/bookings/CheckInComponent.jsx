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
    <div className="bg-white rounded-2xl shadow-sm border border-[#e2e8f0] p-6 max-w-md mx-auto relative overflow-hidden">
      {/* Decorative background element */}
      <div className="absolute top-0 left-0 w-full h-2 bg-gradient-to-r from-indigo-500 to-purple-500" />
      
      <div className="flex items-center justify-between mb-6 pt-2">
        <h2 className="text-xl font-bold text-[#0f172a] tracking-tight">Check In</h2>
        <button onClick={onClose} className="p-1.5 rounded-full hover:bg-[#f1f5f9] text-[#64748b] transition-colors">
          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12"></path></svg>
        </button>
      </div>

      {success && (
        <div className="rounded-xl bg-[#f0fdf4] border border-[#dcfce3] p-4 mb-4 flex items-start gap-3">
          <svg className="w-5 h-5 text-[#16a34a] shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
          <p className="text-sm font-semibold text-[#166534]">{success}</p>
        </div>
      )}

      {error && (
        <div className="rounded-xl bg-[#fef2f2] border border-[#fca5a5] p-4 mb-4 flex items-start gap-3">
          <svg className="w-5 h-5 text-[#ef4444] shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
          <p className="text-sm font-semibold text-[#991b1b] whitespace-pre-line">{error}</p>
        </div>
      )}

      {geofencingData && (
        <div className="rounded-xl bg-[#eff6ff] border border-[#dbeafe] p-4 mb-4">
          <p className="text-[10px] font-bold text-[#1e40af] uppercase tracking-wider mb-1">
            GPS Location Active
          </p>
          <p className="text-xs font-mono text-[#1e3a8a]">
            {geofencingData.latitude?.toFixed(4)}, {geofencingData.longitude?.toFixed(4)}
          </p>
        </div>
      )}

      {/* Method Selection */}
      <div className="space-y-3 mb-6">
        <label className="block text-xs font-bold text-[#64748b] uppercase tracking-wider">
          Check-in Method
        </label>
        <div className="flex p-1 bg-[#f1f5f9] rounded-xl">
          <button
            type="button"
            onClick={() => setMethod("QR")}
            className={`flex-1 px-4 py-2 rounded-lg font-semibold text-sm transition-all shadow-sm ${
              method === "QR"
                ? "bg-white text-[#0f172a]"
                : "text-[#64748b] shadow-none hover:text-[#0f172a]"
            }`}
          >
            Scan QR
          </button>
          <button
            type="button"
            onClick={() => setMethod("MANUAL")}
            className={`flex-1 px-4 py-2 rounded-lg font-semibold text-sm transition-all shadow-sm ${
              method === "MANUAL"
                ? "bg-white text-[#0f172a]"
                : "text-[#64748b] shadow-none hover:text-[#0f172a]"
            }`}
          >
            Manual Entry
          </button>
        </div>
      </div>

      {/* QR Code Input */}
      {method === "QR" && (
        <div className="bg-[#f8fafc] border border-[#e2e8f0] rounded-xl p-8 text-center mb-6">
          <div className="mb-6 flex justify-center">
            <div className="p-4 bg-white rounded-2xl border-2 border-dashed border-[#cbd5e1] shadow-sm relative">
              <svg className="w-12 h-12 text-[#94a3b8]" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.5" d="M12 4v1m6 11h2m-6 0h-2v4m0-11v3m0 0h.01M12 12h4.01M16 20h4M4 12h4m12 0h.01M5 8h2a1 1 0 001-1V5a1 1 0 00-1-1H5a1 1 0 00-1 1v2a1 1 0 001 1zm14 0h2a1 1 0 001-1V5a1 1 0 00-1-1h-2a1 1 0 00-1 1v2a1 1 0 001 1zM5 20h2a1 1 0 001-1v-2a1 1 0 00-1-1H5a1 1 0 00-1 1v2a1 1 0 001 1z"></path></svg>
            </div>
          </div>
          <p className="text-sm text-[#475569] font-medium mb-6 px-4">
            Scan the QR code displayed in the facility to check in instantly.
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
            className="w-full px-4 py-3 bg-[#0f172a] text-white rounded-xl hover:bg-[#1e293b] disabled:opacity-50 disabled:cursor-not-allowed transition-all font-semibold shadow-sm text-sm"
          >
            {loading ? "Verifying..." : "Simulate Scan"}
          </button>
        </div>
      )}

      {/* Manual Entry */}
      {method === "MANUAL" && (
        <div className="bg-[#f8fafc] border border-[#e2e8f0] rounded-xl p-8 mb-6 text-center">
          <div className="w-12 h-12 bg-white rounded-full border border-[#e2e8f0] flex items-center justify-center mx-auto mb-4 shadow-sm">
            <svg className="w-6 h-6 text-[#64748b]" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"></path></svg>
          </div>
          <p className="text-sm text-[#475569] mb-6">
            Your check-in will be manually recorded. Confirm to proceed with GPS verification.
          </p>
          <button
            type="button"
            onClick={() => handleCheckIn("MANUAL")}
            disabled={loading}
            className="w-full px-4 py-3 bg-[#0f172a] text-white rounded-xl hover:bg-[#1e293b] disabled:opacity-50 disabled:cursor-not-allowed transition-all font-semibold shadow-sm text-sm"
          >
            {loading ? "Verifying Location..." : "Confirm Manual Check-in"}
          </button>
        </div>
      )}

      {/* Info Box */}
      <div className="bg-[#fffbeb] border border-[#fde68a] rounded-xl p-4 flex gap-3">
        <svg className="w-5 h-5 text-[#b45309] shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z"></path><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15 11a3 3 0 11-6 0 3 3 0 016 0z"></path></svg>
        <p className="text-xs text-[#92400e] leading-relaxed">
          <span className="font-bold block mb-0.5">Geofencing Active</span>
          Your GPS location is verified to ensure you're at the facility. Location services must be enabled.
        </p>
      </div>
    </div>
  );
}
