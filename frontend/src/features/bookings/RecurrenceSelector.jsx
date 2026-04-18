import React, { useState, useEffect } from "react";

/**
 * RecurrenceSelector Component
 * Allows users to configure recurring bookings with weekly/monthly patterns.
 * Generates iCal RRULE format for backend storage.
 */
export default function RecurrenceSelector({ onRuleChange, maxOccurrences = 52 }) {
  const [recurrenceType, setRecurrenceType] = useState("NONE");
  const [repeatEvery, setRepeatEvery] = useState(1);
  const [occurrences, setOccurrences] = useState(4);
  const [weekDays, setWeekDays] = useState({
    MONDAY: false,
    TUESDAY: false,
    WEDNESDAY: false,
    THURSDAY: false,
    FRIDAY: false,
    SATURDAY: false,
    SUNDAY: false,
  });

  const dayAbbreviations = {
    MONDAY: "Mo",
    TUESDAY: "Tu",
    WEDNESDAY: "We",
    THURSDAY: "Th",
    FRIDAY: "Fr",
    SATURDAY: "Sa",
    SUNDAY: "Su",
  };

  const generateRRule = () => {
    if (recurrenceType === "NONE") {
      return null;
    }

    let rrule = `FREQ=${recurrenceType};INTERVAL=${repeatEvery};COUNT=${occurrences}`;

    if (recurrenceType === "WEEKLY") {
      const selectedDays = Object.keys(weekDays)
        .filter((day) => weekDays[day])
        .map((day) => dayAbbreviations[day]);

      if (selectedDays.length > 0) {
        rrule += `;BYDAY=${selectedDays.join(",")}`;
      }
    }

    return rrule;
  };

  // Update parent when recurrence changes
  useEffect(() => {
    const rule = generateRRule();
    if (onRuleChange) {
      onRuleChange(rule);
    }
  }, [recurrenceType, repeatEvery, occurrences, weekDays]);

  const handleWeekDayToggle = (day) => {
    setWeekDays((prev) => ({
      ...prev,
      [day]: !prev[day],
    }));
  };

  const handleSelectAllWeekdays = () => {
    const allTrue = Object.values(weekDays).every((v) => v);
    const newDays = {};
    Object.keys(weekDays).forEach((day) => {
      newDays[day] = !allTrue;
    });
    setWeekDays(newDays);
  };

  const selectedDaysCount = Object.values(weekDays).filter((v) => v).length;
  const rule = generateRRule();
  const previewCount = rule ? occurrences : 0;

  return (
    <div className="space-y-5 bg-[#f8fafc] p-5 rounded-2xl border border-[#e2e8f0]">
      <div>
        <label className="block text-xs font-bold text-[#64748b] uppercase tracking-wider mb-2">
          Recurrence Pattern
        </label>
        <div className="flex p-1 bg-[#f1f5f9] rounded-xl flex-wrap">
          <button
            type="button"
            onClick={() => setRecurrenceType("NONE")}
            className={`flex-1 min-w-[80px] px-3 py-2 rounded-lg font-semibold text-xs transition-all shadow-sm ${
              recurrenceType === "NONE"
                ? "bg-white text-[#0f172a]"
                : "text-[#64748b] shadow-none hover:text-[#0f172a]"
            }`}
          >
            None
          </button>
          <button
            type="button"
            onClick={() => setRecurrenceType("DAILY")}
            className={`flex-1 min-w-[80px] px-3 py-2 rounded-lg font-semibold text-xs transition-all shadow-sm ${
              recurrenceType === "DAILY"
                ? "bg-white text-[#0f172a]"
                : "text-[#64748b] shadow-none hover:text-[#0f172a]"
            }`}
          >
            Daily
          </button>
          <button
            type="button"
            onClick={() => setRecurrenceType("WEEKLY")}
            className={`flex-1 min-w-[80px] px-3 py-2 rounded-lg font-semibold text-xs transition-all shadow-sm ${
              recurrenceType === "WEEKLY"
                ? "bg-white text-[#0f172a]"
                : "text-[#64748b] shadow-none hover:text-[#0f172a]"
            }`}
          >
            Weekly
          </button>
          <button
            type="button"
            onClick={() => setRecurrenceType("MONTHLY")}
            className={`flex-1 min-w-[80px] px-3 py-2 rounded-lg font-semibold text-xs transition-all shadow-sm ${
              recurrenceType === "MONTHLY"
                ? "bg-white text-[#0f172a]"
                : "text-[#64748b] shadow-none hover:text-[#0f172a]"
            }`}
          >
            Monthly
          </button>
        </div>
      </div>

      {recurrenceType !== "NONE" && (
        <div className="space-y-5 pt-2">
          {/* Settings Grid */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
            <div className="bg-white p-4 rounded-xl border border-[#e2e8f0]">
              <label className="block text-xs font-bold text-[#64748b] uppercase tracking-wider mb-2">
                Repeat Every
              </label>
              <div className="flex gap-3 items-center">
                <input
                  type="number"
                  min="1"
                  max="52"
                  value={repeatEvery}
                  onChange={(e) => setRepeatEvery(Math.max(1, Number(e.target.value)))}
                  className="w-20 px-3 py-2 bg-[#f8fafc] border border-[#e2e8f0] rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-[#6366f1] font-medium transition-all"
                />
                <span className="text-[#475569] text-sm font-semibold">
                  {recurrenceType === "DAILY" && "day(s)"}
                  {recurrenceType === "WEEKLY" && "week(s)"}
                  {recurrenceType === "MONTHLY" && "month(s)"}
                </span>
              </div>
            </div>

            <div className="bg-white p-4 rounded-xl border border-[#e2e8f0]">
              <label className="block text-xs font-bold text-[#64748b] uppercase tracking-wider mb-2">
                Total Occurrences
              </label>
              <div className="flex gap-3 items-center">
                <input
                  type="number"
                  min="1"
                  max={maxOccurrences}
                  value={occurrences}
                  onChange={(e) =>
                    setOccurrences(Math.min(maxOccurrences, Math.max(1, Number(e.target.value))))
                  }
                  className="w-20 px-3 py-2 bg-[#f8fafc] border border-[#e2e8f0] rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-[#6366f1] font-medium transition-all"
                />
                <span className="text-[#94a3b8] text-xs font-semibold">Max: {maxOccurrences}</span>
              </div>
            </div>
          </div>

          {/* Weekly Day Selection */}
          {recurrenceType === "WEEKLY" && (
            <div className="bg-white p-4 rounded-xl border border-[#e2e8f0]">
              <div className="flex items-center justify-between mb-3">
                <label className="block text-xs font-bold text-[#64748b] uppercase tracking-wider">
                  Days of Week
                </label>
                <button
                  type="button"
                  onClick={handleSelectAllWeekdays}
                  className="text-xs text-[#6366f1] hover:text-[#4f46e5] font-semibold transition-colors"
                >
                  {selectedDaysCount === 7 ? "Deselect All" : "Select All Weekdays"}
                </button>
              </div>
              <div className="grid grid-cols-4 md:grid-cols-7 gap-2">
                {Object.keys(weekDays).map((day) => (
                  <button
                    key={day}
                    type="button"
                    onClick={() => handleWeekDayToggle(day)}
                    className={`p-2 rounded-lg font-semibold text-xs transition-colors border ${
                      weekDays[day]
                        ? "bg-[#0f172a] text-white border-[#0f172a]"
                        : "bg-[#f8fafc] text-[#475569] border-[#e2e8f0] hover:bg-[#f1f5f9]"
                    }`}
                  >
                    {day.substring(0, 3)}
                  </button>
                ))}
              </div>
              {selectedDaysCount === 0 && (
                <p className="text-[10px] text-[#ef4444] font-bold uppercase tracking-wider mt-2">Please select at least one day</p>
              )}
            </div>
          )}

          {/* Preview */}
          <div className="bg-[#eff6ff] border border-[#dbeafe] rounded-xl p-4 flex items-start gap-3">
            <svg className="w-5 h-5 text-[#3b82f6] shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
            <div>
              <p className="text-[10px] font-bold text-[#1e40af] uppercase tracking-wider mb-0.5">Recurrence Summary</p>
              <p className="text-sm text-[#1e3a8a] font-medium">
                This rule will create <span className="font-bold">{previewCount}</span> {previewCount === 1 ? "booking" : "bookings"}.
              </p>
              <p className="text-xs text-[#3b82f6] font-mono mt-1 opacity-80 break-all">{rule || "Incomplete rule"}</p>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
