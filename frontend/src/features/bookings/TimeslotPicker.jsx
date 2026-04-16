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
      
      console.log("📅 TimeslotPicker - Fetched bookings:", allBookings.length);
      console.log("📅 TimeslotPicker - Looking for facility:", facilityId, "date:", selectedDate);
      
      const dayBookings = allBookings.filter(b => 
        b.facility?.id === facilityId && 
        b.bookingDate === selectedDate &&
        (b.status === "APPROVED" || b.status === "PENDING")
      );
      
      console.log("📅 TimeslotPicker - Found conflicts for this date:", dayBookings.length);
      
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
      <div className="rounded-lg border border-slate-200 bg-slate-50 p-4">
        <p className="text-sm text-slate-600">Select a date to view available time slots</p>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="rounded-lg border border-slate-200 bg-slate-50 p-4 text-center">
        <p className="text-sm text-slate-600">Loading availability...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="rounded-lg border border-red-200 bg-red-50 p-4">
        <p className="text-sm text-red-700">{error}</p>
      </div>
    );
  }

  // Parse times for comparison - DEFINE FIRST BEFORE USING
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
      <div className="rounded-lg border border-blue-200 bg-blue-50 p-4">
        <p className="text-sm text-blue-700">✓ No conflicts - Time slot appears to be available</p>
        <p className="text-xs text-blue-600 mt-2">You can select any time between 08:00 and 20:00</p>
      </div>
    );
  }

  const bookedSlots = timeslotData?.bookedSlots || [];
  const freeSlots = timeslotData?.freeSlots || [];
  const availStart = timeslotData?.availabilityStart || "08:00:00";
  const availEnd = timeslotData?.availabilityEnd || "20:00:00";

  // If there are booked slots, show them prominently
  if (bookedSlots.length > 0) {
    return (
      <div className="rounded-lg border border-amber-200 bg-amber-50 p-4">
        <div className="text-sm font-semibold text-amber-900 mb-2">⏰ Booked times on this date:</div>
        <div className="space-y-1">
          {bookedSlots.map((slot, idx) => (
            <div key={idx} className="text-sm text-amber-800">
              • {formatTimeDisplay(slot.start)} - {formatTimeDisplay(slot.end)}
              {slot.purpose && <span className="text-xs text-amber-700 ml-2">({slot.purpose})</span>}
            </div>
          ))}
        </div>
        <p className="text-xs text-amber-700 mt-3">💡 Select a time outside these slots to avoid conflicts</p>
      </div>
    );
  }

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
    <div className="space-y-4 rounded-lg border border-slate-200 bg-white p-4">
      <div>
        <h3 className="text-sm font-semibold text-slate-900">Available Time Slots</h3>
        <p className="text-xs text-slate-600 mt-1">
          Facility available: {formatTimeDisplay(availStart)} - {formatTimeDisplay(availEnd)}
        </p>
      </div>

      {/* Booked Slots Display */}
      {bookedSlots.length > 0 && (
        <div className="rounded bg-yellow-50 border border-yellow-200 p-3">
          <h4 className="text-xs font-semibold text-yellow-900 mb-2">Booked Times</h4>
          <div className="space-y-1">
            {bookedSlots.map((slot, idx) => (
              <div key={idx} className="text-xs text-yellow-800">
                <span className="font-medium">
                  {formatTimeDisplay(slot.start)} - {formatTimeDisplay(slot.end)}
                </span>
                {slot.purpose && <span className="text-yellow-700"> • {slot.purpose}</span>}
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Free Slots Display */}
      {freeSlots.length > 0 && (
        <div className="rounded bg-green-50 border border-green-200 p-3">
          <h4 className="text-xs font-semibold text-green-900 mb-2">Available Times</h4>
          <div className="space-y-1">
            {freeSlots.map((slot, idx) => (
              <div key={idx} className="text-xs text-green-800">
                <span className="font-medium">
                  {formatTimeDisplay(slot.start)} - {formatTimeDisplay(slot.end)}
                </span>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Time Slot Selector */}
      <div className="space-y-3 border-t border-slate-200 pt-3">
        <h4 className="text-sm font-semibold text-slate-900">Select Your Time</h4>
        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className="block text-xs font-medium text-slate-700 mb-1">
              Start Time
            </label>
            <select
              value={selectedStartTime}
              onChange={(e) => setSelectedStartTime(e.target.value)}
              className="w-full rounded border border-slate-300 px-3 py-2 text-sm text-slate-900 focus:border-indigo-500 focus:outline-none"
            >
              <option value="">Select start time</option>
              {timeOptions.map((time) => (
                <option key={time} value={time}>
                  {formatTimeDisplay(time)}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-xs font-medium text-slate-700 mb-1">
              End Time
            </label>
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
              className="w-full rounded border border-slate-300 px-3 py-2 text-sm text-slate-900 focus:border-indigo-500 focus:outline-none"
            >
              <option value="">Select end time</option>
              {timeOptions.map((time) => (
                <option key={time} value={time}>
                  {formatTimeDisplay(time)}
                </option>
              ))}
            </select>
          </div>
        </div>

        {selectedStartTime && selectedEndTime && (
          <div
            className={`rounded p-3 text-sm ${
              isTimeSlotAvailable(selectedStartTime, selectedEndTime)
                ? "bg-green-50 border border-green-200"
                : "bg-red-50 border border-red-200"
            }`}
          >
            <p
              className={
                isTimeSlotAvailable(selectedStartTime, selectedEndTime)
                  ? "text-green-800 font-medium"
                  : "text-red-800 font-medium"
              }
            >
              {isTimeSlotAvailable(selectedStartTime, selectedEndTime)
                ? "✓ This time slot is available"
                : "✗ This time slot has a conflict. Please choose another time."}
            </p>
          </div>
        )}
      </div>
    </div>
  );
}
