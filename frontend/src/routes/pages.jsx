import React, { useState } from "react";
import { Navigate } from "react-router-dom";
import { useAuth } from "../contexts/AuthContext";
import { TicketDashboard, TicketDetailViewDefault } from "../features/tickets";
import { ApprovalQueue } from "../features/approvals";
import { AppealCenter } from "../features/appeals";
import { NotificationCenter } from "../features/notifications";
import { UtilizationDashboard } from "../features/analytics";
import {
  FacilityDetailsPage,
  FacilityManagementDashboard,
  TimetablePreviewPage,
} from "../features/facilities";
import FacilitySearch from "../features/facilities/FacilitySearch";
import {
  BookingForm,
  BookingsList,
  BookingRecommendations,
  BookingApprovalQueue,
  AdminBookingsView,
  QuotaPolicySummary,
  QuickCheckInPage,
  QRCodeGenerator,
} from "../features/bookings";

function DashboardPage() {
  const { hasRole } = useAuth();
  const isAdmin = hasRole("ADMIN");
  const isFacilityManager = hasRole("FACILITY_MANAGER");

  // Admin and Facility Managers see the Utilization Dashboard
  if (isAdmin || isFacilityManager) {
    return <UtilizationDashboard />;
  }

  // Other users are redirected to the bookings page
  return <Navigate to="/bookings" replace />;
}

function NotFoundPage() {
  return (
    <section className="space-y-2">
      <h2 className="text-lg font-medium">Page Not Found</h2>
      <p className="text-slate-600">The requested route does not exist.</p>
    </section>
  );
}

function TicketsPage() {
  return <TicketDashboard />;
}

function TicketDetailPage() {
  return <TicketDetailViewDefault />;
}

function ApprovalsPage() {
  return <ApprovalQueue />;
}

function AppealsPage() {
  return <AppealCenter />;
}

function NotificationsPage() {
  return <NotificationCenter />;
}

function AnalyticsPage() {
  return <UtilizationDashboard />;
}

function FacilitiesPage() {
  return <FacilityManagementDashboard />;
}

function FacilityDetailRoutePage() {
  return <FacilityDetailsPage />;
}

function FacilitySuggestionsPage() {
  return <FacilitySuggestionsView />;
}

function TimetableImportPreviewPage() {
  return <TimetablePreviewPage />;
}

function BookingRecommendationsPage() {
  return <BookingRecommendations />;
}

function BookingsPage() {
  return <BookingsList />;
}

function BookingApprovalsPage() {
  return (
    <div className="space-y-6">
      <div className="bg-white rounded-lg shadow-md p-6">
        <h1 className="text-3xl font-bold text-slate-900 mb-2">
          Booking Approvals
        </h1>
        <p className="text-slate-600">
          Review and approve booking requests from facility users
        </p>
      </div>
      <BookingApprovalQueue />
    </div>
  );
}

function AdminBookingsPage() {
  return <AdminBookingsView />;
}

function FacilitiesAndBookingsPage() {
  const [selectedFacility, setSelectedFacility] = useState(null);
  const [isBookingOpen, setIsBookingOpen] = useState(false);
  const [prefillData, setPrefillData] = useState(null);

  const handleFacilitySelect = (facility) => {
    // Check if the facility object has suggestion-related date/time fields
    if (facility && facility.suggestedDate) {
      setPrefillData({
        date: facility.suggestedDate,
        startTime: facility.suggestedStartTime,
        endTime: facility.suggestedEndTime,
      });
    } else {
      setPrefillData(null);
    }

    setSelectedFacility(facility);
    setIsBookingOpen(true);
  };

  const closeBooking = () => {
    setIsBookingOpen(false);
    setSelectedFacility(null);
    setPrefillData(null);
  };

  return (
    <div className="space-y-8 relative">
      <div>
        <h1 className="text-3xl font-bold text-slate-900 mb-2">
          Facility Booking System
        </h1>
        <p className="text-slate-600">
          Search for facilities and create booking requests
        </p>
      </div>

      <div>
        <FacilitySearch
          layout="columns"
          onFacilitySelect={handleFacilitySelect}
        />
      </div>

      {/* Quota Policy Summary below facilities search */}

      {/* Slide-over booking panel (hovering, rounded, spaced) */}
      {isBookingOpen && (
        <div className="fixed inset-0 z-40 flex items-center justify-end">
          <div
            className="absolute inset-0 bg-black/40 backdrop-blur-sm"
            onClick={closeBooking}
          />

          <div className="absolute top-8 bottom-8 right-8 w-full max-w-2xl transform transition-all duration-200">
            <div className="flex flex-col bg-white rounded-2xl overflow-hidden shadow-2xl ring-1 ring-black/5 h-full">
              <div className="px-4 py-2 flex items-center justify-end">
                <button
                  onClick={closeBooking}
                  aria-label="Close booking panel"
                  className="px-3 py-1 rounded-md text-slate-600 hover:bg-slate-100"
                >
                  Close
                </button>
              </div>
              <div className="flex-1 overflow-auto px-6 pb-6">
                <BookingForm
                  facility={selectedFacility}
                  prefill={prefillData}
                  isModal
                  onClose={closeBooking}
                  onBookingComplete={() => {
                    /* close after success */ setTimeout(closeBooking, 600);
                  }}
                />
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

function QuickCheckInPageWrapper() {
  return <QuickCheckInPage />;
}

function QRCodeGeneratorPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold text-slate-900">QR Code Generator</h1>
        <p className="text-slate-600 mt-2">
          Generate QR codes for facility check-in
        </p>
      </div>
      <QRCodeGenerator />
    </div>
  );
}

export {
  DashboardPage,
  NotFoundPage,
  TicketsPage,
  TicketDetailPage,
  ApprovalsPage,
  AppealsPage,
  NotificationsPage,
  AnalyticsPage,
  FacilitiesPage,
  FacilityDetailRoutePage,
  FacilitySuggestionsPage,
  TimetableImportPreviewPage,
  BookingRecommendationsPage,
  BookingsPage,
  BookingApprovalsPage,
  AdminBookingsPage,
  FacilitiesAndBookingsPage,
  QuickCheckInPageWrapper,
  QRCodeGeneratorPage,
};
