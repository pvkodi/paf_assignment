import BookingPage from "../pages/BookingPage";
import PendingBookingsPage from "../pages/PendingBookingsPage";

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

export { DashboardPage, NotFoundPage, BookingPage, PendingBookingsPage };
