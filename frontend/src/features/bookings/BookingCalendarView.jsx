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
    <div className="bg-white rounded-[2rem] shadow-[0_4px_24px_rgb(0,0,0,0.02)] p-6 md:p-8 border border-slate-100">
      <div className="flex items-center justify-between mb-8">
        <h2 className="text-2xl font-bold text-slate-900 tracking-tight">{monthName}</h2>
        <div className="flex gap-2">
          <button
            onClick={handlePrevMonth}
            className="p-2 rounded-full border border-slate-200 text-slate-600 hover:bg-slate-50 hover:text-slate-900 transition-colors shadow-sm focus:outline-none focus:ring-4 focus:ring-slate-100"
          >
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" /></svg>
          </button>
          <button
            onClick={handleNextMonth}
            className="p-2 rounded-full border border-slate-200 text-slate-600 hover:bg-slate-50 hover:text-slate-900 transition-colors shadow-sm focus:outline-none focus:ring-4 focus:ring-slate-100"
          >
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" /></svg>
          </button>
        </div>
      </div>

      {loading && (
        <div className="flex justify-center items-center h-48">
          <p className="text-slate-400 font-bold text-sm animate-pulse">Loading calendar view...</p>
        </div>
      )}
      {error && (
        <div className="flex justify-center items-center h-48">
          <p className="text-red-500 font-bold text-sm bg-red-50 px-4 py-2 rounded-xl">{error}</p>
        </div>
      )}

      {!loading && !error && (
        <div className="grid grid-cols-7 gap-2 md:gap-3">
          {/* Weekday headers */}
          {["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"].map((day) => (
            <div key={day} className="text-center font-bold text-[10px] text-slate-400 uppercase tracking-widest py-2">
              {day}
            </div>
          ))}

          {/* Calendar days */}
          {days.map((day, idx) => {
             const dayBookings = day ? getBookingsForDate(day) : [];
             const isBooked = dayBookings.length > 0;
             return (
              <div
                key={idx}
                className={`min-h-[6rem] p-3 rounded-2xl border transition-all ${
                  day
                    ? isBooked
                       ? "bg-[#49BBBB]/5 border-[#49BBBB]/10 hover:border-[#49BBBB]/30"
                       : "bg-green-50 border-green-100 hover:border-green-300"
                    : "bg-slate-50/50 border-transparent"
                }`}
              >
                {day && (
                  <div className="flex flex-col h-full">
                    <p className={`text-sm font-bold ${isBooked ? 'text-[#49BBBB]' : 'text-green-800'}`}>{day}</p>
                    {isBooked && (
                      <div className="mt-2 space-y-1">
                        {dayBookings.slice(0, 2).map((booking) => (
                           <div key={booking.id} className="bg-white/80 border border-[#49BBBB]/20 text-[#49BBBB] px-1.5 py-0.5 rounded-md text-[10px] font-bold truncate">
                             {formatTime(booking.startTime)}
                           </div>
                        ))}
                        {dayBookings.length > 2 && (
                          <div className="text-[10px] font-bold text-[#49BBBB]/70 uppercase">+{dayBookings.length - 2} more</div>
                        )}
                      </div>
                    )}
                  </div>
                )}
              </div>
             );
          })}
        </div>
      )}

      <div className="mt-8 flex gap-6 text-xs font-bold uppercase tracking-wider text-slate-500 justify-end">
        <div className="flex items-center gap-2">
          <div className="w-3 h-3 rounded-full bg-green-50 border border-green-200" />
          <span>Available</span>
        </div>
        <div className="flex items-center gap-2">
          <div className="w-3 h-3 rounded-full bg-[#49BBBB]/10 border border-[#49BBBB]/20" />
          <span className="text-[#49BBBB]">Booked</span>
        </div>
      </div>
    </div>
  );
}
