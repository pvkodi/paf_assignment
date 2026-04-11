import { createBrowserRouter } from "react-router-dom";
import AppShell from "../app/AppShell";
import ProtectedRoute from "./ProtectedRoute";
import {
  DashboardPage,
  NotFoundPage,
  BookingPage,
  PendingBookingsPage,
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
        path: "bookings",
        element: (
          <ProtectedRoute>
            <BookingPage />
          </ProtectedRoute>
        ),
      },
      {
        path: "approvals",
        element: (
          <ProtectedRoute>
            <PendingBookingsPage />
          </ProtectedRoute>
        ),
      },
    ],
  },
  {
    path: "*",
    element: <NotFoundPage />,
  },
]);

export default router;
