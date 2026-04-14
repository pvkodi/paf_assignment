import React, { useState, useContext, useEffect } from 'react';
import { AuthContext } from '../../contexts/AuthContext';
import { apiClient } from '../../services/apiClient';

/**
 * BookingForm Component
 * Allows users to create booking requests with facility selection, date/time, purpose, attendees,
 * and optional recurrence rules. Supports admin booking on behalf of another user.
 * Implements FR-007, FR-008, FR-009, FR-010, FR-012, FR-013 from the specification.
 */
export default function BookingForm({ facility: initialFacility, onBookingComplete, isModal, onClose }) {
  const { user } = useContext(AuthContext);
  const [facility, setFacility] = useState(initialFacility || null);
  const [bookingDate, setBookingDate] = useState('');
  const [startTime, setStartTime] = useState('');
  const [endTime, setEndTime] = useState('');
  const [purpose, setPurpose] = useState('');
  const [attendees, setAttendees] = useState('');
  const [bookedForUserId, setBookedForUserId] = useState('');
  const [recurrenceRule, setRecurrenceRule] = useState('');
  const [useRecurrence, setUseRecurrence] = useState(false);
  const [recurrencePreset, setRecurrencePreset] = useState('');
  const [recurrenceCount, setRecurrenceCount] = useState('4');
  const [recurrenceCustom, setRecurrenceCustom] = useState(false);
  
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);
  const [validationErrors, setValidationErrors] = useState({});
  const [skippedNotification, setSkippedNotification] = useState(null);
  const [pollingNotifications, setPollingNotifications] = useState(false);

  // Keep local facility state in sync when parent changes the selected facility
  useEffect(() => {
    setFacility(initialFacility || null);
  }, [initialFacility]);

  // Check if user can book for others (has admin or similar role)
  const canBookForOthers = user?.roles?.some(r => 
    ['ADMIN', 'FACILITY_MANAGER', 'LECTURER'].includes(r)
  );

  const validateForm = () => {
    const errors = {};

    if (!facility) errors.facility = 'Please select a facility';
    if (!bookingDate) errors.bookingDate = 'Booking date is required';
    if (!startTime) errors.startTime = 'Start time is required';
    if (!endTime) errors.endTime = 'End time is required';
    if (!purpose || purpose.trim().length < 3) errors.purpose = 'Purpose must be at least 3 characters';
    if (!attendees || Number(attendees) < 1) errors.attendees = 'Attendees must be at least 1';

    // Validate time range
    if (startTime && endTime && startTime >= endTime) {
      errors.timeRange = 'Start time must be before end time';
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
        errors.bookingDate = 'Booking date must be in the future';
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

      const isRecurringRequest = useRecurrence && recurrenceRule;

      const bookingPayload = {
        facilityId: facility.id,
        bookingDate,
        startTime,
        endTime,
        purpose: purpose.trim(),
        attendees: Number(attendees),
      };

      // Add optional fields
      if (canBookForOthers && bookedForUserId) {
        bookingPayload.bookedForUserId = bookedForUserId;
      }

      if (useRecurrence && recurrenceRule) {
        bookingPayload.recurrenceRule = recurrenceRule;
      }

      const response = await apiClient.post('/v1/bookings', bookingPayload);

      setSuccess('Booking created successfully! Your booking is pending approval.');
      const createdBookingId = response?.data?.id;
      const wasRecurringRequest = isRecurringRequest;

      // Reset form
      setFacility(null);
      setBookingDate('');
      setStartTime('');
      setEndTime('');
      setPurpose('');
      setAttendees('');
      setBookedForUserId('');
      setRecurrenceRule('');
      setUseRecurrence(false);

      if (onBookingComplete) {
        onBookingComplete(response.data);
      }

      // Close modal after successful booking
      if (isModal && onClose) {
        setTimeout(() => onClose(), 1500);
      }

      // If this was a recurring booking, poll notifications for any "BOOKING_RECURRING_SKIPPED" message
      if (wasRecurringRequest && createdBookingId) {
        setPollingNotifications(true);
        const maxAttempts = 6;
        const delayMs = 500;

        const findNotification = async () => {
          for (let i = 0; i < maxAttempts; i++) {
            try {
              const notifResp = await apiClient.get('/v1/notifications', { params: { page: 0, size: 10 } });
              const page = notifResp?.data || {};
              const content = Array.isArray(page?.content) ? page.content : (Array.isArray(page) ? page : []);
              const match = content.find(n => n.entityReference === `booking:${createdBookingId}` && n.eventType === 'BOOKING_RECURRING_SKIPPED');
              if (match) {
                setSkippedNotification(match);
                break;
              }
            } catch {
              // ignore transient errors and retry
            }

            // wait before next attempt
            await new Promise(res => setTimeout(res, delayMs));
          }

          setPollingNotifications(false);
        };

        void findNotification();
      }
    } catch (err) {
      console.error('Booking submission error:', err);
      
      // Handle specific error codes
      if (err.response?.status === 409) {
        setError('Booking conflict detected. This time slot may have been booked by another user. Please try again.');
      } else if (err.response?.status === 400) {
        setError(err.response?.data?.message || 'Invalid booking details. Please check your input.');
      } else if (err.response?.status === 403) {
        setError('You do not have permission to submit this booking. You may be suspended.');
      } else {
        setError(err.response?.data?.message || 'Failed to create booking. Please try again.');
      }
    } finally {
      setLoading(false);
    }
  };

  const handleClear = () => {
    setFacility(initialFacility || null);
    setBookingDate('');
    setStartTime('');
    setEndTime('');
    setPurpose('');
    setAttendees('');
    setBookedForUserId('');
    setRecurrenceRule('');
    setUseRecurrence(false);
    setError(null);
    setValidationErrors({});
  };

  if (!facility) {
    return (
      <div className="bg-white rounded-md shadow p-4 border border-slate-100">
        <p className="text-slate-600">Please select a facility to create a booking.</p>
      </div>
    );
  }

  return (
    <div className="bg-white rounded-md shadow p-4 border border-slate-100">
      <h2 className="text-xl font-semibold mb-1 text-slate-900">Create Booking</h2>
      <p className="text-slate-600 mb-4 text-sm">Fill the details below to request this facility.</p>

      <div className="mb-4">
        <p className="text-sm text-slate-700 font-semibold">Selected facility</p>
        <p className="text-lg font-bold text-slate-900">{facility?.name || '—' } {facility?.facilityCode ? `— ${facility.facilityCode}` : ''}</p>
      </div>

      {success && (
        <div className="rounded-md bg-green-50 border border-green-100 p-4 mb-4">
          <p className="font-semibold text-green-800">{success}</p>
          <p className="text-sm text-green-700 mt-1">Your booking is pending approval.</p>
        </div>
      )}

      {pollingNotifications && (
        <div className="rounded-md bg-blue-50 border border-blue-100 p-4 mb-4 text-sm text-blue-800">
          Checking for recurring booking notifications...
        </div>
      )}

      {skippedNotification && (
        <div className="rounded-md bg-yellow-50 border border-yellow-100 p-4 mb-4">
          <h4 className="font-semibold text-yellow-900">{skippedNotification.title}</h4>
          <p className="mt-1 text-sm text-yellow-800">{skippedNotification.message}</p>
          {skippedNotification.actionUrl && (
            <a href={skippedNotification.actionUrl} className="inline-block mt-2 text-yellow-700 font-medium hover:underline text-sm">
              {skippedNotification.actionLabel || 'View booking details'}
            </a>
          )}
        </div>
      )}

      {error && (
        <div className="rounded-md bg-red-50 border border-red-100 p-4 mb-4">
          <p className="font-semibold text-red-800">Booking error</p>
          <p className="text-sm text-red-700 mt-1">{error}</p>
        </div>
      )}

      <div className="mb-4 p-4 bg-slate-50 rounded-md border border-slate-100 flex justify-between items-center gap-4">
        <div className="min-w-0">
          <p className="text-sm text-slate-500 font-medium">Selected facility</p>
          <p className="text-sm font-semibold text-slate-900 line-clamp-1">{facility.name}</p>
          <p className="text-xs text-slate-500 mt-1 line-clamp-1">{facility.location}, {facility.building} • Floor {facility.floor}</p>
        </div>
        <div className="text-right flex-shrink-0">
          <div className="text-lg font-bold text-indigo-600">{facility.capacity}</div>
          <div className="text-xs text-slate-500">Capacity</div>
        </div>
      </div>

      <form onSubmit={handleSubmit} className="space-y-4">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {/* Booking Date */}
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">
              Booking Date *
            </label>
            <input
              type="date"
              value={bookingDate}
              onChange={(e) => setBookingDate(e.target.value)}
              className={`w-full px-3 py-2 border rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500 ${
                validationErrors.bookingDate
                  ? 'border-red-300 bg-red-50'
                  : 'border-slate-300'
              }`}
            />
            {validationErrors.bookingDate && (
              <p className="text-sm text-red-600 mt-1">{validationErrors.bookingDate}</p>
            )}
          </div>

          {/* Attendees */}
          <div>
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
                  ? 'border-red-300 bg-red-50'
                  : 'border-slate-300'
              }`}
            />
            {validationErrors.attendees && (
              <p className="text-sm text-red-600 mt-1">{validationErrors.attendees}</p>
            )}
          </div>

          {/* Start Time */}
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">
              Start Time *
            </label>
            <input
              type="time"
              value={startTime}
              onChange={(e) => setStartTime(e.target.value)}
              className={`w-full px-3 py-2 border rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500 ${
                validationErrors.startTime
                  ? 'border-red-300 bg-red-50'
                  : 'border-slate-300'
              }`}
            />
            {validationErrors.startTime && (
              <p className="text-sm text-red-600 mt-1">{validationErrors.startTime}</p>
            )}
          </div>

          {/* End Time */}
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">
              End Time *
            </label>
            <input
              type="time"
              value={endTime}
              onChange={(e) => setEndTime(e.target.value)}
              className={`w-full px-3 py-2 border rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500 ${
                validationErrors.endTime
                  ? 'border-red-300 bg-red-50'
                  : 'border-slate-300'
              }`}
            />
            {validationErrors.endTime && (
              <p className="text-sm text-red-600 mt-1">{validationErrors.endTime}</p>
            )}
          </div>
        </div>

        {/* Time Range Error */}
        {validationErrors.timeRange && (
          <div className="rounded-md bg-red-50 p-3 border border-red-200">
            <p className="text-sm text-red-600">{validationErrors.timeRange}</p>
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
                ? 'border-red-300 bg-red-50'
                : 'border-slate-300'
            }`}
          />
          {validationErrors.purpose && (
            <p className="text-sm text-red-600 mt-1">{validationErrors.purpose}</p>
          )}
        </div>

        {/* Admin Booking For Another User */}
        {canBookForOthers && (
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">
              Book for Another User (Optional - Admin/Lecturer only)
            </label>
            <input
              type="text"
              value={bookedForUserId}
              onChange={(e) => setBookedForUserId(e.target.value)}
              placeholder="Enter user ID (leave blank to book for yourself)"
              className="w-full px-3 py-2 border border-slate-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
            />
          </div>
        )}

        {/* Recurrence Section */}
        <div className="border-t-2 border-slate-200 pt-4">
          <label className="flex items-center">
            <input
              type="checkbox"
              checked={useRecurrence}
              onChange={(e) => setUseRecurrence(e.target.checked)}
              className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-slate-300 rounded"
            />
            <span className="ml-2 text-sm font-medium text-slate-700">
              This is a recurring booking
            </span>
          </label>

          {useRecurrence && (
            <div className="mt-4 p-4 bg-blue-50 rounded-lg border border-blue-200">
              {/* Recurrence Presets */}
              <div className="mb-4">
                <label className="block text-sm font-medium text-slate-700 mb-2">
                  Recurrence Pattern
                </label>
                <div className="grid grid-cols-2 gap-2">
                  <button
                    type="button"
                    onClick={() => {
                      setRecurrencePreset('daily');
                      setRecurrenceCustom(false);
                      setRecurrenceRule(`FREQ=DAILY;COUNT=${recurrenceCount}`);
                    }}
                    className={`px-3 py-2 rounded-md text-sm font-medium transition-colors ${
                      recurrencePreset === 'daily'
                        ? 'bg-blue-600 text-white'
                        : 'bg-white border border-slate-300 text-slate-700 hover:bg-slate-50'
                    }`}
                  >
                    Daily
                  </button>
                  <button
                    type="button"
                    onClick={() => {
                      setRecurrencePreset('weekly');
                      setRecurrenceCustom(false);
                      setRecurrenceRule(`FREQ=WEEKLY;BYDAY=MO,WE,FR;COUNT=${recurrenceCount}`);
                    }}
                    className={`px-3 py-2 rounded-md text-sm font-medium transition-colors ${
                      recurrencePreset === 'weekly'
                        ? 'bg-blue-600 text-white'
                        : 'bg-white border border-slate-300 text-slate-700 hover:bg-slate-50'
                    }`}
                  >
                    Weekly (M/W/F)
                  </button>
                  <button
                    type="button"
                    onClick={() => {
                      setRecurrencePreset('biweekly');
                      setRecurrenceCustom(false);
                      setRecurrenceRule(`FREQ=WEEKLY;INTERVAL=2;COUNT=${recurrenceCount}`);
                    }}
                    className={`px-3 py-2 rounded-md text-sm font-medium transition-colors ${
                      recurrencePreset === 'biweekly'
                        ? 'bg-blue-600 text-white'
                        : 'bg-white border border-slate-300 text-slate-700 hover:bg-slate-50'
                    }`}
                  >
                    Bi-Weekly
                  </button>
                  <button
                    type="button"
                    onClick={() => {
                      setRecurrencePreset('monthly');
                      setRecurrenceCustom(false);
                      setRecurrenceRule(`FREQ=MONTHLY;COUNT=${recurrenceCount}`);
                    }}
                    className={`px-3 py-2 rounded-md text-sm font-medium transition-colors ${
                      recurrencePreset === 'monthly'
                        ? 'bg-blue-600 text-white'
                        : 'bg-white border border-slate-300 text-slate-700 hover:bg-slate-50'
                    }`}
                  >
                    Monthly
                  </button>
                </div>
              </div>

              {/* Recurrence Count */}
              {recurrencePreset && (
                <div className="mb-4">
                  <label className="block text-sm font-medium text-slate-700 mb-1">
                    Number of Occurrences
                  </label>
                  <input
                    type="number"
                    min="1"
                    max="52"
                    value={recurrenceCount}
                    onChange={(e) => {
                      setRecurrenceCount(e.target.value);
                      if (recurrencePreset === 'daily') {
                        setRecurrenceRule(`FREQ=DAILY;COUNT=${e.target.value}`);
                      } else if (recurrencePreset === 'weekly') {
                        setRecurrenceRule(`FREQ=WEEKLY;BYDAY=MO,WE,FR;COUNT=${e.target.value}`);
                      } else if (recurrencePreset === 'biweekly') {
                        setRecurrenceRule(`FREQ=WEEKLY;INTERVAL=2;COUNT=${e.target.value}`);
                      } else if (recurrencePreset === 'monthly') {
                        setRecurrenceRule(`FREQ=MONTHLY;COUNT=${e.target.value}`);
                      }
                    }}
                    className="w-full px-3 py-2 border border-slate-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                  />
                </div>
              )}

              {/* Custom Rule Option */}
              <div className="mb-3">
                <label className="flex items-center text-sm">
                  <input
                    type="checkbox"
                    checked={recurrenceCustom}
                    onChange={(e) => setRecurrenceCustom(e.target.checked)}
                    className="h-4 w-4 text-orange-600 focus:ring-orange-500 border-slate-300 rounded"
                  />
                  <span className="ml-2 text-slate-700 font-medium">
                    Advanced: Custom iCal RRULE
                  </span>
                </label>
              </div>

              {recurrenceCustom && (
                <div>
                  <input
                    type="text"
                    value={recurrenceRule}
                    onChange={(e) => setRecurrenceRule(e.target.value)}
                    placeholder="e.g., FREQ=WEEKLY;BYDAY=MO,WE,FR;COUNT=12"
                    className="w-full px-3 py-2 border border-slate-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500 font-mono text-sm"
                  />
                  <p className="text-xs text-slate-500 mt-1">
                    See <a href="https://datatracker.ietf.org/doc/html/rfc5545" target="_blank" rel="noreferrer" className="text-blue-600 hover:underline">RFC 5545</a> for RRULE format
                  </p>
                </div>
              )}

              {recurrenceRule && (
                <div className="mt-3 p-2 bg-white rounded border border-blue-200 text-xs text-slate-600">
                  <strong>Rule:</strong> <code className="text-slate-700">{recurrenceRule}</code>
                </div>
              )}
            </div>
          )}
        </div>

        {/* Form Actions */}
        <div className="flex gap-3 pt-4 border-t border-slate-200">
          <button
            type="submit"
            disabled={loading}
            className="flex-1 px-4 py-2 bg-indigo-600 text-white font-semibold rounded-md hover:bg-indigo-700 disabled:opacity-50 disabled:cursor-not-allowed transition"
          >
            {loading ? 'Submitting...' : 'Create Booking'}
          </button>
          <button
            type="button"
            onClick={handleClear}
            className="px-4 py-2 bg-slate-100 text-slate-700 font-medium rounded-md hover:bg-slate-200 transition"
          >
            Clear
          </button>
        </div>
      </form>
    </div>
  );
}
