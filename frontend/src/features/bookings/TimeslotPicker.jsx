import React, { useState, useEffect } from "react";
import { apiClient } from "../../services/apiClient";

/**
 * TimeslotPicker Component
 * Displays available and booked time slots for a facility on a selected date.
 * Helps users visualize conflicts before attempting to book.
 */
export default function TimeslotPicker({ facilityId, selectedDate, onTimeSelect }) {
  const [timeslotData, setTimeslotData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [selectedStartTime, setSelectedStartTime] = useState("");
  const [selectedEndTime, setSelectedEndTime] = useState("");

  useEffect(() => {
    if (facilityId && selectedDate) {
      fetchAvailableTimeslots();
    }
  }, [facilityId, selectedDate]);

  const fetchAvailableTimeslots = async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await apiClient.get("/v1/bookings");
      const allBookings = Array.isArray(response.data) ? response.data : [];
      
      const dayBookings = allBookings.filter(b => 
        b.facility?.id === facilityId && 
        b.bookingDate === selectedDate &&
        (b.status === "APPROVED" || b.status === "PENDING")
      );
      
      const bookedSlots = dayBookings.map(b => ({
        start: b.startTime,
        end: b.endTime,
        purpose: b.purpose
      }));
      
      setTimeslotData({
        bookedSlots,
        freeSlots: [],
        availabilityStart: "08:00:00",
        availabilityEnd: "20:00:00"
      });
    } catch (err) {
      console.error("Failed to fetch timeslots:", err);
      setError("Unable to load availability information");
      setTimeslotData(null);
    } finally {
      setLoading(false);
    }
  };

  const handleTimeSelect = (startTime, endTime) => {
    setSelectedStartTime(startTime);
    setSelectedEndTime(endTime);
    if (onTimeSelect) {
      onTimeSelect(startTime, endTime);
    }
  };

  if (!selectedDate) {
    return (
      <div className="rounded-xl border border-dashed border-[#cbd5e1] bg-[#f8fafc] p-6 text-center">
        <p className="text-sm font-medium text-[#64748b]">Select a date to view available time slots</p>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="rounded-xl border border-[#e2e8f0] bg-[#f8fafc] p-6 flex flex-col items-center justify-center">
        <div className="w-6 h-6 rounded-full border-2 border-indigo-200 border-t-indigo-600 animate-spin mb-2"></div>
        <p className="text-sm font-medium text-[#64748b]">Loading availability...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="rounded-xl border border-[#fca5a5] bg-[#fef2f2] p-4 flex items-center gap-3">
        <svg className="w-5 h-5 text-[#ef4444]" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
        <p className="text-sm font-semibold text-[#991b1b]">{error}</p>
      </div>
    );
  }

  // Parse times for comparison
  const parseTime = (timeStr) => {
    if (!timeStr) return 0;
    try {
      const parts = timeStr.split(":");
      const h = parseInt(parts[0]);
      const m = parseInt(parts[1]);
      return h * 60 + m;
    } catch (e) {
      return 0;
    }
  };

  const formatTimeDisplay = (timeStr) => {
    if (!timeStr) return "N/A";
    try {
      const parts = timeStr.split(":");
      const hour = parseInt(parts[0]);
      const minute = parts[1];
      const period = hour >= 12 ? "PM" : "AM";
      const displayHour = hour > 12 ? hour - 12 : hour === 0 ? 12 : hour;
      return `${displayHour}:${minute} ${period}`;
    } catch (e) {
      return timeStr;
    }
  };

  if (!timeslotData || (!timeslotData.bookedSlots && !timeslotData.freeSlots)) {
    return (
      <div className="rounded-xl border border-[#dcfce3] bg-[#f0fdf4] p-4 flex items-start gap-3">
        <svg className="w-5 h-5 text-[#16a34a] shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
        <div>
          <p className="text-sm font-semibold text-[#166534]">No conflicts found</p>
          <p className="text-xs text-[#15803d] mt-1">You can select any time between 08:00 and 20:00.</p>
        </div>
      </div>
    );
  }

  const bookedSlots = timeslotData?.bookedSlots || [];
  const freeSlots = timeslotData?.freeSlots || [];
  const availStart = timeslotData?.availabilityStart || "08:00:00";
  const availEnd = timeslotData?.availabilityEnd || "20:00:00";

  // Generate time slots (hourly) for the picker
  const generateTimeOptions = () => {
    const start = parseTime(availStart);
    const end = parseTime(availEnd);
    const options = [];

    for (let time = start; time <= end; time += 60) {
      const hour = Math.floor(time / 60);
      const minute = time % 60;
      const timeStr = `${String(hour).padStart(2, "0")}:${String(minute).padStart(2, "0")}`;
      options.push(timeStr);
    }

    return options;
  };

  const timeOptions = generateTimeOptions();

  // Check if a time slot is available
  const isTimeSlotAvailable = (startTime, endTime) => {
    const startMins = parseTime(startTime);
    const endMins = parseTime(endTime);

    for (const booked of bookedSlots) {
      const bookedStart = parseTime(booked.start);
      const bookedEnd = parseTime(booked.end);
      if (startMins < bookedEnd && endMins > bookedStart) {
        return false; // Conflict found
      }
    }
    return true;
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h3 className="text-xs font-bold text-[#64748b] uppercase tracking-wider">Availability Overview</h3>
          <p className="text-sm text-[#0f172a] font-medium mt-1">
            {formatTimeDisplay(availStart)} - {formatTimeDisplay(availEnd)}
          </p>
        </div>
      </div>

      {/* Booked Slots Display */}
      {bookedSlots.length > 0 && (
        <div className="rounded-xl bg-[#fffbeb] border border-[#fde68a] p-4">
          <h4 className="text-[10px] font-bold text-[#b45309] uppercase tracking-wider mb-3 flex items-center gap-1.5">
            <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
            Booked Timeslots
          </h4>
          <div className="flex flex-wrap gap-2">
            {bookedSlots.map((slot, idx) => (
              <div key={idx} className="bg-white border border-[#fcd34d] px-3 py-1.5 rounded-lg text-xs font-semibold text-[#92400e] shadow-sm flex items-center gap-2">
                <span>{formatTimeDisplay(slot.start)} - {formatTimeDisplay(slot.end)}</span>
                {slot.purpose && <span className="text-[#b45309] border-l border-[#fde68a] pl-2">{slot.purpose}</span>}
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Time Slot Selector */}
      <div className="bg-[#f8fafc] border border-[#e2e8f0] rounded-xl p-5">
        <h4 className="text-[10px] font-bold text-[#64748b] uppercase tracking-wider mb-4">Select Your Time</h4>
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div>
            <label className="block text-xs font-semibold text-[#475569] mb-1.5">
              Start Time
            </label>
            <div className="relative">
              <select
                value={selectedStartTime}
                onChange={(e) => setSelectedStartTime(e.target.value)}
                className="w-full rounded-xl bg-white border border-[#e2e8f0] pl-4 pr-10 py-3 text-sm font-medium text-[#0f172a] focus:ring-2 focus:ring-[#6366f1] focus:border-transparent outline-none appearance-none transition-all shadow-sm"
              >
                <option value="">Select start time</option>
                {timeOptions.map((time) => (
                  <option key={time} value={time}>
                    {formatTimeDisplay(time)}
                  </option>
                ))}
              </select>
              <div className="pointer-events-none absolute inset-y-0 right-0 flex items-center px-3 text-[#64748b]">
                <svg className="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 9l-7 7-7-7"></path></svg>
              </div>
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-[#475569] mb-1.5">
              End Time
            </label>
            <div className="relative">
              <select
                value={selectedEndTime}
                onChange={(e) => {
                  setSelectedEndTime(e.target.value);
                  if (selectedStartTime && e.target.value) {
                    if (isTimeSlotAvailable(selectedStartTime, e.target.value)) {
                      handleTimeSelect(selectedStartTime, e.target.value);
                    }
                  }
                }}
                className="w-full rounded-xl bg-white border border-[#e2e8f0] pl-4 pr-10 py-3 text-sm font-medium text-[#0f172a] focus:ring-2 focus:ring-[#6366f1] focus:border-transparent outline-none appearance-none transition-all shadow-sm"
              >
                <option value="">Select end time</option>
                {timeOptions.map((time) => (
                  <option key={time} value={time}>
                    {formatTimeDisplay(time)}
                  </option>
                ))}
              </select>
              <div className="pointer-events-none absolute inset-y-0 right-0 flex items-center px-3 text-[#64748b]">
                <svg className="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 9l-7 7-7-7"></path></svg>
              </div>
            </div>
          </div>
        </div>

        {selectedStartTime && selectedEndTime && (
          <div
            className={`mt-4 rounded-xl p-3.5 text-sm flex items-start gap-2.5 ${
              isTimeSlotAvailable(selectedStartTime, selectedEndTime)
                ? "bg-[#f0fdf4] border border-[#dcfce3]"
                : "bg-[#fef2f2] border border-[#fca5a5]"
            }`}
          >
            {isTimeSlotAvailable(selectedStartTime, selectedEndTime) ? (
              <svg className="w-5 h-5 text-[#16a34a] shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
            ) : (
              <svg className="w-5 h-5 text-[#ef4444] shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
            )}
            <p
              className={`font-semibold mt-0.5 ${
                isTimeSlotAvailable(selectedStartTime, selectedEndTime)
                  ? "text-[#166534]"
                  : "text-[#991b1b]"
              }`}
            >
              {isTimeSlotAvailable(selectedStartTime, selectedEndTime)
                ? "This time slot is available and ready to book."
                : "Conflict detected. This facility is already booked during this time."}
            </p>
          </div>
        )}
      </div>
    </div>
  );
}
