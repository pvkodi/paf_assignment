import React, { useState, useContext, useEffect } from "react";
import { AuthContext } from "../../contexts/AuthContext";
import { apiClient } from "../../services/apiClient";
import RecurrenceSelector from "./RecurrenceSelector";
import AdminBookForUserSelector from "./AdminBookForUserSelector";

export default function BookingForm({ 
  facility: initialFacility, 
  onBookingComplete, 
  isModal, 
  onClose,
  prefill = null
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

  // Handle pre-filling from suggestions
  useEffect(() => {
    if (prefill) {
      if (prefill.date) setBookingDate(prefill.date);
      if (prefill.startTime) setStartTime(prefill.startTime);
      if (prefill.endTime) setEndTime(prefill.endTime);
    }
  }, [prefill]);

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
      
      if (Array.isArray(response.data) && response.data.length > 0) {
        setAvailabilityData(response.data[0]);
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

  const inputClass = "w-full px-4 py-2.5 bg-white border border-[#e2e8f0] rounded-xl shadow-sm text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500 transition-all text-[#0f172a] placeholder-[#94a3b8]";
  const errorInputClass = "w-full px-4 py-2.5 bg-[#fef2f2] border border-[#fca5a5] rounded-xl shadow-sm text-sm focus:outline-none focus:ring-2 focus:ring-red-500/20 focus:border-red-500 transition-all text-[#0f172a]";

  if (!facility && !success) {
    return (
      <div className={`bg-white rounded-xl shadow-sm border border-[#e2e8f0] p-8 text-center ${isModal ? 'h-full flex flex-col items-center justify-center' : ''}`}>
        <div className="w-16 h-16 bg-[#f8fafc] rounded-full flex items-center justify-center mx-auto mb-4 border border-[#e2e8f0]">
          <svg className="w-8 h-8 text-[#94a3b8]" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.5" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"></path></svg>
        </div>
        <h3 className="text-lg font-semibold text-[#0f172a] mb-2">No Facility Selected</h3>
        <p className="text-[#64748b] text-sm max-w-sm mx-auto">Please select a facility from the list or map to start the booking process.</p>
      </div>
    );
  }

  const rootClass = isModal
    ? 'bg-white h-full flex flex-col'
    : 'bg-white rounded-2xl shadow-sm p-6 sm:p-8 border border-[#e2e8f0] max-w-3xl mx-auto';

  return (
    <div className={rootClass}>
      <div className="mb-6">
        <h2 className="text-2xl font-bold mb-1 text-[#0f172a] tracking-tight">Create Booking</h2>
        <p className="text-[#64748b] text-sm">Fill in the details below to request a reservation.</p>
      </div>

      <div className="flex-1 overflow-auto">
        {success && (
          <div className="rounded-xl bg-[#e8f5e9] p-4 mb-6 border border-[#c8e6c9] flex gap-3 items-start">
            <svg className="w-5 h-5 text-[#2e7d32] shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
            <p className="text-sm font-medium text-[#1b5e20]">{success}</p>
          </div>
        )}

        {error && (
          <div className="rounded-xl bg-[#fef2f2] border border-[#fca5a5] p-4 mb-6 flex gap-3 items-start">
            <svg className="w-5 h-5 text-[#ef4444] shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
            <div>
              <p className="font-semibold text-[#991b1b] text-sm">Booking error</p>
              <p className="text-sm text-[#b91c1c] mt-0.5">{error}</p>
            </div>
          </div>
        )}

        {facility && (
          <>
            <div className="mb-8 p-5 bg-[#f8fafc] rounded-2xl border border-[#e2e8f0] flex flex-col sm:flex-row sm:items-center justify-between gap-4">
              <div className="flex gap-4 items-center min-w-0">
                <div className="w-12 h-12 rounded-xl bg-white border border-[#e2e8f0] flex items-center justify-center shrink-0 shadow-sm text-indigo-600">
                  <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4"></path></svg>
                </div>
                <div className="min-w-0">
                  <p className="text-xs font-semibold text-indigo-600 tracking-wider uppercase mb-0.5">Selected Facility</p>
                  <p className="text-base font-bold text-[#0f172a] truncate">{facility.name}</p>
                  <p className="text-sm text-[#64748b] truncate mt-0.5">{facility.location}, {facility.building} • Floor {facility.floor}</p>
                </div>
              </div>
              <div className="sm:text-right shrink-0 px-4 py-2 bg-white rounded-xl border border-[#e2e8f0] shadow-sm">
                <div className="text-lg font-bold text-[#0f172a]">{facility.capacity}</div>
                <div className="text-xs font-medium text-[#64748b]">Max Capacity</div>
              </div>
            </div>

            <form onSubmit={handleSubmit} className="space-y-5">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
                <div>
                  <label className="block text-sm font-semibold text-[#475569] mb-1.5">Booking Date <span className="text-red-500">*</span></label>
                  <input 
                    type="date" 
                    min={minDate}
                    value={bookingDate} 
                    onChange={(e) => setBookingDate(e.target.value)} 
                    className={validationErrors.bookingDate ? errorInputClass : inputClass} 
                  />
                  {validationErrors.bookingDate && <p className="text-xs font-medium text-red-500 mt-1.5">{validationErrors.bookingDate}</p>}
                </div>

                <div>
                  <label className="block text-sm font-semibold text-[#475569] mb-1.5">Attendees <span className="text-red-500">*</span> <span className="font-normal text-[#94a3b8]">(Max: {facility.capacity})</span></label>
                  <input 
                    type="number" 
                    min="1" 
                    max={facility.capacity} 
                    value={attendees} 
                    onChange={(e) => setAttendees(e.target.value)} 
                    placeholder="Enter number"
                    className={validationErrors.attendees ? errorInputClass : inputClass} 
                  />
                  {validationErrors.attendees && <p className="text-xs font-medium text-red-500 mt-1.5">{validationErrors.attendees}</p>}
                </div>

                {/* Availability Display Section */}
                {bookingDate && (
                  <div className="col-span-full">
                    {availabilityLoading && (
                      <div className="rounded-xl border border-indigo-100 bg-indigo-50/50 p-4 animate-pulse">
                        <div className="h-4 bg-indigo-200/50 rounded w-1/4 mb-2"></div>
                        <div className="h-3 bg-indigo-100 rounded w-1/2"></div>
                      </div>
                    )}

                    {availabilityError && (
                      <div className="rounded-xl border border-amber-200 bg-[#fffbeb] p-4 text-sm text-[#b45309]">
                        {availabilityError}
                      </div>
                    )}

                    {availabilityData && !availabilityLoading && (
                      <div className="rounded-xl border border-[#e2e8f0] bg-[#f8fafc] p-5">
                        <h4 className="text-sm font-bold text-[#0f172a] mb-4 flex items-center gap-2">
                          <svg className="w-4 h-4 text-indigo-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
                          Available Time Slots for {bookingDate}
                        </h4>
                        
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                          <div>
                            {availabilityData.freeSlots && availabilityData.freeSlots.length > 0 ? (
                              <div>
                                <p className="text-xs font-semibold text-[#64748b] uppercase tracking-wider mb-2.5">Free Slots</p>
                                <div className="space-y-2 max-h-48 overflow-y-auto pr-2 custom-scrollbar">
                                  {availabilityData.freeSlots.map((slot, idx) => (
                                    <button
                                      key={idx}
                                      type="button"
                                      onClick={() => {
                                        setStartTime(slot.startTime.substring(0, 5));
                                        setEndTime(slot.endTime.substring(0, 5));
                                      }}
                                      className="w-full text-left px-3 py-2.5 bg-white border border-[#e2e8f0] rounded-lg hover:border-indigo-500 hover:ring-1 hover:ring-indigo-500 transition-all text-sm font-medium text-[#0f172a] flex items-center justify-between group shadow-sm"
                                    >
                                      <span>{slot.startTime} - {slot.endTime}</span>
                                      <span className="text-indigo-600 opacity-0 group-hover:opacity-100 transition-opacity text-xs bg-indigo-50 px-2 py-0.5 rounded">Select</span>
                                    </button>
                                  ))}
                                </div>
                              </div>
                            ) : (
                              <div className="h-full flex items-center justify-center p-4 bg-white border border-[#e2e8f0] rounded-lg border-dashed">
                                <p className="text-sm text-[#94a3b8] font-medium">No free slots available.</p>
                              </div>
                            )}
                          </div>

                          <div>
                            {availabilityData.bookedSlots && availabilityData.bookedSlots.length > 0 ? (
                              <div>
                                <p className="text-xs font-semibold text-[#64748b] uppercase tracking-wider mb-2.5">Booked Times</p>
                                <div className="space-y-2 max-h-48 overflow-y-auto pr-2 custom-scrollbar">
                                  {availabilityData.bookedSlots.map((slot, idx) => (
                                    <div key={idx} className="px-3 py-2.5 bg-[#f1f5f9] border border-[#e2e8f0] rounded-lg text-sm text-[#475569] flex flex-col justify-center">
                                      <span className="font-medium">{slot.startTime} - {slot.endTime}</span>
                                      {slot.purpose && <span className="text-xs text-[#64748b] truncate mt-0.5">{slot.purpose}</span>}
                                    </div>
                                  ))}
                                </div>
                              </div>
                            ) : (
                              <div className="h-full flex items-center justify-center p-4 bg-white border border-[#e2e8f0] rounded-lg border-dashed">
                                <p className="text-sm text-[#94a3b8] font-medium">No bookings yet.</p>
                              </div>
                            )}
                          </div>
                        </div>
                      </div>
                    )}
                  </div>
                )}

                <div>
                  <label className="block text-sm font-semibold text-[#475569] mb-1.5">Start Time <span className="text-red-500">*</span></label>
                  <input type="time" value={startTime} onChange={(e) => setStartTime(e.target.value)} className={validationErrors.startTime ? errorInputClass : inputClass} />
                  {validationErrors.startTime && <p className="text-xs font-medium text-red-500 mt-1.5">{validationErrors.startTime}</p>}
                </div>

                <div>
                  <label className="block text-sm font-semibold text-[#475569] mb-1.5">End Time <span className="text-red-500">*</span></label>
                  <input type="time" value={endTime} onChange={(e) => setEndTime(e.target.value)} className={validationErrors.endTime ? errorInputClass : inputClass} />
                  {validationErrors.endTime && <p className="text-xs font-medium text-red-500 mt-1.5">{validationErrors.endTime}</p>}
                </div>

                {validationErrors.timeRange && (
                  <div className="col-span-full rounded-xl bg-[#fef2f2] p-3 border border-[#fca5a5]">
                    <p className="text-sm font-medium text-[#b91c1c]">{validationErrors.timeRange}</p>
                  </div>
                )}

                <div className="col-span-full">
                  <label className="block text-sm font-semibold text-[#475569] mb-1.5">Purpose of Booking <span className="text-red-500">*</span></label>
                  <textarea 
                    value={purpose} 
                    onChange={(e) => setPurpose(e.target.value)} 
                    rows="3" 
                    placeholder="Briefly describe what the facility will be used for..." 
                    className={validationErrors.purpose ? errorInputClass : inputClass} 
                  />
                  {validationErrors.purpose && <p className="text-xs font-medium text-red-500 mt-1.5">{validationErrors.purpose}</p>}
                </div>

                {canBookForOthers && (
                  <div className="col-span-full bg-[#f8fafc] p-5 rounded-xl border border-[#e2e8f0]">
                    <h4 className="text-sm font-bold text-[#0f172a] mb-3">Admin Action</h4>
                    <AdminBookForUserSelector 
                      onUserSelect={setBookedForUserId} 
                      userRole={user?.roles?.[0]}
                    />
                  </div>
                )}
              </div>

              {/* Recurrence Section */}
              <div className="border-t border-[#e2e8f0] pt-6 mt-2">
                <label className="flex items-center group cursor-pointer w-fit">
                  <div className={`w-5 h-5 rounded border flex items-center justify-center transition-colors ${useRecurrence ? 'bg-indigo-600 border-indigo-600' : 'bg-white border-gray-300 group-hover:border-indigo-400'}`}>
                    {useRecurrence && <svg className="w-3.5 h-3.5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M5 13l4 4L19 7"></path></svg>}
                  </div>
                  <input type="checkbox" className="hidden" checked={useRecurrence} onChange={(e) => setUseRecurrence(e.target.checked)} />
                  <span className="ml-2.5 text-sm font-semibold text-[#0f172a]">Set up recurring booking</span>
                </label>

                {useRecurrence && (
                  <div className="mt-4 p-5 bg-[#f8fafc] rounded-xl border border-[#e2e8f0] animate-in fade-in slide-in-from-top-2">
                    <div className="mb-5">
                      <label className="block text-sm font-semibold text-[#475569] mb-2.5">Recurrence Pattern</label>
                      <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
                        {['daily', 'weekly', 'biweekly', 'monthly'].map((preset) => (
                          <button 
                            key={preset}
                            type="button" 
                            onClick={() => { 
                              setRecurrencePreset(preset); 
                              setRecurrenceCustom(false); 
                              const rrule = preset === 'daily' ? `FREQ=DAILY;COUNT=${recurrenceCount}` 
                                        : preset === 'weekly' ? `FREQ=WEEKLY;BYDAY=MO,WE,FR;COUNT=${recurrenceCount}`
                                        : preset === 'biweekly' ? `FREQ=WEEKLY;INTERVAL=2;COUNT=${recurrenceCount}`
                                        : `FREQ=MONTHLY;COUNT=${recurrenceCount}`;
                              setRecurrenceRule(rrule); 
                            }} 
                            className={`px-3 py-2.5 rounded-lg text-sm font-medium transition-all ${
                              recurrencePreset === preset 
                                ? 'bg-[#0f172a] text-white shadow-sm' 
                                : 'bg-white border border-[#e2e8f0] text-[#475569] hover:border-[#cbd5e1] hover:bg-[#f1f5f9]'
                            }`}
                          >
                            {preset === 'daily' ? 'Daily' : preset === 'weekly' ? 'Weekly (M/W/F)' : preset === 'biweekly' ? 'Bi-Weekly' : 'Monthly'}
                          </button>
                        ))}
                      </div>
                    </div>

                    {recurrencePreset && (
                      <div className="mb-5">
                        <label className="block text-sm font-semibold text-[#475569] mb-1.5">Number of Occurrences</label>
                        <input 
                          type="number" 
                          min="1" max="52" 
                          value={recurrenceCount} 
                          onChange={(e) => { 
                            setRecurrenceCount(e.target.value); 
                            if (recurrencePreset === 'daily') setRecurrenceRule(`FREQ=DAILY;COUNT=${e.target.value}`);
                            else if (recurrencePreset === 'weekly') setRecurrenceRule(`FREQ=WEEKLY;BYDAY=MO,WE,FR;COUNT=${e.target.value}`);
                            else if (recurrencePreset === 'biweekly') setRecurrenceRule(`FREQ=WEEKLY;INTERVAL=2;COUNT=${e.target.value}`);
                            else if (recurrencePreset === 'monthly') setRecurrenceRule(`FREQ=MONTHLY;COUNT=${e.target.value}`);
                          }} 
                          className={inputClass} 
                        />
                      </div>
                    )}

                    <div className="mb-3 pt-3 border-t border-[#e2e8f0]/50">
                      <label className="flex items-center group cursor-pointer w-fit">
                        <div className={`w-4 h-4 rounded-sm border flex items-center justify-center transition-colors ${recurrenceCustom ? 'bg-orange-500 border-orange-500' : 'bg-white border-gray-300 group-hover:border-orange-400'}`}>
                          {recurrenceCustom && <svg className="w-3 h-3 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M5 13l4 4L19 7"></path></svg>}
                        </div>
                        <input type="checkbox" className="hidden" checked={recurrenceCustom} onChange={(e) => setRecurrenceCustom(e.target.checked)} />
                        <span className="ml-2 text-sm font-medium text-[#475569]">Advanced: Custom iCal RRULE</span>
                      </label>
                    </div>

                    {recurrenceCustom && (
                      <div className="animate-in fade-in">
                        <input 
                          type="text" 
                          value={recurrenceRule} 
                          onChange={(e) => setRecurrenceRule(e.target.value)} 
                          placeholder="e.g., FREQ=WEEKLY;BYDAY=MO,WE,FR;COUNT=12" 
                          className={`${inputClass} font-mono`} 
                        />
                      </div>
                    )}

                    {recurrenceRule && (
                      <div className="mt-4 p-3 bg-white rounded-lg border border-[#e2e8f0] text-xs text-[#64748b] flex items-center gap-2">
                        <svg className="w-4 h-4 text-indigo-500 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
                        <span className="font-mono text-[#0f172a]">{recurrenceRule}</span>
                      </div>
                    )}
                  </div>
                )}
              </div>

              <div className="flex gap-3 pt-6 border-t border-[#e2e8f0] mt-8">
                <button 
                  type="submit" 
                  disabled={loading} 
                  className="flex-1 px-5 py-2.5 bg-[#0f172a] text-white text-sm font-medium rounded-xl hover:bg-[#1e293b] disabled:opacity-50 disabled:cursor-not-allowed transition-all shadow-sm focus:outline-none focus:ring-2 focus:ring-slate-500 focus:ring-offset-2 flex justify-center items-center gap-2"
                >
                  {loading ? (
                    <><svg className="animate-spin h-4 w-4 text-white" viewBox="0 0 24 24"><circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" fill="none"></circle><path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path></svg> Submitting...</>
                  ) : 'Confirm Booking'}
                </button>
                <button 
                  type="button" 
                  onClick={handleClear} 
                  className="px-5 py-2.5 bg-white text-[#475569] text-sm font-medium rounded-xl border border-[#e2e8f0] hover:bg-[#f8fafc] hover:border-[#cbd5e1] transition-all focus:outline-none focus:ring-2 focus:ring-slate-200 focus:ring-offset-2"
                >
                  Clear Form
                </button>
              </div>
            </form>
          </>
        )}
      </div>
    </div>
  );
}
