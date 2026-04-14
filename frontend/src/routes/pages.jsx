import React, { useState } from "react";
import { TicketDashboard, TicketDetailViewDefault } from "../features/tickets";
import { ApprovalQueue } from "../features/approvals";
import { AppealCenter } from "../features/appeals";
import { NotificationCenter } from "../features/notifications";
import { UtilizationDashboard } from "../features/analytics";
import FacilitySearch from "../features/facilities/FacilitySearch";
import {
  BookingForm,
  BookingsList,
  BookingApprovalQueue,
  QuotaPolicySummary,
} from "../features/bookings";

function DashboardPage() {
  return (
    <section className="space-y-6">
      <div>
        <h2 className="text-3xl font-bold text-slate-900">Smart Campus Dashboard</h2>
        <p className="text-slate-600 mt-2">Welcome to the Smart Campus Operations Hub</p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div className="bg-blue-50 border border-blue-200 rounded-lg p-6">
          <h3 className="text-lg font-semibold text-blue-900 mb-2">📋 My Bookings</h3>
          <p className="text-sm text-blue-700">View and manage your facility bookings</p>
        </div>
        <div className="bg-amber-50 border border-amber-200 rounded-lg p-6">
          <h3 className="text-lg font-semibold text-amber-900 mb-2">✅ Pending Approvals</h3>
          <p className="text-sm text-amber-700">Review booking requests needing your approval</p>
        </div>
      </div>
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

function BookingApprovalsPage() {
  return <BookingApprovalQueue />;
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

        {/* Booking Form & Status */}
        <div className="lg:col-span-1 space-y-6">
          <QuotaPolicySummary userRole="USER" compact={false} />
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
  BookingApprovalsPage,
  DashboardPage,
  FacilitiesAndBookingsPage,
  NotFoundPage,
  NotificationsPage,
  TicketDetailPage,
  TicketsPage,
};
