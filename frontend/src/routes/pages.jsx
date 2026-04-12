import React, { useState } from "react";
import { TicketDashboard, TicketDetailViewDefault } from "../features/tickets";
import { ApprovalQueue } from "../features/approvals";
import { AppealCenter } from "../features/appeals";
import { NotificationCenter } from "../features/notifications";
import { UtilizationDashboard } from "../features/analytics";
import FacilitySearch from "../features/facilities/FacilitySearch";
import { BookingForm, BookingsList } from "../features/bookings";

function DashboardPage() {
  return (
    <section className="space-y-2">
      <h2 className="text-lg font-medium">Dashboard</h2>
      <p className="text-slate-600">
        Route skeleton ready for upcoming feature modules.
      </p>
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

function BookingsPage() {
  return <BookingsList />;
}

function FacilitiesAndBookingsPage() {
  const [selectedFacility, setSelectedFacility] = useState(null);

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-3xl font-bold text-slate-900 mb-2">
          Facility Booking System
        </h1>
        <p className="text-slate-600">
          Search for facilities and create booking requests
        </p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Facility Search */}
        <div className="lg:col-span-2">
          <FacilitySearch onFacilitySelect={setSelectedFacility} />
        </div>

        {/* Booking Form */}
        <div className="lg:col-span-1">
          <BookingForm facility={selectedFacility} />
        </div>
      </div>
    </div>
  );
}

export {
  AnalyticsPage,
  AppealsPage,
  ApprovalsPage,
  BookingsPage,
  DashboardPage,
  FacilitiesAndBookingsPage,
  NotFoundPage,
  NotificationsPage,
  TicketDetailPage,
  TicketsPage,
};
