import React, { useState, useEffect, useContext } from "react";
import bookingService from "../../services/bookingService";
import { AuthContext } from "../../contexts/AuthContext";
import { formatDate, getStatusLabel, getStatusColor } from "../../types";

/**
 * BookingList Component
 * Displays pending bookings in the approval queue based on user's role
 * Features:
 * - Paginated list of pending bookings
 * - Filter by status/stage
 * - Quick actions (approve, reject, view details)
 * - Role-based visibility
 */
function BookingList({ onSelectBooking }) {
  const { user } = useContext(AuthContext);
  const [bookings, setBookings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  /**
   * Load pending bookings
   */
  useEffect(() => {
    loadBookings();
  }, [page, pageSize]);

  const loadBookings = async () => {
    setLoading(true);
    setError(null);

    try {
      const response = await bookingService.getPendingBookings({
        page,
        size: pageSize,
      });

      setBookings(response.content || []);
      setTotalPages(response.totalPages || 0);
      setTotalElements(response.totalElements || 0);
    } catch (err) {
      setError(err.response?.data?.message || "Failed to load bookings");
      setBookings([]);
    } finally {
      setLoading(false);
    }
  };

  /**
   * Handle pagination
   */
  const handlePreviousPage = () => {
    setPage((prev) => Math.max(0, prev - 1));
  };

  const handleNextPage = () => {
    setPage((prev) => Math.min(totalPages - 1, prev + 1));
  };

  /**
   * Determine if current user is an approver for a booking
   */
  const isApprover = (booking) => {
    if (user?.roles?.includes("ADMIN")) return true;
    if (user?.roles?.includes("LECTURER")) return true;
    if (user?.roles?.includes("FACILITY_MANAGER")) return true;
    return false;
  };

  if (!isApprover({})) {
    return (
      <div className="bg-yellow-50 border border-yellow-200 rounded-md p-6 text-center">
        <p className="text-yellow-800">
          You do not have permission to view pending bookings.
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      <div className="flex justify-between items-center">
        <h2 className="text-2xl font-bold">Pending Bookings for Approval</h2>
        <div className="text-sm text-gray-600">
          Total: <span className="font-semibold">{totalElements}</span>
        </div>
      </div>

      {/* Error message */}
      {error && (
        <div className="bg-red-50 border border-red-200 rounded-md p-4">
          <p className="text-red-800">{error}</p>
        </div>
      )}

      {/* Loading state */}
      {loading ? (
        <div className="text-center py-12">
          <div className="inline-block">
            <div className="w-8 h-8 border-4 border-blue-600 border-t-transparent rounded-full animate-spin"></div>
          </div>
          <p className="text-gray-600 mt-2">Loading bookings...</p>
        </div>
      ) : bookings.length > 0 ? (
        <div className="space-y-4">
          {/* Bookings Table */}
          <div className="bg-white rounded-lg shadow overflow-hidden">
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead>
                  <tr className="border-b bg-gray-50">
                    <th className="px-4 py-3 text-left text-sm font-semibold text-gray-700">
                      Facility
                    </th>
                    <th className="px-4 py-3 text-left text-sm font-semibold text-gray-700">
                      Requester
                    </th>
                    <th className="px-4 py-3 text-left text-sm font-semibold text-gray-700">
                      Date & Time
                    </th>
                    <th className="px-4 py-3 text-left text-sm font-semibold text-gray-700">
                      Status
                    </th>
                    <th className="px-4 py-3 text-left text-sm font-semibold text-gray-700">
                      Actions
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {bookings.map((booking) => (
                    <BookingRow
                      key={booking.id}
                      booking={booking}
                      onSelect={onSelectBooking}
                    />
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="flex justify-between items-center">
              <div className="text-sm text-gray-600">
                Page {page + 1} of {totalPages}
              </div>
              <div className="flex gap-2">
                <button
                  onClick={handlePreviousPage}
                  disabled={page === 0}
                  className="px-3 py-2 bg-gray-200 text-gray-800 rounded-md hover:bg-gray-300 disabled:bg-gray-100 disabled:text-gray-400 disabled:cursor-not-allowed transition"
                >
                  Previous
                </button>
                <button
                  onClick={handleNextPage}
                  disabled={page === totalPages - 1}
                  className="px-3 py-2 bg-gray-200 text-gray-800 rounded-md hover:bg-gray-300 disabled:bg-gray-100 disabled:text-gray-400 disabled:cursor-not-allowed transition"
                >
                  Next
                </button>
              </div>
            </div>
          )}
        </div>
      ) : (
        <div className="bg-gray-50 rounded-md p-8 text-center">
          <p className="text-gray-600">No pending bookings for approval.</p>
        </div>
      )}
    </div>
  );
}

/**
 * BookingRow Component
 * Displays a single booking row in the table
 */
function BookingRow({ booking, onSelect }) {
  const formatDateTime = (date, time) => {
    const dateStr = formatDate(date);
    return `${dateStr} ${time}`;
  };

  return (
    <tr className="border-b hover:bg-gray-50 transition">
      <td className="px-4 py-3 text-sm">
        <div>
          <p className="font-medium text-gray-900">{booking.facility?.name}</p>
          <p className="text-gray-600 text-xs">{booking.facility?.building}</p>
        </div>
      </td>
      <td className="px-4 py-3 text-sm">
        <div>
          <p className="font-medium text-gray-900">
            {booking.requestingUser?.displayName || booking.requestedBy}
          </p>
          {booking.bookedFor && (
            <p className="text-gray-600 text-xs">
              Booking for: {booking.bookedForUser?.displayName}
            </p>
          )}
        </div>
      </td>
      <td className="px-4 py-3 text-sm">
        <div>
          <p className="text-gray-900">
            {formatDateTime(booking.date, booking.startTime)}
          </p>
          <p className="text-gray-600 text-xs">
            {booking.startTime} - {booking.endTime}
          </p>
        </div>
      </td>
      <td className="px-4 py-3 text-sm">
        <span
          className={`inline-block px-2 py-1 rounded-full text-xs font-medium ${getStatusColor(booking.status)}`}
        >
          {getStatusLabel(booking.status)}
        </span>
      </td>
      <td className="px-4 py-3 text-sm">
        <button
          onClick={() => onSelect(booking)}
          className="px-3 py-1 bg-blue-600 text-white rounded-md text-xs hover:bg-blue-700 transition"
        >
          Review
        </button>
      </td>
    </tr>
  );
}

export default BookingList;
