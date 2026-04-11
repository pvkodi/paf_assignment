import React, { useState, useEffect, useContext } from "react";
import bookingService from "../../services/bookingService";
import { AuthContext } from "../../contexts/AuthContext";
import { formatDate, getStatusLabel, getStatusColor } from "../../types";

/**
 * BookingDetail Component
 * Displays detailed booking information with approval/rejection/check-in actions
 * Features:
 * - Full booking details (facility, dates, times, approvals)
 * - Approval workflow actions
 * - Check-in functionality
 * - Rejection with reason
 */
function BookingDetail({ bookingId, onClose, onUpdated }) {
  const { user } = useContext(AuthContext);
  const [booking, setBooking] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [actionLoading, setActionLoading] = useState(false);
  const [showRejectForm, setShowRejectForm] = useState(false);
  const [rejectReason, setRejectReason] = useState("");
  const [successMessage, setSuccessMessage] = useState(null);

  /**
   * Load booking details
   */
  useEffect(() => {
    loadBooking();
  }, [bookingId]);

  const loadBooking = async () => {
    setLoading(true);
    setError(null);

    try {
      const response = await bookingService.getBookingById(bookingId);
      setBooking(response);
    } catch (err) {
      setError(err.response?.data?.message || "Failed to load booking");
    } finally {
      setLoading(false);
    }
  };

  /**
   * Handle approval
   */
  const handleApprove = async () => {
    if (!window.confirm("Are you sure you want to approve this booking?")) {
      return;
    }

    setActionLoading(true);
    setError(null);
    setSuccessMessage(null);

    try {
      const updated = await bookingService.approveBooking(bookingId);
      setBooking(updated);
      setSuccessMessage("Booking approved successfully");
      if (onUpdated) {
        onUpdated(updated);
      }
    } catch (err) {
      setError(err.response?.data?.message || "Failed to approve booking");
    } finally {
      setActionLoading(false);
    }
  };

  /**
   * Handle rejection
   */
  const handleReject = async () => {
    if (!rejectReason.trim()) {
      setError("Rejection reason is required");
      return;
    }

    if (!window.confirm("Are you sure you want to reject this booking?")) {
      return;
    }

    setActionLoading(true);
    setError(null);
    setSuccessMessage(null);

    try {
      const updated = await bookingService.rejectBooking(
        bookingId,
        rejectReason,
      );
      setBooking(updated);
      setSuccessMessage("Booking rejected successfully");
      setShowRejectForm(false);
      setRejectReason("");
      if (onUpdated) {
        onUpdated(updated);
      }
    } catch (err) {
      setError(err.response?.data?.message || "Failed to reject booking");
    } finally {
      setActionLoading(false);
    }
  };

  /**
   * Handle check-in
   */
  const handleCheckIn = async () => {
    if (!window.confirm("Check in to this booking now?")) {
      return;
    }

    setActionLoading(true);
    setError(null);
    setSuccessMessage(null);

    try {
      const updated = await bookingService.checkInBooking(bookingId);
      setBooking(updated);
      setSuccessMessage("Successfully checked in to booking");
      if (onUpdated) {
        onUpdated(updated);
      }
    } catch (err) {
      setError(err.response?.data?.message || "Failed to check in");
    } finally {
      setActionLoading(false);
    }
  };

  /**
   * Determine if current user can approve this booking
   */
  const canApprove = () => {
    if (!booking) return false;

    const approveStatus = booking.status;
    if (
      approveStatus === "PENDING_LECTURER_APPROVAL" &&
      user?.roles?.includes("LECTURER")
    ) {
      return true;
    }
    if (
      approveStatus === "PENDING_ADMIN_APPROVAL" &&
      user?.roles?.includes("ADMIN")
    ) {
      return true;
    }
    if (
      approveStatus === "PENDING_FACILITY_MANAGER_APPROVAL" &&
      user?.roles?.includes("FACILITY_MANAGER")
    ) {
      return true;
    }
    return user?.roles?.includes("ADMIN");
  };

  const canCheckIn = () => {
    if (!booking) return false;

    // Can check in if approved and not yet checked in
    if (booking.status === "APPROVED" && !booking.checkedInAt) {
      // Check if within 15 minutes of start time
      const bookingStart = new Date(`${booking.date}T${booking.startTime}`);
      const now = new Date();
      const diffMinutes = (now - bookingStart) / (1000 * 60);

      // Allow check-in 15 minutes before start to 15 minutes after start
      if (diffMinutes >= -15 && diffMinutes <= 15) {
        return true;
      }
    }
    return false;
  };

  if (loading) {
    return (
      <div className="text-center py-12">
        <div className="inline-block">
          <div className="w-8 h-8 border-4 border-blue-600 border-t-transparent rounded-full animate-spin"></div>
        </div>
        <p className="text-gray-600 mt-2">Loading booking details...</p>
      </div>
    );
  }

  if (error && !booking) {
    return (
      <div className="bg-red-50 border border-red-200 rounded-md p-4">
        <p className="text-red-800">{error}</p>
        <button
          onClick={onClose}
          className="mt-2 px-3 py-2 bg-red-600 text-white rounded-md hover:bg-red-700"
        >
          Close
        </button>
      </div>
    );
  }

  if (!booking) {
    return (
      <div className="text-center py-8">
        <p className="text-gray-600">Booking not found</p>
      </div>
    );
  }

  return (
    <div className="bg-white rounded-lg shadow-lg">
      {/* Header */}
      <div className="border-b px-6 py-4 flex justify-between items-center">
        <h2 className="text-2xl font-bold">Booking Details</h2>
        <button
          onClick={onClose}
          className="text-gray-500 hover:text-gray-700 text-2xl leading-none"
        >
          ×
        </button>
      </div>

      <div className="p-6 space-y-6">
        {/* Messages */}
        {error && (
          <div className="bg-red-50 border border-red-200 rounded-md p-4">
            <p className="text-red-800">{error}</p>
          </div>
        )}

        {successMessage && (
          <div className="bg-green-50 border border-green-200 rounded-md p-4">
            <p className="text-green-800">{successMessage}</p>
          </div>
        )}

        {/* Status */}
        <div className="flex justify-between items-start">
          <div>
            <p className="text-gray-600 text-sm">Status</p>
            <span
              className={`inline-block px-3 py-1 rounded-full text-sm font-medium mt-1 ${getStatusColor(booking.status)}`}
            >
              {getStatusLabel(booking.status)}
            </span>
          </div>
          <div>
            <p className="text-gray-600 text-sm">Booking ID</p>
            <p className="font-mono font-semibold text-sm mt-1">{booking.id}</p>
          </div>
        </div>

        {/* Facility Details */}
        <div className="border-t pt-6">
          <h3 className="text-lg font-semibold mb-4">Facility</h3>
          <div className="grid grid-cols-2 gap-4">
            <DetailItem label="Name" value={booking.facility?.name} />
            <DetailItem label="Type" value={booking.facility?.type} />
            <DetailItem
              label="Capacity"
              value={`${booking.facility?.capacity} people`}
            />
            <DetailItem label="Building" value={booking.facility?.building} />
            <DetailItem label="Location" value={booking.facility?.location} />
            {booking.facility?.floor && (
              <DetailItem label="Floor" value={booking.facility.floor} />
            )}
          </div>
        </div>

        {/* Booking Details */}
        <div className="border-t pt-6">
          <h3 className="text-lg font-semibold mb-4">Booking Information</h3>
          <div className="grid grid-cols-2 gap-4">
            <DetailItem label="Date" value={formatDate(booking.date)} />
            <DetailItem
              label="Time"
              value={`${booking.startTime} - ${booking.endTime}`}
            />
            <DetailItem label="Attendees" value={booking.attendees} />
            <DetailItem label="Purpose" value={booking.purpose} />
            {booking.recurrenceRule && (
              <DetailItem
                label="Recurrence"
                value={booking.recurrenceRule}
                fullWidth
              />
            )}
          </div>
        </div>

        {/* Requester Details */}
        <div className="border-t pt-6">
          <h3 className="text-lg font-semibold mb-4">Requester Information</h3>
          <div className="grid grid-cols-2 gap-4">
            <DetailItem
              label="Requested By"
              value={booking.requestingUser?.displayName || booking.requestedBy}
            />
            {booking.bookedFor && (
              <DetailItem
                label="Booking For"
                value={booking.bookedForUser?.displayName || booking.bookedFor}
              />
            )}
          </div>
        </div>

        {/* Check-in Status */}
        {booking.checkedInAt && (
          <div className="border-t pt-6">
            <h3 className="text-lg font-semibold mb-4">Check-in Status</h3>
            <div className="grid grid-cols-2 gap-4">
              <DetailItem
                label="Checked In At"
                value={new Date(booking.checkedInAt).toLocaleString()}
              />
              <DetailItem label="Method" value={booking.checkInMethod} />
            </div>
          </div>
        )}

        {/* Approvals */}
        {booking.approvals && booking.approvals.length > 0 && (
          <div className="border-t pt-6">
            <h3 className="text-lg font-semibold mb-4">Approval Workflow</h3>
            <div className="space-y-3">
              {booking.approvals.map((approval, idx) => (
                <div key={idx} className="bg-gray-50 p-3 rounded-md">
                  <div className="flex justify-between">
                    <div>
                      <p className="text-sm font-medium text-gray-900">
                        {approval.role}
                      </p>
                      <p className="text-xs text-gray-600">
                        Status:{" "}
                        <span className="font-semibold">{approval.status}</span>
                      </p>
                    </div>
                    {approval.approvedAt && (
                      <div className="text-right">
                        <p className="text-xs text-gray-600">
                          Approved by: {approval.approvedBy}
                        </p>
                        <p className="text-xs text-gray-600">
                          {new Date(approval.approvedAt).toLocaleString()}
                        </p>
                      </div>
                    )}
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Action Buttons */}
        <div className="border-t pt-6 space-y-3">
          <div className="flex gap-2 flex-wrap">
            {canApprove() && (
              <button
                onClick={handleApprove}
                disabled={actionLoading}
                className="px-4 py-2 bg-green-600 text-white rounded-md hover:bg-green-700 disabled:bg-gray-400 transition"
              >
                {actionLoading ? "Processing..." : "Approve"}
              </button>
            )}

            {canCheckIn() && (
              <button
                onClick={handleCheckIn}
                disabled={actionLoading}
                className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:bg-gray-400 transition"
              >
                {actionLoading ? "Checking In..." : "Check In"}
              </button>
            )}

            {canApprove() && !showRejectForm && (
              <button
                onClick={() => setShowRejectForm(true)}
                className="px-4 py-2 bg-red-600 text-white rounded-md hover:bg-red-700 transition"
              >
                Reject
              </button>
            )}

            <button
              onClick={onClose}
              className="px-4 py-2 bg-gray-200 text-gray-800 rounded-md hover:bg-gray-300 transition"
            >
              Close
            </button>
          </div>

          {/* Reject Form */}
          {showRejectForm && (
            <div className="mt-4 p-4 bg-red-50 border border-red-200 rounded-md space-y-3">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Rejection Reason <span className="text-red-500">*</span>
                </label>
                <textarea
                  value={rejectReason}
                  onChange={(e) => setRejectReason(e.target.value)}
                  placeholder="Why are you rejecting this booking?"
                  rows="3"
                  className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-red-500 focus:border-red-500"
                />
              </div>
              <div className="flex gap-2">
                <button
                  onClick={handleReject}
                  disabled={actionLoading || !rejectReason.trim()}
                  className="flex-1 px-3 py-2 bg-red-600 text-white rounded-md hover:bg-red-700 disabled:bg-gray-400 transition"
                >
                  {actionLoading ? "Rejecting..." : "Confirm Rejection"}
                </button>
                <button
                  onClick={() => {
                    setShowRejectForm(false);
                    setRejectReason("");
                  }}
                  className="flex-1 px-3 py-2 bg-gray-200 text-gray-800 rounded-md hover:bg-gray-300 transition"
                >
                  Cancel
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

/**
 * DetailItem Component
 * Displays a label-value pair
 */
function DetailItem({ label, value, fullWidth = false }) {
  return (
    <div className={fullWidth ? "col-span-2" : ""}>
      <p className="text-sm text-gray-600">{label}</p>
      <p className="text-base font-medium text-gray-900 mt-1">{value}</p>
    </div>
  );
}

export default BookingDetail;
