import React, { useState } from "react";
import { BookingList, BookingDetail } from "../features/bookings";

/**
 * PendingBookingsPage
 * Shows pending bookings for approval based on user's role
 */
function PendingBookingsPage() {
  const [selectedBooking, setSelectedBooking] = useState(null);

  const handleBookingUpdate = (updatedBooking) => {
    setSelectedBooking(null);
    // Could refresh list here
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold text-gray-900">Pending Bookings</h1>
        <p className="text-gray-600 mt-2">
          Review and approve booking requests
        </p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2">
          <BookingList onSelectBooking={setSelectedBooking} />
        </div>

        {selectedBooking && (
          <div className="lg:col-span-1">
            <BookingDetail
              bookingId={selectedBooking.id}
              onClose={() => setSelectedBooking(null)}
              onUpdated={handleBookingUpdate}
            />
          </div>
        )}
      </div>
    </div>
  );
}

export default PendingBookingsPage;
