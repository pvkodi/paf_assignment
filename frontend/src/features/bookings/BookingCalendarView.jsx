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
    return bookings.filter((b) => b.booking_date === dateStr);
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
    <div className="bg-white rounded-2xl shadow-sm p-6 border border-[#e2e8f0]">
      <div className="flex items-center justify-between mb-6 border-b border-[#e2e8f0] pb-4">
        <h2 className="text-xl font-bold text-[#0f172a]">{monthName}</h2>
        <div className="flex gap-2">
          <button
            onClick={handlePrevMonth}
            className="px-3 py-1.5 rounded-lg bg-[#f1f5f9] text-[#0f172a] font-semibold hover:bg-[#e2e8f0] transition-colors text-sm"
          >
            ← Prev
          </button>
          <button
            onClick={handleNextMonth}
            className="px-3 py-1.5 rounded-lg bg-[#f1f5f9] text-[#0f172a] font-semibold hover:bg-[#e2e8f0] transition-colors text-sm"
          >
            Next →
          </button>
        </div>
      </div>

      {loading && (
        <div className="flex justify-center py-12">
          <div className="w-8 h-8 rounded-full border-2 border-indigo-200 border-t-indigo-600 animate-spin"></div>
        </div>
      )}
      
      {error && <p className="text-center text-[#ef4444] font-medium text-sm py-4">{error}</p>}

      {!loading && !error && (
        <div className="grid grid-cols-7 gap-2">
          {/* Weekday headers */}
          {["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"].map((day) => (
            <div key={day} className="text-center font-bold text-[10px] uppercase tracking-wider text-[#94a3b8] py-2">
              {day}
            </div>
          ))}

          {/* Calendar days */}
          {days.map((day, idx) => {
            const dayBookings = day ? getBookingsForDate(day) : [];
            return (
              <div
                key={idx}
                className={`min-h-[100px] p-2.5 rounded-xl border transition-all ${
                  day
                    ? dayBookings.length > 0
                      ? "bg-[#fffbeb] border-[#fde68a] hover:shadow-sm"
                      : "bg-[#f8fafc] border-[#e2e8f0] hover:bg-white hover:border-[#cbd5e1]"
                    : "bg-transparent border-transparent"
                }`}
              >
                {day && (
                  <>
                    <p className={`text-sm font-bold mb-2 ${dayBookings.length > 0 ? "text-[#b45309]" : "text-[#475569]"}`}>
                      {day}
                    </p>
                    {dayBookings.length > 0 && (
                      <div className="space-y-1.5">
                        {dayBookings.slice(0, 2).map((booking) => (
                          <div key={booking.id} className="bg-white/60 border border-[#fde68a] text-[#92400e] px-1.5 py-1 rounded-md text-[10px] font-semibold truncate shadow-sm">
                            {formatTime(booking.startTime)} - {formatTime(booking.endTime)}
                          </div>
                        ))}
                        {dayBookings.length > 2 && (
                          <div className="text-[10px] font-bold text-[#b45309] px-1">
                            +{dayBookings.length - 2} more
                          </div>
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

      <div className="mt-6 pt-4 border-t border-[#e2e8f0] flex gap-6 text-xs font-semibold">
        <div className="flex items-center gap-2 text-[#475569]">
          <div className="w-3 h-3 rounded-full bg-[#f8fafc] border border-[#e2e8f0]" />
          <span>Available</span>
        </div>
        <div className="flex items-center gap-2 text-[#b45309]">
          <div className="w-3 h-3 rounded-full bg-[#fffbeb] border border-[#fde68a]" />
          <span>Booked</span>
        </div>
      </div>
    </div>
  );
}
