import React, { useState, useContext } from "react";
import { AuthContext } from "../../contexts/AuthContext";
import bookingService from "../../services/bookingService";
import { formatTime, formatDate } from "../../types";

/**
 * BookingForm Component
 * Allows users to create booking requests for selected facilities
 * Features:
 * - Form inputs: date, time range, purpose, attendees, recurrence
 * - Admin-only "bookedFor" field
 * - Validation and error handling
 * - Integration with BookingService
 */
function BookingForm({ facility, onBookingCreated, onCancel }) {
  const { user } = useContext(AuthContext);
  const [formData, setFormData] = useState({
    date: "",
    startTime: "",
    endTime: "",
    purpose: "",
    attendees: "",
    recurrenceRule: "",
    bookedFor: "",
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [errors, setErrors] = useState({});
  const [successMessage, setSuccessMessage] = useState(null);

  /**
   * Validate form data
   */
  const validateForm = () => {
    const newErrors = {};

    // Date validation
    if (!formData.date) {
      newErrors.date = "Date is required";
    } else {
      const selectedDate = new Date(formData.date);
      const today = new Date();
      today.setHours(0, 0, 0, 0);
      if (selectedDate < today) {
        newErrors.date = "Cannot book past dates";
      }
    }

    // Time validation
    if (!formData.startTime) {
      newErrors.startTime = "Start time is required";
    }
    if (!formData.endTime) {
      newErrors.endTime = "End time is required";
    }
    if (formData.startTime && formData.endTime) {
      if (formData.startTime >= formData.endTime) {
        newErrors.endTime = "End time must be after start time";
      }
      // Check minimum 15 min duration
      const start = new Date(`2000-01-01T${formData.startTime}`);
      const end = new Date(`2000-01-01T${formData.endTime}`);
      const diffMinutes = (end - start) / (1000 * 60);
      if (diffMinutes < 15) {
        newErrors.endTime = "Booking must be at least 15 minutes";
      }
    }

    // Purpose validation
    if (!formData.purpose || formData.purpose.trim() === "") {
      newErrors.purpose = "Purpose is required";
    }

    // Attendees validation
    if (!formData.attendees) {
      newErrors.attendees = "Number of attendees is required";
    } else {
      const attendees = parseInt(formData.attendees);
      if (isNaN(attendees) || attendees < 1) {
        newErrors.attendees = "Must have at least 1 attendee";
      }
      if (attendees > facility.capacity) {
        newErrors.attendees = `Attendees cannot exceed facility capacity (${facility.capacity})`;
      }
    }

    // Admin-only bookedFor field
    if (
      user?.roles?.includes("ADMIN") &&
      formData.bookedFor &&
      formData.bookedFor.trim() === ""
    ) {
      // Optional field
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  /**
   * Handle form input change
   */
  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: value,
    }));
    // Clear error for this field
    if (errors[name]) {
      setErrors((prev) => ({
        ...prev,
        [name]: "",
      }));
    }
  };

  /**
   * Handle form submission
   */
  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!validateForm()) {
      return;
    }

    setLoading(true);
    setError(null);
    setSuccessMessage(null);

    try {
      const bookingPayload = {
        facilityId: facility.id,
        date: formData.date,
        startTime: formData.startTime,
        endTime: formData.endTime,
        purpose: formData.purpose,
        attendees: parseInt(formData.attendees),
      };

      // Add optional fields
      if (formData.recurrenceRule) {
        bookingPayload.recurrenceRule = formData.recurrenceRule;
      }

      // Admin-only bookedFor field
      if (user?.roles?.includes("ADMIN") && formData.bookedFor) {
        bookingPayload.bookedFor = formData.bookedFor;
      }

      const response = await bookingService.createBooking(bookingPayload);

      setSuccessMessage("Booking request created successfully!");
      setFormData({
        date: "",
        startTime: "",
        endTime: "",
        purpose: "",
        attendees: "",
        recurrenceRule: "",
        bookedFor: "",
      });

      // Callback to parent component
      if (onBookingCreated) {
        onBookingCreated(response);
      }
    } catch (err) {
      // Handle validation errors
      if (err.response?.data?.validationErrors) {
        const fieldErrors = {};
        err.response.data.validationErrors.forEach((ve) => {
          fieldErrors[ve.field] = ve.message;
        });
        setErrors(fieldErrors);
        setError("Please fix the validation errors below");
      } else {
        setError(err.response?.data?.message || "Failed to create booking");
      }
    } finally {
      setLoading(false);
    }
  };

  // Get today's date in YYYY-MM-DD format
  const today = new Date().toISOString().split("T")[0];

  return (
    <div className="bg-white rounded-lg shadow p-6 max-w-2xl mx-auto">
      <div className="mb-6">
        <h2 className="text-2xl font-bold mb-2">Book Facility</h2>
        <div className="bg-blue-50 border-l-4 border-blue-500 p-4 mb-4">
          <p className="font-semibold text-blue-900">{facility.name}</p>
          <p className="text-blue-800 text-sm">
            Capacity: {facility.capacity} | Building: {facility.building}
          </p>
        </div>
      </div>

      {/* Error message */}
      {error && (
        <div className="bg-red-50 border border-red-200 rounded-md p-4 mb-4">
          <p className="text-red-800 text-sm">{error}</p>
        </div>
      )}

      {/* Success message */}
      {successMessage && (
        <div className="bg-green-50 border border-green-200 rounded-md p-4 mb-4">
          <p className="text-green-800 text-sm">{successMessage}</p>
        </div>
      )}

      <form onSubmit={handleSubmit} className="space-y-6">
        {/* Date and Time Row */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          {/* Date */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Date <span className="text-red-500">*</span>
            </label>
            <input
              type="date"
              name="date"
              value={formData.date}
              onChange={handleInputChange}
              min={today}
              className={`w-full px-3 py-2 border rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500 ${
                errors.date ? "border-red-500" : "border-gray-300"
              }`}
            />
            {errors.date && (
              <p className="text-red-500 text-xs mt-1">{errors.date}</p>
            )}
          </div>

          {/* Start Time */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Start Time <span className="text-red-500">*</span>
            </label>
            <input
              type="time"
              name="startTime"
              value={formData.startTime}
              onChange={handleInputChange}
              className={`w-full px-3 py-2 border rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500 ${
                errors.startTime ? "border-red-500" : "border-gray-300"
              }`}
            />
            {errors.startTime && (
              <p className="text-red-500 text-xs mt-1">{errors.startTime}</p>
            )}
          </div>

          {/* End Time */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              End Time <span className="text-red-500">*</span>
            </label>
            <input
              type="time"
              name="endTime"
              value={formData.endTime}
              onChange={handleInputChange}
              className={`w-full px-3 py-2 border rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500 ${
                errors.endTime ? "border-red-500" : "border-gray-300"
              }`}
            />
            {errors.endTime && (
              <p className="text-red-500 text-xs mt-1">{errors.endTime}</p>
            )}
          </div>
        </div>

        {/* Purpose */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            Purpose <span className="text-red-500">*</span>
          </label>
          <input
            type="text"
            name="purpose"
            value={formData.purpose}
            onChange={handleInputChange}
            placeholder="e.g., Lecture, Lab Session, Team Meeting"
            className={`w-full px-3 py-2 border rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500 ${
              errors.purpose ? "border-red-500" : "border-gray-300"
            }`}
          />
          {errors.purpose && (
            <p className="text-red-500 text-xs mt-1">{errors.purpose}</p>
          )}
        </div>

        {/* Attendees */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            Number of Attendees <span className="text-red-500">*</span>
          </label>
          <input
            type="number"
            name="attendees"
            value={formData.attendees}
            onChange={handleInputChange}
            min="1"
            max={facility.capacity}
            placeholder="e.g., 30"
            className={`w-full px-3 py-2 border rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500 ${
              errors.attendees ? "border-red-500" : "border-gray-300"
            }`}
          />
          {errors.attendees && (
            <p className="text-red-500 text-xs mt-1">{errors.attendees}</p>
          )}
        </div>

        {/* Recurrence Rule (Optional) */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            Recurrence (Optional)
          </label>
          <input
            type="text"
            name="recurrenceRule"
            value={formData.recurrenceRule}
            onChange={handleInputChange}
            placeholder="e.g., FREQ=WEEKLY;BYDAY=MO,WE,FR;COUNT=10"
            className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
          />
          <p className="text-gray-500 text-xs mt-1">
            Use RRULE format for recurring bookings
          </p>
        </div>

        {/* Admin-only "Booked For" field */}
        {user?.roles?.includes("ADMIN") && (
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Book For User ID (Optional - Admin Only)
            </label>
            <input
              type="text"
              name="bookedFor"
              value={formData.bookedFor}
              onChange={handleInputChange}
              placeholder="Enter user ID"
              className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
            />
            <p className="text-gray-500 text-xs mt-1">
              Leave empty to book for yourself
            </p>
          </div>
        )}

        {/* Action Buttons */}
        <div className="flex gap-3 pt-4">
          <button
            type="submit"
            disabled={loading}
            className="flex-1 px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:bg-gray-400 disabled:cursor-not-allowed transition font-medium"
          >
            {loading ? "Creating Booking..." : "Create Booking Request"}
          </button>
          <button
            type="button"
            onClick={onCancel}
            disabled={loading}
            className="flex-1 px-4 py-2 bg-gray-200 text-gray-800 rounded-md hover:bg-gray-300 transition font-medium"
          >
            Cancel
          </button>
        </div>
      </form>

      {/* Info Box */}
      <div className="mt-6 p-4 bg-blue-50 rounded-md border border-blue-200">
        <h4 className="font-semibold text-blue-900 mb-2">Important Notes:</h4>
        <ul className="text-sm text-blue-800 space-y-1">
          <li>• Your booking request will be subject to approval workflow</li>
          <li>• Check-in must occur within 15 minutes of start time</li>
          <li>• Failure to check in will be marked as a no-show</li>
          <li>• Peak hours (8:00 AM - 10:00 AM) may have restrictions</li>
        </ul>
      </div>
    </div>
  );
}

export default BookingForm;
