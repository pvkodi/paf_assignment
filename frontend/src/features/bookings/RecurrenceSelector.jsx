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
    <div className="space-y-4">
      <div>
        <label className="block text-sm font-medium text-slate-700 mb-2">
          Recurrence Pattern
        </label>
        <div className="flex gap-2 flex-wrap">
          <button
            type="button"
            onClick={() => setRecurrenceType("NONE")}
            className={`px-3 py-2 rounded-md text-sm font-medium transition-colors ${
              recurrenceType === "NONE"
                ? "bg-blue-600 text-white"
                : "bg-slate-200 text-slate-700 hover:bg-slate-300"
            }`}
          >
            None (One-time)
          </button>
          <button
            type="button"
            onClick={() => setRecurrenceType("DAILY")}
            className={`px-3 py-2 rounded-md text-sm font-medium transition-colors ${
              recurrenceType === "DAILY"
                ? "bg-blue-600 text-white"
                : "bg-slate-200 text-slate-700 hover:bg-slate-300"
            }`}
          >
            Daily
          </button>
          <button
            type="button"
            onClick={() => setRecurrenceType("WEEKLY")}
            className={`px-3 py-2 rounded-md text-sm font-medium transition-colors ${
              recurrenceType === "WEEKLY"
                ? "bg-blue-600 text-white"
                : "bg-slate-200 text-slate-700 hover:bg-slate-300"
            }`}
          >
            Weekly
          </button>
          <button
            type="button"
            onClick={() => setRecurrenceType("MONTHLY")}
            className={`px-3 py-2 rounded-md text-sm font-medium transition-colors ${
              recurrenceType === "MONTHLY"
                ? "bg-blue-600 text-white"
                : "bg-slate-200 text-slate-700 hover:bg-slate-300"
            }`}
          >
            Monthly
          </button>
        </div>
      </div>

      {recurrenceType !== "NONE" && (
        <>
          {/* Repeat Every */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">
                Repeat Every
              </label>
              <div className="flex gap-2 items-center">
                <input
                  type="number"
                  min="1"
                  max="52"
                  value={repeatEvery}
                  onChange={(e) => setRepeatEvery(Math.max(1, Number(e.target.value)))}
                  className="w-20 px-3 py-2 border border-slate-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                />
                <span className="text-slate-700 font-medium">
                  {recurrenceType === "DAILY" && "day(s)"}
                  {recurrenceType === "WEEKLY" && "week(s)"}
                  {recurrenceType === "MONTHLY" && "month(s)"}
                </span>
              </div>
            </div>

            {/* Number of Occurrences */}
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">
                Number of Occurrences
              </label>
              <div className="flex gap-2 items-center">
                <input
                  type="number"
                  min="1"
                  max={maxOccurrences}
                  value={occurrences}
                  onChange={(e) =>
                    setOccurrences(Math.min(maxOccurrences, Math.max(1, Number(e.target.value))))
                  }
                  className="w-20 px-3 py-2 border border-slate-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                />
                <span className="text-slate-600 text-sm">Max: {maxOccurrences}</span>
              </div>
            </div>
          </div>

          {/* Weekly Day Selection */}
          {recurrenceType === "WEEKLY" && (
            <div>
              <div className="flex items-center justify-between mb-2">
                <label className="block text-sm font-medium text-slate-700">
                  Days of Week
                </label>
                <button
                  type="button"
                  onClick={handleSelectAllWeekdays}
                  className="text-xs text-blue-600 hover:text-blue-700 font-medium"
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
                    className={`p-2 rounded-md font-medium text-sm transition-colors ${
                      weekDays[day]
                        ? "bg-blue-600 text-white"
                        : "bg-slate-200 text-slate-700 hover:bg-slate-300"
                    }`}
                  >
                    {day.substring(0, 3)}
                  </button>
                ))}
              </div>
              {selectedDaysCount === 0 && (
                <p className="text-xs text-red-600 mt-1">Please select at least one day</p>
              )}
            </div>
          )}

          {/* Preview */}
          <div className="bg-blue-50 border border-blue-200 rounded-lg p-3">
            <p className="text-sm font-medium text-blue-900 mb-1">Preview</p>
            <p className="text-sm text-blue-800">
              Creates {previewCount} {previewCount === 1 ? "booking" : "bookings"}
            </p>
            <p className="text-xs text-blue-600 font-mono mt-1">Rule: {rule || "None"}</p>
          </div>
        </>
      )}
    </div>
  );
}
