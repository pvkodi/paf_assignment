import React, { useState, useEffect } from "react";
import { apiClient } from "../../services/apiClient";

/**
 * BookingCalendarView Component
 * Displays a month calendar view of facility bookings.
 * Shows booked, pending, and available dates.
 */
export default function BookingCalendarView({ facilityId }) {
  const [currentDate, setCurrentDate] = useState(new Date());
  const [bookings, setBookings] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (facilityId) {
      fetchMonthBookings();
    }
  }, [facilityId, currentDate]);

  const fetchMonthBookings = async () => {
    try {
      setLoading(true);
      setError(null);
      
      const monthStart = new Date(currentDate.getFullYear(), currentDate.getMonth(), 1)
        .toISOString()
        .split("T")[0];
      const monthEnd = new Date(currentDate.getFullYear(), currentDate.getMonth() + 1, 0)
        .toISOString()
        .split("T")[0];

      const response = await apiClient.get(`/v1/bookings/admin/all`, {
        params: {
          facilityId,
          from: monthStart,
          to: monthEnd,
          status: "APPROVED",
        },
      });
      setBookings(Array.isArray(response.data) ? response.data : []);
    } catch (err) {
      console.error("Failed to fetch bookings:", err);
      setError("Unable to load calendar data");
    } finally {
      setLoading(false);
    }
  };

  const getDaysInMonth = (date) => {
    return new Date(date.getFullYear(), date.getMonth() + 1, 0).getDate();
  };

  const getFirstDayOfMonth = (date) => {
    return new Date(date.getFullYear(), date.getMonth(), 1).getDay();
  };

  const getBookingsForDate = (day) => {
    const dateStr = new Date(currentDate.getFullYear(), currentDate.getMonth(), day)
      .toISOString()
      .split("T")[0];
    return bookings.filter((b) => b.bookingDate === dateStr);
  };

  const formatTime = (timeStr) => {
    const [h, m] = timeStr.split(":");
    const hour = parseInt(h);
    const period = hour >= 12 ? "PM" : "AM";
    const displayHour = hour > 12 ? hour - 12 : hour === 0 ? 12 : hour;
    return `${displayHour}:${m}${period}`;
  };

  const monthName = currentDate.toLocaleString("default", {
    month: "long",
    year: "numeric",
  });
  const daysInMonth = getDaysInMonth(currentDate);
  const firstDay = getFirstDayOfMonth(currentDate);
  const days = [];

  // Add empty cells for days before month starts
  for (let i = 0; i < firstDay; i++) {
    days.push(null);
  }

  // Add days of month
  for (let day = 1; day <= daysInMonth; day++) {
    days.push(day);
  }

  const handlePrevMonth = () => {
    setCurrentDate(new Date(currentDate.getFullYear(), currentDate.getMonth() - 1));
  };

  const handleNextMonth = () => {
    setCurrentDate(new Date(currentDate.getFullYear(), currentDate.getMonth() + 1));
  };

  return (
    <div className="bg-white rounded-lg shadow-md p-6 border border-slate-100">
      <div className="flex items-center justify-between mb-4">
        <h2 className="text-xl font-semibold text-slate-900">{monthName}</h2>
        <div className="flex gap-2">
          <button
            onClick={handlePrevMonth}
            className="px-3 py-1 rounded bg-slate-200 text-slate-900 hover:bg-slate-300 transition-colors"
          >
            ← Prev
          </button>
          <button
            onClick={handleNextMonth}
            className="px-3 py-1 rounded bg-slate-200 text-slate-900 hover:bg-slate-300 transition-colors"
          >
            Next →
          </button>
        </div>
      </div>

      {loading && <p className="text-center text-slate-600">Loading calendar...</p>}
      {error && <p className="text-center text-red-600 text-sm">{error}</p>}

      {!loading && (
        <div className="grid grid-cols-7 gap-1">
          {/* Weekday headers */}
          {["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"].map((day) => (
            <div key={day} className="text-center font-semibold text-sm text-slate-700 py-2">
              {day}
            </div>
          ))}

          {/* Calendar days */}
          {days.map((day, idx) => {
            const dayBookings = day ? getBookingsForDate(day) : [];
            return (
              <div
                key={idx}
                className={`min-h-24 p-2 rounded border ${
                  day
                    ? dayBookings.length > 0
                      ? "bg-yellow-50 border-yellow-200"
                      : "bg-green-50 border-green-200"
                    : "bg-slate-50 border-slate-200"
                }`}
              >
                {day && (
                  <>
                    <p className="text-sm font-semibold text-slate-900">{day}</p>
                    {dayBookings.length > 0 && (
                      <div className="mt-1 text-xs space-y-0.5">
                        {dayBookings.slice(0, 2).map((booking) => (
                          <div key={booking.id} className="bg-yellow-100 text-yellow-800 px-1 py-0.5 rounded truncate">
                            {formatTime(booking.startTime)}
                          </div>
                        ))}
                        {dayBookings.length > 2 && (
                          <div className="text-slate-600">+{dayBookings.length - 2} more</div>
                        )}
                      </div>
                    )}
                  </>
                )}
              </div>
            );
          })}
        </div>
      )}

      <div className="mt-4 flex gap-4 text-xs">
        <div className="flex items-center gap-2">
          <div className="w-4 h-4 rounded bg-green-100 border border-green-200" />
          <span>Available</span>
        </div>
        <div className="flex items-center gap-2">
          <div className="w-4 h-4 rounded bg-yellow-100 border border-yellow-200" />
          <span>Booked</span>
        </div>
      </div>
    </div>
  );
}
