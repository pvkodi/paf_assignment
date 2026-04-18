import React, { useState, useContext, useEffect } from "react";
import { AuthContext } from "../../contexts/AuthContext";
import { apiClient } from "../../services/apiClient";
import RecurrenceSelector from "./RecurrenceSelector";
import AdminBookForUserSelector from "./AdminBookForUserSelector";

export default function BookingForm({ facility: initialFacility, onBookingComplete, isModal, onClose }) {
  const { user } = useContext(AuthContext);
  const [facility, setFacility] = useState(initialFacility || null);
  const [bookingDate, setBookingDate] = useState("");
  const [startTime, setStartTime] = useState("");
  const [endTime, setEndTime] = useState("");
  const [purpose, setPurpose] = useState("");
  const [attendees, setAttendees] = useState("");
  const [bookedForUserId, setBookedForUserId] = useState("");
  const [recurrenceRule, setRecurrenceRule] = useState("");
  const [useRecurrence, setUseRecurrence] = useState(false);
  const [recurrencePreset, setRecurrencePreset] = useState("");
  const [recurrenceCount, setRecurrenceCount] = useState("4");
  const [recurrenceCustom, setRecurrenceCustom] = useState(false);

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);
  const [validationErrors, setValidationErrors] = useState({});
  const [skippedNotification, setSkippedNotification] = useState(null);
  const [pollingNotifications, setPollingNotifications] = useState(false);

  // Availability state
  const [availabilityData, setAvailabilityData] = useState(null);
  const [availabilityLoading, setAvailabilityLoading] = useState(false);
  const [availabilityError, setAvailabilityError] = useState(null);

  useEffect(() => {
    setFacility(initialFacility || null);
  }, [initialFacility]);

  // Fetch availability slots when facility and date are selected
  useEffect(() => {
    if (facility && bookingDate) {
      fetchAvailabilitySlots();
    } else {
      setAvailabilityData(null);
    }
  }, [facility, bookingDate]);

  const fetchAvailabilitySlots = async () => {
    try {
      setAvailabilityLoading(true);
      setAvailabilityError(null);
      
      const response = await apiClient.get(`/v1/bookings/availability/${facility.id}`, {
        params: { date: bookingDate }
      });
      
      console.log("Availability response:", response.data);
      console.log("freeslots: ", Array.isArray(response.data) && response.data.length > 0 ? response.data[0].freeSlots : "N/A");
      
      if (Array.isArray(response.data) && response.data.length > 0) {
        const facilityAvailability = response.data[0];
        console.log("Availability debug:", {
          facilityId: facilityAvailability.facilityId,
          date: bookingDate,
          bookedSlots: facilityAvailability.bookedSlots?.length || 0,
          freeSlots: facilityAvailability.freeSlots?.length || 0,
          sampleBooked: facilityAvailability.bookedSlots?.[0],
          sampleFree: facilityAvailability.freeSlots?.[0]
        });
        
        setAvailabilityData(facilityAvailability);
      }
    } catch (err) {
      console.error("Failed to fetch availability:", err);
      setAvailabilityError("Unable to load availability information");
      setAvailabilityData(null);
    } finally {
      setAvailabilityLoading(false);
    }
  };

  const canBookForOthers = user?.roles?.some((r) =>
    ["ADMIN", "FACILITY_MANAGER"].includes(r),
  );

  // Get today's date in YYYY-MM-DD format for date input min attribute
  const getTodayDate = () => {
    const today = new Date();
    const year = today.getFullYear();
    const month = String(today.getMonth() + 1).padStart(2, '0');
    const day = String(today.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  };
  const minDate = getTodayDate();

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

    if (startTime && endTime && startTime >= endTime) {
      errors.timeRange = "Start time must be before end time";
    }

    if (facility && attendees && Number(attendees) > facility.capacity) {
      errors.attendees = `Attendees (${attendees}) cannot exceed facility capacity (${facility.capacity})`;
    }

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

    if (!validateForm()) return;

    try {
      setLoading(true);

      const isRecurringRequest = useRecurrence && recurrenceRule;

      const bookingPayload = {
        facility_id: facility.id,
        booking_date: bookingDate,
        start_time: startTime,
        end_time: endTime,
        purpose: purpose.trim(),
        attendees: Number(attendees),
      };

      if (canBookForOthers && bookedForUserId) {
        bookingPayload.booked_for_user_id = bookedForUserId;
      }

      if (recurrenceRule) {
        bookingPayload.recurrence_rule = recurrenceRule;
      }

      const response = await apiClient.post("/v1/bookings", bookingPayload);

      setSuccess("✓ Booking submitted successfully! Your booking is now being reviewed.");
      setError(null);
      setValidationErrors({});

      const createdBookingId = response?.data?.id;
      const wasRecurringRequest = isRecurringRequest;

      // Reset form after a short delay so user notices success
      setTimeout(() => {
        setFacility(null);
        setBookingDate("");
        setStartTime("");
        setEndTime("");
        setPurpose("");
        setBookedForUserId("");
        setRecurrenceRule("");
        setUseRecurrence(false);
        setRecurrencePreset("");
        setRecurrenceCount("4");
        setRecurrenceCustom(false);
        setSuccess(null);
      }, 2500);

      if (onBookingComplete) onBookingComplete(response.data);

      if (isModal && onClose) setTimeout(() => onClose(), 1500);

      if (wasRecurringRequest && createdBookingId) {
        setPollingNotifications(true);
        const maxAttempts = 6;
        const delayMs = 500;

        const findNotification = async () => {
          for (let i = 0; i < maxAttempts; i++) {
            try {
              const notifResp = await apiClient.get("/v1/notifications", { params: { page: 0, size: 10 } });
              const page = notifResp?.data || {};
              const content = Array.isArray(page?.content) ? page.content : (Array.isArray(page) ? page : []);
              const match = content.find(n => n.entityReference === `booking:${createdBookingId}` && n.eventType === 'BOOKING_RECURRING_SKIPPED');
              if (match) {
                setSkippedNotification(match);
                break;
              }
            } catch {
              // ignore
            }
            await new Promise(res => setTimeout(res, delayMs));
          }
          setPollingNotifications(false);
        };

        void findNotification();
      }
    } catch (err) {
      console.error("Booking submission error:", err);
      if (err.response?.status === 409) {
        setError("Booking conflict detected. This time slot may have been booked by another user. Please try again.");
      } else if (err.response?.status === 400) {
        setError(err.response?.data?.message || "Invalid booking details. Please check your input.");
      } else if (err.response?.status === 403) {
        setError("You do not have permission to submit this booking. You may be suspended.");
      } else {
        setError(err.response?.data?.message || "Failed to create booking. Please try again.");
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
    setRecurrencePreset("");
    setRecurrenceCount("4");
    setRecurrenceCustom(false);
    setError(null);
    setValidationErrors({});
  };

  if (!facility && !success) {
    return (
      <div className={`bg-white rounded-[2rem] shadow-[0_8px_30px_rgb(0,0,0,0.04)] border border-slate-100 p-8 ${isModal ? 'h-[90vh] overflow-y-auto flex items-center justify-center' : ''}`}>
        <div className="text-center">
          <div className="w-16 h-16 bg-slate-50 flex items-center justify-center rounded-full mx-auto mb-4 border border-slate-100">
            <svg className="w-8 h-8 text-slate-300" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" /></svg>
          </div>
          <p className="text-slate-500 font-medium text-lg">Please select a facility back in the list to create a booking.</p>
        </div>
      </div>
    );
  }

  const rootClass = isModal
    ? 'bg-transparent h-full flex flex-col p-0'
    : 'bg-white rounded-[2rem] shadow-[0_8px_30px_rgb(0,0,0,0.04)] p-8 border border-slate-100';

  const selectedBoxClass = isModal
    ? 'mb-6 p-5 flex justify-between items-center gap-4 bg-slate-50 rounded-2xl border border-slate-100/50'
    : 'mb-6 p-5 bg-slate-50 rounded-2xl border border-slate-100 flex justify-between items-center gap-4';

  return (
    <div className={rootClass}>
      <div className="mb-8 border-b border-slate-100 pb-4">
        <h2 className="text-3xl font-bold tracking-tight text-slate-900 mb-2">Create Booking</h2>
        <p className="text-slate-500 font-medium">Verify your facility and set your schedule below.</p>
      </div>

      <div className="flex-1 overflow-auto">
        {success && (
          <div className="rounded-md bg-green-50 p-4 mb-4 border border-green-200">
            <p className="text-sm font-medium text-green-800">{success}</p>
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

        {facility && (
          <>
            <div className={selectedBoxClass}>
              <div className="min-w-0">
                <p className="text-xs font-bold text-slate-400 uppercase tracking-wider mb-1">Selected facility</p>
                <p className="text-lg font-bold text-slate-900 truncate">{facility.name}</p>
                <p className="text-sm font-medium text-slate-500 mt-1 truncate">
                  <svg className="w-3.5 h-3.5 inline mr-1 text-slate-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" /><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" /></svg>
                  {facility.location}, {facility.building} • Fl {facility.floor}
                </p>
              </div>
              <div className="text-right flex-shrink-0 bg-white p-3 rounded-xl border border-slate-100 shadow-[0_2px_10px_rgb(0,0,0,0.02)]">
                <div className="text-2xl font-black text-slate-900 leading-none">{facility.capacity}</div>
                <div className="text-[10px] font-bold text-slate-400 uppercase tracking-widest mt-1">Capacity</div>
              </div>
            </div>

            <form onSubmit={handleSubmit} className="space-y-6">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div>
                  <label className="block text-sm font-bold text-slate-700 mb-2">Date *</label>
                  <input 
                    type="date" 
                    min={minDate}
                    value={bookingDate} 
                    onChange={(e) => setBookingDate(e.target.value)} 
                    className={`w-full px-4 py-3 bg-slate-50 border rounded-xl font-medium text-slate-800 transition-all focus:bg-white focus:outline-none focus:ring-4 focus:ring-slate-100 ${validationErrors.bookingDate ? 'border-red-300 bg-red-50 focus:ring-red-100' : 'border-transparent focus:border-slate-300'}`} 
                  />
                  {validationErrors.bookingDate && <p className="text-xs font-semibold text-red-500 mt-2">{validationErrors.bookingDate}</p>}
                </div>

                <div>
                  <label className="block text-sm font-bold text-slate-700 mb-2">Attendees * <span className="text-slate-400 font-medium ml-1">(Max {facility.capacity})</span></label>
                  <input type="number" min="1" max={facility.capacity} value={attendees} onChange={(e) => setAttendees(e.target.value)} placeholder="0" className={`w-full px-4 py-3 bg-slate-50 border rounded-xl font-medium text-slate-800 transition-all focus:bg-white focus:outline-none focus:ring-4 focus:ring-slate-100 ${validationErrors.attendees ? 'border-red-300 bg-red-50 focus:ring-red-100' : 'border-transparent focus:border-slate-300'}`} />
                  {validationErrors.attendees && <p className="text-xs font-semibold text-red-500 mt-2">{validationErrors.attendees}</p>}
                </div>

                {/* Availability Display Section */}
                {bookingDate && (
                  <div className="col-span-full">
                    {availabilityLoading && (
                      <div className="rounded-lg border border-blue-200 bg-blue-50 p-4">
                        <p className="text-sm text-blue-700">Loading available time slots...</p>
                      </div>
                    )}

                    {availabilityError && (
                      <div className="rounded-lg border border-amber-200 bg-amber-50 p-4">
                        <p className="text-sm text-amber-700">{availabilityError}</p>
                      </div>
                    )}

                    {availabilityData && !availabilityLoading && (
                      <div className="rounded-lg border border-slate-200 bg-slate-50 p-4">
                        <h4 className="text-sm font-semibold text-slate-900 mb-3">Available Time Slots</h4>
                        
                        {availabilityData.bookedSlots && availabilityData.bookedSlots.length > 0 && (
                          <div className="mb-4 p-3 bg-amber-50 border border-amber-200 rounded-md">
                            <p className="text-xs font-semibold text-amber-900 mb-2">Booked Times:</p>
                            <div className="space-y-1">
                              {availabilityData.bookedSlots.map((slot, idx) => (
                                <div key={idx} className="text-xs text-amber-800">
                                  • {slot.startTime} - {slot.endTime}
                                  {slot.purpose && <span className="ml-2 text-amber-700">({slot.purpose})</span>}
                                </div>
                              ))}
                            </div>
                          </div>
                        )}

                        {availabilityData.freeSlots && availabilityData.freeSlots.length > 0 ? (
                          <div className="p-3 bg-green-50 border border-green-200 rounded-md">
                            <p className="text-xs font-semibold text-green-900 mb-2">Free Time Slots ({availabilityData.freeSlots.length}):</p>
                            <div className="space-y-2">
                              {availabilityData.freeSlots.map((slot, idx) => (
                                <button
                                  key={idx}
                                  type="button"
                                  onClick={() => {
                                    setStartTime(slot.startTime.substring(0, 5));
                                    setEndTime(slot.endTime.substring(0, 5));
                                  }}
                                  className="w-full text-left px-4 py-3 bg-white border border-green-200 rounded-xl hover:bg-green-50 hover:border-green-300 shadow-[0_2px_8px_rgb(0,0,0,0.02)] transition-all text-sm text-green-800 font-bold flex justify-between items-center group"
                                >
                                  <span>{slot.startTime} - {slot.endTime}</span>
                                  <span className="opacity-0 group-hover:opacity-100 transition-opacity text-xs bg-green-200 text-green-900 px-2 py-1 rounded-md">Use Slot</span>
                                </button>
                              ))}
                            </div>
                            <p className="text-xs text-green-700 mt-3">Click on any slot above to fill in start and end times.</p>
                          </div>
                        ) : (
                          <div className="p-3 bg-red-50 border border-red-200 rounded-md">
                            <p className="text-xs text-red-700 font-medium">No free time slots available for this date.</p>
                          </div>
                        )}

                        <p className="text-xs text-slate-500 mt-3">
                          Facility available: {availabilityData.availabilityStart} - {availabilityData.availabilityEnd}
                        </p>
                      </div>
                    )}
                  </div>
                )}

                <div>
                  <label className="block text-sm font-bold text-slate-700 mb-2">Start Time *</label>
                  <input type="time" value={startTime} onChange={(e) => setStartTime(e.target.value)} className={`w-full px-4 py-3 bg-slate-50 border rounded-xl font-medium text-slate-800 transition-all focus:bg-white focus:outline-none focus:ring-4 focus:ring-slate-100 ${validationErrors.startTime ? 'border-red-300 bg-red-50 focus:ring-red-100' : 'border-transparent focus:border-slate-300'}`} />
                  {validationErrors.startTime && <p className="text-xs font-semibold text-red-500 mt-2">{validationErrors.startTime}</p>}
                </div>

                <div>
                  <label className="block text-sm font-bold text-slate-700 mb-2">End Time *</label>
                  <input type="time" value={endTime} onChange={(e) => setEndTime(e.target.value)} className={`w-full px-4 py-3 bg-slate-50 border rounded-xl font-medium text-slate-800 transition-all focus:bg-white focus:outline-none focus:ring-4 focus:ring-slate-100 ${validationErrors.endTime ? 'border-red-300 bg-red-50 focus:ring-red-100' : 'border-transparent focus:border-slate-300'}`} />
                  {validationErrors.endTime && <p className="text-xs font-semibold text-red-500 mt-2">{validationErrors.endTime}</p>}
                </div>

                {validationErrors.timeRange && (
                  <div className="col-span-full rounded-xl bg-red-50/50 p-4 border border-red-100">
                    <p className="text-sm font-bold text-red-600">{validationErrors.timeRange}</p>
                  </div>
                )}

                <div className="col-span-full">
                  <label className="block text-sm font-bold text-slate-700 mb-2">Purpose * <span className="text-slate-400 font-medium ml-1">(Min 3 chars)</span></label>
                  <textarea value={purpose} onChange={(e) => setPurpose(e.target.value)} rows="3" placeholder="Briefly describe what this booking is for..." className={`w-full px-4 py-3 bg-slate-50 border rounded-xl font-medium text-slate-800 transition-all focus:bg-white focus:outline-none focus:ring-4 focus:ring-slate-100 resize-none ${validationErrors.purpose ? 'border-red-300 bg-red-50 focus:ring-red-100' : 'border-transparent focus:border-slate-300'}`} />
                  {validationErrors.purpose && <p className="text-xs font-semibold text-red-500 mt-2">{validationErrors.purpose}</p>}
                </div>

                {canBookForOthers && (
                  <div className="col-span-full">
                    <AdminBookForUserSelector 
                      onUserSelect={setBookedForUserId} 
                      userRole={user?.roles?.[0]}
                    />
                  </div>
                )}
              </div>

              {/* Recurrence Section - Moved to bottom for better UX */}
              <div className="border-t-2 border-slate-200 pt-4">
                <label className="flex items-center">
                  <input type="checkbox" checked={useRecurrence} onChange={(e) => setUseRecurrence(e.target.checked)} className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-slate-300 rounded" />
                  <span className="ml-2 text-sm font-medium text-slate-700">This is a recurring booking</span>
                </label>

                {useRecurrence && (
                  <div className="mt-4 p-4 bg-blue-50 rounded-lg border border-blue-200">
                    <div className="mb-4">
                      <label className="block text-sm font-medium text-slate-700 mb-2">Recurrence Pattern</label>
                      <div className="grid grid-cols-2 gap-2">
                        <button type="button" onClick={() => { setRecurrencePreset('daily'); setRecurrenceCustom(false); setRecurrenceRule(`FREQ=DAILY;COUNT=${recurrenceCount}`); }} className={`px-3 py-2 rounded-md text-sm font-medium transition-colors ${recurrencePreset === 'daily' ? 'bg-blue-600 text-white' : 'bg-white border border-slate-300 text-slate-700 hover:bg-slate-50'}`}>Daily</button>
                        <button type="button" onClick={() => { setRecurrencePreset('weekly'); setRecurrenceCustom(false); setRecurrenceRule(`FREQ=WEEKLY;BYDAY=MO,WE,FR;COUNT=${recurrenceCount}`); }} className={`px-3 py-2 rounded-md text-sm font-medium transition-colors ${recurrencePreset === 'weekly' ? 'bg-blue-600 text-white' : 'bg-white border border-slate-300 text-slate-700 hover:bg-slate-50'}`}>Weekly (M/W/F)</button>
                        <button type="button" onClick={() => { setRecurrencePreset('biweekly'); setRecurrenceCustom(false); setRecurrenceRule(`FREQ=WEEKLY;INTERVAL=2;COUNT=${recurrenceCount}`); }} className={`px-3 py-2 rounded-md text-sm font-medium transition-colors ${recurrencePreset === 'biweekly' ? 'bg-blue-600 text-white' : 'bg-white border border-slate-300 text-slate-700 hover:bg-slate-50'}`}>Bi-Weekly</button>
                        <button type="button" onClick={() => { setRecurrencePreset('monthly'); setRecurrenceCustom(false); setRecurrenceRule(`FREQ=MONTHLY;COUNT=${recurrenceCount}`); }} className={`px-3 py-2 rounded-md text-sm font-medium transition-colors ${recurrencePreset === 'monthly' ? 'bg-blue-600 text-white' : 'bg-white border border-slate-300 text-slate-700 hover:bg-slate-50'}`}>Monthly</button>
                      </div>
                    </div>

                    {recurrencePreset && (
                      <div className="mb-4">
                        <label className="block text-sm font-medium text-slate-700 mb-1">Number of Occurrences</label>
                        <input type="number" min="1" max="52" value={recurrenceCount} onChange={(e) => { setRecurrenceCount(e.target.value); if (recurrencePreset === 'daily') { setRecurrenceRule(`FREQ=DAILY;COUNT=${e.target.value}`); } else if (recurrencePreset === 'weekly') { setRecurrenceRule(`FREQ=WEEKLY;BYDAY=MO,WE,FR;COUNT=${e.target.value}`); } else if (recurrencePreset === 'biweekly') { setRecurrenceRule(`FREQ=WEEKLY;INTERVAL=2;COUNT=${e.target.value}`); } else if (recurrencePreset === 'monthly') { setRecurrenceRule(`FREQ=MONTHLY;COUNT=${e.target.value}`); } }} className="w-full px-3 py-2 border border-slate-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500" />
                      </div>
                    )}

                    <div className="mb-3">
                      <label className="flex items-center text-sm">
                        <input type="checkbox" checked={recurrenceCustom} onChange={(e) => setRecurrenceCustom(e.target.checked)} className="h-4 w-4 text-orange-600 focus:ring-orange-500 border-slate-300 rounded" />
                        <span className="ml-2 text-slate-700 font-medium">Advanced: Custom iCal RRULE</span>
                      </label>
                    </div>

                    {recurrenceCustom && (
                      <div>
                        <input type="text" value={recurrenceRule} onChange={(e) => setRecurrenceRule(e.target.value)} placeholder="e.g., FREQ=WEEKLY;BYDAY=MO,WE,FR;COUNT=12" className="w-full px-3 py-2 border border-slate-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500 font-mono text-sm" />
                        <p className="text-xs text-slate-500 mt-1">See <a href="https://datatracker.ietf.org/doc/html/rfc5545" target="_blank" rel="noreferrer" className="text-blue-600 hover:underline">RFC 5545</a> for RRULE format</p>
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

              <div className="flex gap-4 pt-6 border-t border-slate-100">
                <button type="submit" disabled={loading} className="w-2/3 py-3.5 bg-[#49BBBB] text-white font-bold rounded-xl shadow-[0_4px_14px_rgba(73,187,187,0.3)] hover:bg-[#3CA0A0] hover:shadow-[0_6px_20px_rgba(73,187,187,0.4)] hover:-translate-y-0.5 active:translate-y-0 disabled:opacity-50 disabled:cursor-not-allowed disabled:transform-none disabled:shadow-none transition-all duration-200">{loading ? 'Submitting...' : 'Confirm Book'}</button>
                <button type="button" onClick={handleClear} className="w-1/3 py-3.5 bg-slate-50 text-slate-600 font-bold rounded-xl border border-slate-200 hover:bg-slate-100 hover:text-slate-900 transition-colors">Clear</button>
              </div>
            </form>
          </>
        )}
      </div>
    </div>
  );
}
