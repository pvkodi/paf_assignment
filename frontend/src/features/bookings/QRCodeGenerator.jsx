import React, { useState } from "react";
import { QRCodeSVG as QRCode } from "qrcode.react";

/**
 * QR Code Generator Component
 *
 * Allows admins to generate QR codes for quick check-in
 * QR codes can be:
 * 1. Printed and placed in facilities
 * 2. Downloaded as image
 * 3. Displayed on screens
 *
 * Usage:
 * - Admin enters booking ID or uses facility ID
 * - Component generates URL: https://smartcampus.com/check-in/booking/[bookingId]
 * - QR code is displayed and can be printed/downloaded
 */

export default function QRCodeGenerator() {
  const [bookingId, setBookingId] = useState("");
  const [showQR, setShowQR] = useState(false);

  const checkInUrl = bookingId
    ? `${window.location.origin}/check-in/booking/${bookingId}`
    : "";

  const handleDownload = () => {
    const element = document.getElementById("qrcode-image");
    const link = document.createElement("a");
    link.href = element.toDataURL("image/png");
    link.download = `checkin-${bookingId}.png`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  const handlePrint = () => {
    const element = document.getElementById("qrcode-image");
    const printWindow = window.open("", "", "height=400,width=600");
    printWindow.document.write(
      "<img src='" + element.toDataURL("image/png") + "' />",
    );
    printWindow.document.close();
    printWindow.print();
  };

  return (
    <div className="max-w-md mx-auto bg-white rounded-lg shadow-lg p-6">
      <h2 className="text-2xl font-bold text-slate-900 mb-4">
        📱 QR Code Generator
      </h2>

      <div className="space-y-4">
        {/* Input */}
        <div>
          <label className="block text-sm font-medium text-slate-700 mb-2">
            Booking ID
          </label>
          <input
            type="text"
            placeholder="Enter booking ID (UUID)"
            value={bookingId}
            onChange={(e) => setBookingId(e.target.value)}
            className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
          <p className="text-xs text-slate-600 mt-1">
            Find booking ID in the bookings list
          </p>
        </div>

        {/* Generate Button */}
        <button
          onClick={() => setShowQR(!!bookingId)}
          disabled={!bookingId}
          className="w-full px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:bg-blue-400 disabled:cursor-not-allowed transition font-medium"
        >
          Generate QR Code
        </button>

        {/* QR Code Display */}
        {showQR && bookingId && (
          <div className="space-y-4">
            {/* QR Code */}
            <div className="flex justify-center p-4 bg-slate-50 rounded-lg">
              <QRCode
                id="qrcode-image"
                value={checkInUrl}
                size={256}
                level="H"
                includeMargin={true}
              />
            </div>

            {/* Check-in URL (for reference) */}
            <div className="bg-blue-50 border border-blue-200 rounded-lg p-3">
              <p className="text-xs font-medium text-blue-900 mb-1">
                Check-in URL:
              </p>
              <p className="text-xs text-blue-800 break-all font-mono">
                {checkInUrl}
              </p>
            </div>

            {/* Action Buttons */}
            <div className="grid grid-cols-2 gap-2">
              <button
                onClick={handleDownload}
                className="px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 transition text-sm font-medium"
              >
                ⬇ Download
              </button>
              <button
                onClick={handlePrint}
                className="px-4 py-2 bg-purple-600 text-white rounded-lg hover:bg-purple-700 transition text-sm font-medium"
              >
                🖨 Print
              </button>
            </div>

            {/* Instructions */}
            <div className="bg-amber-50 border border-amber-200 rounded-lg p-3">
              <p className="text-xs font-medium text-amber-900 mb-1">
                ℹ️ How to use:
              </p>
              <ul className="text-xs text-amber-800 space-y-1">
                <li>1. Download and print the QR code</li>
                <li>2. Place it in the facility</li>
                <li>3. Users scan with phone</li>
                <li>4. Auto-opens check-in page</li>
              </ul>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
