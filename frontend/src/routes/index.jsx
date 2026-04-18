import { createBrowserRouter } from "react-router-dom";
import AppShell from "../app/AppShell";
import {
  DashboardPage,
  NotFoundPage,
  TicketsPage,
  TicketDetailPage,
  FacilitiesAndBookingsPage,
  BookingsPage,
  BookingRecommendationsPage,
  ApprovalsPage,
  AppealsPage,
  BookingApprovalsPage,
  AdminBookingsPage,
  AnalyticsPage,
  FacilitiesPage,
  FacilityDetailRoutePage,
  FacilitySuggestionsPage,
  NotificationsPage,
} from "./pages";

const router = createBrowserRouter([
  {
    path: "/",
    element: <AppShell />,
    children: [
      {
        index: true,
        element: <DashboardPage />,
      },
      {
        path: "my-bookings",
        element: <BookingsPage />,
      },
      {
        path: "bookings/recommendations",
        element: <BookingRecommendationsPage />,
      },
      {
        path: "facilities",
        element: <FacilitiesAndBookingsPage />,
      },
      {
        path: "facility/:id",
        element: <FacilityDetailRoutePage />,
      },
      {
        path: "facilities/suggestions",
        element: <FacilitySuggestionsPage />,
      },
      {
        path: "approvals",
        element: <ApprovalsPage />,
      },
      {
        path: "approvals/bookings",
        element: <BookingApprovalsPage />,
      },
      {
        path: "appeals",
        element: <AppealsPage />,
      },
      {
        path: "notifications",
        element: <NotificationsPage />,
      },
      {
        path: "analytics",
        element: <AnalyticsPage />,
      },
      {
        path: "approvals",
        element: <ApprovalsPage />,
      },
      {
        path: "approvals/bookings",
        element: <BookingApprovalsPage />,
      },
      {
        path: "admin/bookings",
        element: <AdminBookingsPage />,
      },
      {
        path: "appeals",
        element: <AppealsPage />,
      },
      {
        path: "tickets",
        element: <TicketsPage />,
      },
      {
        path: "tickets/:id",
        element: <TicketDetailPage />,
      },
    ],
  },
  {
    path: "*",
    element: <NotFoundPage />,
  },
]);

export default router;
