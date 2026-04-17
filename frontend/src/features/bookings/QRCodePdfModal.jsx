import React, { useRef, useEffect } from "react";
import jsPDF from "jspdf";
import QRCode from "qrcode";

/**
 * QRCodePdfModal Component
 *
 * Modal that appears after booking approval
 * Offers to generate and download a PDF with:
 * - QR code for check-in
 * - Facility name
 * - Booking ID
 * - Instructions
 */
export default function QRCodePdfModal({ booking, isOpen, onClose }) {
  const canvasRef = useRef();

  if (!isOpen || !booking) return null;

  const checkInUrl = `${window.location.origin}/check-in/booking/${booking.id}`;

  const handleGeneratePdf = async () => {
    try {
      // Generate QR code as canvas
      const canvas = await QRCode.toCanvas(checkInUrl, {
        width: 300,
        margin: 2,
        color: {
          dark: "#000000",
          light: "#FFFFFF",
        },
      });

      // Create PDF
      const pdf = new jsPDF({
        orientation: "portrait",
        unit: "mm",
        format: "a4",
      });

      const pageWidth = pdf.internal.pageSize.getWidth();
      const pageHeight = pdf.internal.pageSize.getHeight();
      const margin = 15;
      let yPosition = margin;

      // Title
      pdf.setFontSize(24);
      pdf.setTextColor(31, 41, 55);
      pdf.text("Quick Check-In", pageWidth / 2, yPosition, { align: "center" });
      yPosition += 15;

      // Facility Name
      pdf.setFontSize(18);
      pdf.setTextColor(79, 70, 229);
      pdf.text(booking.facility?.name || "Facility", pageWidth / 2, yPosition, {
        align: "center",
      });
      yPosition += 20;

      // QR Code
      const qrWidth = 100;
      const qrHeight = 100;
      const qrX = (pageWidth - qrWidth) / 2;
      const imgData = canvas.toDataURL("image/png");
      pdf.addImage(imgData, "PNG", qrX, yPosition, qrWidth, qrHeight);
      yPosition += qrHeight + 15;

      // Booking Details Box
      pdf.setFontSize(11);
      pdf.setTextColor(31, 41, 55);

      // Box background
      pdf.setDrawColor(200, 200, 200);
      pdf.rect(margin, yPosition - 5, pageWidth - 2 * margin, 50);

      pdf.setFontSize(10);
      pdf.text("Booking Details", margin + 5, yPosition + 2);

      yPosition += 10;
      pdf.setFontSize(9);
      pdf.text(`Booking ID: ${booking.id}`, margin + 5, yPosition);
      yPosition += 7;
      pdf.text(
        `Facility: ${booking.facility?.name || "N/A"}`,
        margin + 5,
        yPosition,
      );
      yPosition += 7;
      pdf.text(
        `Date: ${new Date(booking.bookingDate).toLocaleDateString()}`,
        margin + 5,
        yPosition,
      );
      yPosition += 7;
      pdf.text(
        `Time: ${booking.startTime} - ${booking.endTime}`,
        margin + 5,
        yPosition,
      );

      yPosition += 20;

      // Instructions
      pdf.setFontSize(11);
      pdf.setTextColor(31, 41, 55);
      pdf.text("How to Use:", margin, yPosition);
      yPosition += 8;

      pdf.setFontSize(9);
      pdf.setTextColor(107, 114, 128);
      const instructions = [
        "1. Print this document or display on screen",
        "2. User scans QR code with phone camera",
        "3. Automatically opens check-in page",
        "4. User allows location access",
        "5. Check-in completes once location verified",
      ];

      instructions.forEach((instruction) => {
        if (yPosition > pageHeight - 30) {
          pdf.addPage();
          yPosition = margin;
        }
        pdf.text(instruction, margin + 5, yPosition);
        yPosition += 7;
      });

      // Footer
      yPosition = pageHeight - 15;
      pdf.setFontSize(8);
      pdf.setTextColor(156, 163, 175);
      pdf.text(
        `Generated: ${new Date().toLocaleString()}`,
        pageWidth / 2,
        yPosition,
        {
          align: "center",
        },
      );

      // Save PDF
      pdf.save(`QR-CheckIn-${booking.id.substring(0, 8)}.pdf`);

      onClose();
    } catch (error) {
      console.error("Error generating PDF:", error);
      alert("Failed to generate PDF. Please try again.");
    }
  };

  return (
    <>
      {/* Backdrop */}
      <div
        onClick={onClose}
        className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50"
      >
        {/* Modal */}
        <div
          onClick={(e) => e.stopPropagation()}
          className="bg-white rounded-lg shadow-xl max-w-md w-full mx-4 p-6 space-y-6"
        >
          <div>
            <h2 className="text-2xl font-bold text-slate-900">
              Generate QR Code PDF?
            </h2>
            <p className="text-slate-600 mt-1">
              Create a printable PDF with a QR code for quick check-in
            </p>
          </div>

          <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 space-y-2">
            <p className="text-sm font-semibold text-blue-900">Facility:</p>
            <p className="text-sm text-blue-700 font-mono">
              {booking.facility?.name || "N/A"}
            </p>
            <p className="text-sm font-semibold text-blue-900 mt-3">
              Booking ID:
            </p>
            <p className="text-sm text-blue-700 font-mono">{booking.id}</p>
          </div>

          <div className="bg-green-50 border border-green-200 rounded-lg p-3">
            <p className="text-xs text-green-700">
              ✓ PDF will include QR code, facility details, and check-in
              instructions
            </p>
          </div>

          <div className="flex gap-3">
            <button
              onClick={onClose}
              className="flex-1 px-4 py-2 bg-slate-200 text-slate-700 font-medium rounded-lg hover:bg-slate-300 transition-colors"
            >
              Skip
            </button>
            <button
              onClick={handleGeneratePdf}
              className="flex-1 px-4 py-2 bg-blue-600 text-white font-medium rounded-lg hover:bg-blue-700 transition-colors"
            >
              📄 Generate PDF
            </button>
          </div>
        </div>
      </div>

      {/* Hidden canvas ref */}
      <canvas ref={canvasRef} style={{ display: "none" }} />
    </>
  );
}
