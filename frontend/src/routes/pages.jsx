import React, { useState } from "react";
import { TicketDashboard, TicketDetailViewDefault } from "../features/tickets";
import { ApprovalQueue } from "../features/approvals";
import { AppealCenter } from "../features/appeals";
import { NotificationCenter } from "../features/notifications";
import { UtilizationDashboard } from "../features/analytics";
import {
  FacilityDetailsPage,
  FacilityManagementDashboard,
  FacilitySuggestionsView,
  UnderutilizedFacilitiesView,
} from "../features/facilities";
import FacilitySearch from "../features/facilities/FacilitySearch";
import { BookingRecommendations, BookingForm, BookingsList } from "../features/bookings";

function DashboardPage() {
  return (
    <section className="space-y-2">
      <h2 className="text-lg font-medium">Dashboard</h2>
      <p className="text-slate-600">Route skeleton ready for upcoming feature modules.</p>
    </section>
  );
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

function UnderutilizedPage() {
  return <UnderutilizedFacilitiesView />;
}

function FacilitySuggestionsPage() {
  return <FacilitySuggestionsView />;
}

function BookingRecommendationsPage() {
  return <BookingRecommendations />;
}

function BookingsPage() {
  return <BookingsList />;
}

function FacilitiesAndBookingsPage() {
  const [selectedFacility, setSelectedFacility] = useState(null);
  const [isBookingOpen, setIsBookingOpen] = useState(false);

  const handleFacilitySelect = (facility) => {
    setSelectedFacility(facility);
    setIsBookingOpen(true);
  };

  const closeBooking = () => {
    setIsBookingOpen(false);
    setSelectedFacility(null);
  };

  return (
    <div className="space-y-8 relative">
      <div>
        <h1 className="text-3xl font-bold text-slate-900 mb-2">Facility Booking System</h1>
        <p className="text-slate-600">Search for facilities and create booking requests</p>
      </div>

      <div>
        <FacilitySearch layout="columns" onFacilitySelect={handleFacilitySelect} />
      </div>

      {/* Slide-over booking panel (hovering, rounded, spaced) */}
      {isBookingOpen && (
        <div className="fixed inset-0 z-40 flex items-center justify-end">
          <div className="absolute inset-0 bg-black/40 backdrop-blur-sm" onClick={closeBooking} />

          <div className="absolute top-8 bottom-8 right-8 w-full max-w-2xl transform transition-all duration-200">
            <div className="flex flex-col bg-white rounded-2xl overflow-hidden shadow-2xl ring-1 ring-black/5 h-full">
              <div className="px-4 py-2 flex items-center justify-end">
                <button onClick={closeBooking} aria-label="Close booking panel" className="px-3 py-1 rounded-md text-slate-600 hover:bg-slate-100">Close</button>
              </ div>
              <div className="flex-1 overflow-auto px-6 pb-6">
                <BookingForm facility={selectedFacility} isModal onClose={closeBooking} onBookingComplete={() => { /* close after success */ setTimeout(closeBooking, 600); }} />
              </div>
            </div>
          </div>
        </div>
      )}
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
  UnderutilizedPage,
  FacilitySuggestionsPage,
  BookingRecommendationsPage,
  BookingsPage,
  FacilitiesAndBookingsPage,
};
