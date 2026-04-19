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
        className="fixed inset-0 bg-[#0f172a]/60 backdrop-blur-sm flex items-center justify-center z-50 p-4 animate-in fade-in duration-200"
      >
        {/* Modal */}
        <div
          onClick={(e) => e.stopPropagation()}
          className="bg-white rounded-2xl shadow-xl max-w-md w-full mx-auto p-8 space-y-6 animate-in zoom-in-95 duration-200"
        >
          <div className="text-center mb-6">
            <div className="w-16 h-16 bg-[#f8fafc] border border-[#e2e8f0] rounded-2xl flex items-center justify-center mx-auto mb-4 shadow-sm">
              <svg className="w-8 h-8 text-[#0f172a]" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.5" d="M12 4v1m6 11h2m-6 0h-2v4m0-11v3m0 0h.01M12 12h4.01M16 20h4M4 12h4m12 0h.01M5 8h2a1 1 0 001-1V5a1 1 0 00-1-1H5a1 1 0 00-1 1v2a1 1 0 001 1zm14 0h2a1 1 0 001-1V5a1 1 0 00-1-1h-2a1 1 0 00-1 1v2a1 1 0 001 1zM5 20h2a1 1 0 001-1v-2a1 1 0 00-1-1H5a1 1 0 00-1 1v2a1 1 0 001 1z"></path></svg>
            </div>
            <h2 className="text-2xl font-bold text-[#0f172a] tracking-tight">
              Generate QR PDF?
            </h2>
            <p className="text-[#64748b] mt-2 text-sm leading-relaxed">
              Create a printable PDF with a custom QR code for quick facility check-in.
            </p>
          </div>

          <div className="bg-[#f8fafc] border border-[#e2e8f0] rounded-xl p-5 space-y-3">
            <div>
              <p className="text-[10px] font-bold text-[#94a3b8] uppercase tracking-wider mb-1">Facility</p>
              <p className="text-sm text-[#0f172a] font-semibold">
                {booking.facility?.name || "N/A"}
              </p>
            </div>
            <div>
              <p className="text-[10px] font-bold text-[#94a3b8] uppercase tracking-wider mb-1">Booking ID</p>
              <p className="text-sm text-[#475569] font-mono font-medium">
                {booking.id}
              </p>
            </div>
          </div>

          <div className="bg-[#f0fdf4] border border-[#dcfce3] rounded-xl p-4 flex gap-3 items-start">
            <svg className="w-5 h-5 text-[#16a34a] shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
            <p className="text-xs text-[#166534] font-medium leading-relaxed">
              PDF includes the QR code, facility details, and simple step-by-step check-in instructions.
            </p>
          </div>

          <div className="flex gap-3 pt-2">
            <button
              onClick={onClose}
              className="flex-1 px-4 py-3 bg-[#f1f5f9] text-[#0f172a] font-semibold rounded-xl hover:bg-[#e2e8f0] transition-colors shadow-sm"
            >
              Skip
            </button>
            <button
              onClick={handleGeneratePdf}
              className="flex-[2] px-4 py-3 bg-[#0f172a] text-white font-semibold rounded-xl hover:bg-[#1e293b] transition-all shadow-sm flex items-center justify-center gap-2"
            >
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"></path></svg>
              Generate PDF
            </button>
          </div>
        </div>
      </div>

      {/* Hidden canvas ref */}
      <canvas ref={canvasRef} style={{ display: "none" }} />
    </>
  );
}
