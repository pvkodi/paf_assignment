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
            { path: "bookings", element: <BookingsPage /> },
            { path: "bookings/recommendations", element: <BookingRecommendationsPage /> },
            { path: "facilities", element: <FacilitiesAndBookingsPage /> },
      {
        path: "my-bookings",
        element: <BookingsPage />,
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
