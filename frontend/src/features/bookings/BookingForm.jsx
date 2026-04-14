import React, { useState, useContext, useEffect } from "react";
import { AuthContext } from "../../contexts/AuthContext";
import { apiClient } from "../../services/apiClient";
import RecurrenceSelector from "./RecurrenceSelector";
import QuotaPolicySummary from "./QuotaPolicySummary";
import AdminBookForUserSelector from "./AdminBookForUserSelector";

/**
 * BookingForm Component
 * Allows users to create booking requests with facility selection, date/time, purpose, attendees,
 * and optional recurrence rules. Supports admin booking on behalf of another user.
 * Implements FR-007, FR-008, FR-009, FR-010, FR-012, FR-013 from the specification.
 */
export default function BookingForm({
  facility: initialFacility,
  onBookingComplete,
}) {
  const { user } = useContext(AuthContext);
  const [facility, setFacility] = useState(initialFacility || null);
  const [bookingDate, setBookingDate] = useState("");
  const [startTime, setStartTime] = useState("");
  const [endTime, setEndTime] = useState("");
  const [purpose, setPurpose] = useState("");
  const [attendees, setAttendees] = useState("");
  const [bookedForUserId, setBookedForUserId] = useState("");
  const [recurrenceRule, setRecurrenceRule] = useState("");

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);
  const [validationErrors, setValidationErrors] = useState({});

  // Sync facility prop with local state
  useEffect(() => {
    setFacility(initialFacility || null);
  }, [initialFacility]);

  // Check if user can book for others (has admin or similar role)
  const canBookForOthers = user?.roles?.some((r) =>
    ["ADMIN", "FACILITY_MANAGER"].includes(r),
  );

  // Get user role for quota display
  const userRole = user?.roles?.[0] || "USER";

  const validateForm = () => {
    const errors = {};

    if (!facility) errors.facility = "Please select a facility";
    if (!bookingDate) errors.bookingDate = "Booking date is required";
    if (!startTime) errors.startTime = "Start time is required";
    if (!endTime) errors.endTime = "End time is required";
    if (!purpose || purpose.trim().length < 3)
      errors.purpose = "Purpose must be at least 3 characters";
    if (!attendees || Number(attendees) < 1)
      errors.attendees = "Attendees must be at least 1";

    // Validate time range
    if (startTime && endTime && startTime >= endTime) {
      errors.timeRange = "Start time must be before end time";
    }

    // Validate attendees don't exceed capacity
    if (facility && attendees && Number(attendees) > facility.capacity) {
      errors.attendees = `Attendees (${attendees}) cannot exceed facility capacity (${facility.capacity})`;
    }

    // Validate booking date is in future
    if (bookingDate) {
      const selectedDate = new Date(bookingDate);
      const today = new Date();
      today.setHours(0, 0, 0, 0);
      if (selectedDate < today) {
        errors.bookingDate = "Booking date must be in the future";
      }
    }

    setValidationErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    setSuccess(null);

    if (!validateForm()) {
      return;
    }

    try {
      setLoading(true);

      const bookingPayload = {
        facility_id: facility.id,
        booking_date: bookingDate,
        start_time: startTime,
        end_time: endTime,
        purpose: purpose.trim(),
        attendees: Number(attendees),
      };

      // Add optional fields
      if (canBookForOthers && bookedForUserId) {
        bookingPayload.booked_for_user_id = bookedForUserId;
      }

      if (recurrenceRule) {
        bookingPayload.recurrence_rule = recurrenceRule;
      }

      const response = await apiClient.post("/v1/bookings", bookingPayload);

      setSuccess(
        "Booking created successfully! Your booking is pending approval.",
      );
      setError(null);
      setValidationErrors({});

      // Reset form after a delay so user can see success message
      setTimeout(() => {
        setFacility(null);
        setBookingDate("");
        setStartTime("");
        setEndTime("");
        setPurpose("");
        setBookedForUserId("");
        setRecurrenceRule("");
        setUseRecurrence(false);
        setSuccess(null);
      }, 3000);

      if (onBookingComplete) {
        onBookingComplete(response.data);
      }
    } catch (err) {
      console.error("Booking submission error:", err);

      // Handle specific error codes
      if (err.response?.status === 409) {
        setError(
          "Booking conflict detected. This time slot may have been booked by another user. Please try again.",
        );
      } else if (err.response?.status === 400) {
        setError(
          err.response?.data?.message ||
            "Invalid booking details. Please check your input.",
        );
      } else if (err.response?.status === 403) {
        setError(
          "You do not have permission to submit this booking. You may be suspended.",
        );
      } else {
        setError(
          err.response?.data?.message ||
            "Failed to create booking. Please try again.",
        );
      }
    } finally {
      setLoading(false);
    }
  };

  const handleClear = () => {
    setFacility(initialFacility || null);
    setBookingDate("");
    setStartTime("");
    setEndTime("");
    setPurpose("");
    setAttendees("");
    setBookedForUserId("");
    setRecurrenceRule("");
    setUseRecurrence(false);
    setValidationErrors({});
  };

  if (!facility && !success) {
    return (
      <div className="bg-white rounded-lg shadow-md p-6">
        <p className="text-slate-600">
          Please select a facility to create a booking.
        </p>
      </div>
    );
  }

  return (
    <div className="bg-white rounded-lg shadow-md p-6">
      <h2 className="text-2xl font-bold mb-4 text-slate-900">Create Booking</h2>

      {/* Success Message */}
      {success && (
        <div className="rounded-md bg-green-50 p-4 mb-4 border border-green-200">
          <p className="text-sm font-medium text-green-800">{success}</p>
        </div>
      )}

      {/* Error Message */}
      {error && (
        <div className="rounded-md bg-red-50 p-4 mb-4">
          <p className="text-sm font-medium text-red-800">{error}</p>
        </div>
      )}

      {facility && (
        <>
          {/* Selected Facility Display */}
          <div className="mb-6 p-4 bg-blue-50 rounded-lg border border-blue-200">
            <h3 className="font-semibold text-slate-900">Selected Facility</h3>
            <p className="text-slate-700 mt-1">
              <strong>{facility.name}</strong> - Capacity: {facility.capacity}
            </p>
            <p className="text-slate-600 text-sm">
              {facility.location}, {facility.building} (Floor: {facility.floor})
            </p>
          </div>

          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              {/* Quota Policy Summary */}
              <div className="md:col-span-1">
                <QuotaPolicySummary userRole={userRole} compact={true} />
              </div>

              {/* Admin Book For User Selector */}
              {canBookForOthers && (
                <div className="md:col-span-1">
                  <AdminBookForUserSelector
                    onUserSelect={setBookedForUserId}
                    userRole={userRole}
                  />
                </div>
              )}

              {/* Date/Time Inputs Group */}
              <div className={canBookForOthers ? "md:col-span-1" : "md:col-span-2"}>
                {/* Booking Date */}
                <div className="mb-4">
                  <label className="block text-sm font-medium text-slate-700 mb-1">
                    Booking Date *
                  </label>
                  <input
                    type="date"
                    value={bookingDate}
                    onChange={(e) => setBookingDate(e.target.value)}
                    className={`w-full px-3 py-2 border rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500 ${
                      validationErrors.bookingDate
                        ? "border-red-300 bg-red-50"
                        : "border-slate-300"
                    }`}
                  />
                  {validationErrors.bookingDate && (
                    <p className="text-sm text-red-600 mt-1">
                      {validationErrors.bookingDate}
                    </p>
                  )}
                </div>

                {/* Attendees */}
                <div className="mb-4">
                  <label className="block text-sm font-medium text-slate-700 mb-1">
                    Number of Attendees * (Max: {facility.capacity})
                  </label>
                  <input
                    type="number"
                    min="1"
                    max={facility.capacity}
                    value={attendees}
                    onChange={(e) => setAttendees(e.target.value)}
                    className={`w-full px-3 py-2 border rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500 ${
                      validationErrors.attendees
                        ? "border-red-300 bg-red-50"
                        : "border-slate-300"
                    }`}
                  />
                  {validationErrors.attendees && (
                    <p className="text-sm text-red-600 mt-1">
                      {validationErrors.attendees}
                    </p>
                  )}
                </div>

                {/* Start Time */}
                <div className="mb-4">
                  <label className="block text-sm font-medium text-slate-700 mb-1">
                    Start Time *
                  </label>
                  <input
                    type="time"
                    value={startTime}
                    onChange={(e) => setStartTime(e.target.value)}
                    className={`w-full px-3 py-2 border rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500 ${
                      validationErrors.startTime
                        ? "border-red-300 bg-red-50"
                        : "border-slate-300"
                    }`}
                  />
                  {validationErrors.startTime && (
                    <p className="text-sm text-red-600 mt-1">
                      {validationErrors.startTime}
                    </p>
                  )}
                </div>

                {/* End Time */}
                <div className="mb-4">
                  <label className="block text-sm font-medium text-slate-700 mb-1">
                    End Time *
                  </label>
                  <input
                    type="time"
                    value={endTime}
                    onChange={(e) => setEndTime(e.target.value)}
                    className={`w-full px-3 py-2 border rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500 ${
                      validationErrors.endTime
                        ? "border-red-300 bg-red-50"
                        : "border-slate-300"
                    }`}
                  />
                  {validationErrors.endTime && (
                    <p className="text-sm text-red-600 mt-1">
                      {validationErrors.endTime}
                    </p>
                  )}
                </div>
              </div>
            </div>

            {/* Time Range Error */}
            {validationErrors.timeRange && (
              <div className="rounded-md bg-red-50 p-3 border border-red-200">
                <p className="text-sm text-red-600">
                  {validationErrors.timeRange}
                </p>
              </div>
            )}

            {/* Purpose */}
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">
                Purpose of Booking * (Min 3 characters)
              </label>
              <textarea
                value={purpose}
                onChange={(e) => setPurpose(e.target.value)}
                rows="3"
                placeholder="Describe the purpose of this booking..."
                className={`w-full px-3 py-2 border rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500 ${
                  validationErrors.purpose
                    ? "border-red-300 bg-red-50"
                    : "border-slate-300"
                }`}
              />
              {validationErrors.purpose && (
                <p className="text-sm text-red-600 mt-1">
                  {validationErrors.purpose}
                </p>
              )}
            </div>

            {/* Admin Booking For Another User */}
            {canBookForOthers && (
              <AdminBookForUserSelector
                onUserSelect={setBookedForUserId}
                userRole={userRole}
              />
            )}

            {/* Recurrence Section */}
            <div className="border-t-2 border-slate-200 pt-4">
              <h3 className="text-sm font-semibold text-slate-900 mb-4">
                🔄 Recurrence Options
              </h3>
              <RecurrenceSelector
                onRuleChange={setRecurrenceRule}
                maxOccurrences={52}
              />
            </div>

            {/* Form Actions */}
            <div className="flex gap-2 pt-4">
              <button
                type="submit"
                disabled={loading}
                className="px-4 py-2 bg-blue-600 text-white font-medium rounded-md hover:bg-blue-700 disabled:bg-blue-400 disabled:cursor-not-allowed transition-colors"
              >
                {loading ? "Submitting..." : "Submit Booking"}
              </button>
              <button
                type="button"
                onClick={handleClear}
                className="px-4 py-2 bg-slate-200 text-slate-700 font-medium rounded-md hover:bg-slate-300 transition-colors"
              >
                Clear
              </button>
            </div>
          </form>
        </>
      )}
    </div>
  );
}
