import React, { useState, useEffect } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { batchCreateFacilities } from "./api";

const Building = ({ className }) => (<svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1v2H9V7zm0 4h1v2H9v-2zm0 4h1v2H9v-2zm-4-8h1v2H5V7zm0 4h1v2H5v-2zm0 4h1v2H5v-2zm8-8h1v2h-1V7zm0 4h1v2h-1v-2zm0 4h1v2h-1v-2zm4-8h1v2h-1V7zm0 4h1v2h-1v-2zm0 4h1v2h-1v-2z" /></svg>);
const MapPin = ({ className }) => (<svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" /><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" /></svg>);
const Users = ({ className }) => (<svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" /></svg>);
const Tag = ({ className }) => (<svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A1.994 1.994 0 013 12V7a4 4 0 014-4z" /></svg>);
const Save = ({ className }) => (<svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 7H5a2 2 0 00-2 2v9a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-3m-1 4l-3 3m0 0l-3-3m3 3V4" /></svg>);
const ArrowLeft = ({ className }) => (<svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 19l-7-7m0 0l7-7m-7 7h18" /></svg>);
const Calendar = ({ className }) => (<svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" /></svg>);
const Clock = ({ className }) => (<svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>);
const CheckCircle = ({ className }) => (<svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>);
const AlertTriangle = ({ className }) => (<svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" /></svg>);

const TimetablePreviewPage = () => {
  const location = useLocation();
  const navigate = useNavigate();
  
  const result = location.state?.result;
  const [drafts, setDrafts] = useState({});
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!result) {
      navigate('/dashboard');
    }
  }, [result, navigate]);

  useEffect(() => {
    if (result && result.unmatchedRooms) {
      const initialDrafts = {};
      result.unmatchedRooms.forEach((r) => {
        initialDrafts[r.facilityCode] = {
          name: r.name || r.facilityCode,
          location: r.building || "Main Building",
          floor: r.floor || "Ground Floor",
          capacity: r.capacity || 60,
          type: r.type || "LECTURE_HALL",
        };
      });
      setDrafts(initialDrafts);
    }
  }, [result]);

  if (!result) return null;

  const handleDraftChange = (facilityCode, field, value) => {
    setDrafts((prev) => ({
      ...prev,
      [facilityCode]: { ...prev[facilityCode], [field]: value },
    }));
  };

  const handleBatchImport = async () => {
    try {
      setIsSubmitting(true);
      setError(null);
      const itemsToCreate = result.unmatchedRooms.map((r) => ({
        facilityCode: r.facilityCode,
        name: drafts[r.facilityCode]?.name || r.facilityCode,
        location: drafts[r.facilityCode].location,
        floor: drafts[r.facilityCode].floor,
        capacity: drafts[r.facilityCode].capacity,
        type: drafts[r.facilityCode].type,
        // Assume default availability
        availabilityStartTime: "08:00:00",
        availabilityEndTime: "18:00:00",
      }));

      await batchCreateFacilities(itemsToCreate);
      alert("Successfully imported " + itemsToCreate.length + " missing facilities!");
      navigate('/dashboard');
    } catch (err) {
      console.error(err);
      setError(err.message || "Failed to batch import facilities");
    } finally {
      setIsSubmitting(false);
    }
  };

  const renderSchedule = (roomCode) => {
    if (!result.roomSchedules || !result.roomSchedules[roomCode]) {
      return <div className="text-[#64748b] text-sm mt-2">No schedules extracted or past 18:00</div>;
    }

    const scheduleObj = result.roomSchedules[roomCode];
    const days = Object.keys(scheduleObj);

    return (
      <div className="mt-3 grid grid-cols-1 md:grid-cols-2 gap-3 max-w-full">
        {days.map(day => (
          <div key={day} className="border border-[#f1f5f9] rounded-2xl bg-[#f8fafc] p-3">
             <div className="flex items-center text-sm font-semibold text-[#334155] mb-2">
                <Calendar className="w-4 h-4 mr-2 text-indigo-500" />
                {day}
             </div>
             <div className="flex flex-wrap gap-2">
               {scheduleObj[day].map((time, i) => (
                 <span key={i} className="inline-flex items-center px-2 py-1 bg-white border border-[#e2e8f0] rounded text-xs font-medium text-[#475569] shadow-sm">
                   <Clock className="w-3 h-3 mr-1 text-[#94a3b8]" />
                   {time}
                 </span>
               ))}
             </div>
          </div>
        ))}
      </div>
    );
  };

  return (
    <div className="max-w-7xl mx-auto px-4 py-8">
      {/* Header element */}
      <div className="flex items-center justify-between space-y-2 mb-8">
        <div>
          <button 
            onClick={() => navigate('/dashboard')}
            className="flex items-center text-sm text-[#64748b] hover:text-[#0f172a] transition-colors mb-2"
          >
            <ArrowLeft className="w-4 h-4 mr-1" />
            Back to Dashboard
          </button>
          <h2 className="text-3xl font-bold tracking-tight text-[#0f172a]">Timetable Extractor</h2>
          <p className="text-[#64748b] mt-1">Review the rooms and schedules extracted from the uploaded HTML.</p>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
        <div className="bg-white border rounded-xl p-6 shadow-sm flex items-center shadow-indigo-100/50">
          <div className="p-4 bg-indigo-50 rounded-2xl text-indigo-600 mr-4">
            <CheckCircle className="w-6 h-6" />
          </div>
          <div>
            <p className="text-sm font-medium text-[#64748b]">Total Valid Extracted Slots</p>
            <p className="text-2xl font-bold tracking-tight text-[#0f172a]">{result.totalSlotsExtracted}</p>
          </div>
        </div>

        <div className="bg-white border rounded-xl p-6 shadow-sm flex items-center  shadow-green-100/50">
          <div className="p-4 bg-emerald-50 rounded-2xl text-emerald-600 mr-4">
             <Building className="w-6 h-6" />
          </div>
          <div>
            <p className="text-sm font-medium text-[#64748b]">Matched Recognized Facilities</p>
            <p className="text-2xl font-bold tracking-tight text-[#0f172a]">{result.matchedRooms?.length || 0}</p>
          </div>
        </div>

        <div className="bg-white border rounded-xl p-6 shadow-sm flex items-center shadow-amber-100/50">
          <div className="p-4 bg-amber-50 rounded-2xl text-amber-600 mr-4">
             <AlertTriangle className="w-6 h-6" />
          </div>
          <div>
            <p className="text-sm font-medium text-[#64748b]">Unregistered Facilities Identified</p>
            <p className="text-2xl font-bold tracking-tight text-amber-600">{result.unmatchedRooms?.length || 0}</p>
          </div>
        </div>
      </div>

      <div className="space-y-8">
         {/* Unmatched / Review Section */}
        {result.unmatchedRooms && result.unmatchedRooms.length > 0 && (
          <div className="border border-amber-200 bg-white shadow-sm rounded-xl overflow-hidden">
             <div className="bg-amber-50 px-6 py-4 border-b border-amber-100 flex items-center justify-between">
               <div>
                  <h3 className="font-semibold text-amber-800 flex items-center text-lg">
                     <AlertTriangle className="w-5 h-5 mr-2" />
                     {result.unmatchedRooms.length} Facilities Need Registration
                  </h3>
                  <p className="text-sm text-amber-700 mt-1">Review the AI generated metadata for these unrecognized room codes.</p>
               </div>
               <button
                  onClick={handleBatchImport}
                  disabled={isSubmitting}
                  className="flex items-center px-4 py-2 bg-amber-600 hover:bg-amber-700 text-white rounded-2xl shadow font-medium transition-all"
                >
                  {isSubmitting ? (
                    <span className="animate-pulse">Importing...</span>
                  ) : (
                    <>
                      <Save className="w-4 h-4 mr-2" />
                      Save & Import All
                    </>
                  )}
                </button>
             </div>
             
             {error && (
                <div className="mx-6 mt-4 p-4 text-sm text-red-600 bg-red-50 border border-red-200 rounded-2xl">
                  {error}
                </div>
              )}

             <div className="p-6 divide-y divide-gray-100">
               {result.unmatchedRooms.map((room) => {
                  const draft = drafts[room.facilityCode] || {};
                  return (
                    <div key={room.facilityCode} className="py-6 first:pt-0 last:pb-0">
                      <div className="flex flex-col md:flex-row md:items-start space-y-4 md:space-y-0 md:space-x-8">
                        <div className="w-full md:w-1/4">
                          <h4 className="text-lg font-bold text-[#0f172a] flex items-center">
                            <span className="w-2 h-2 rounded-full bg-amber-500 mr-2"></span>
                            {room.facilityCode}
                          </h4>
                          <div className="mt-2 text-sm text-[#64748b] space-y-1">
                             <p>Suggested Details</p>
                             <p className="text-xs text-[#94a3b8]">Based on heuristics.</p>
                          </div>
                        </div>

                        <div className="w-full md:w-3/4">
                           {/* Grid Editor */}
                          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 p-4 bg-[#f8fafc] rounded-xl border border-[#f1f5f9]">
                             <div>
                               <label className="flex items-center text-xs font-semibold text-[#64748b] mb-1 tracking-wide uppercase">
                                 <Building className="w-3 h-3 mr-1" /> Building
                               </label>
                               <input
                                  type="text"
                                  className="w-full text-sm p-2 border border-[#e2e8f0] rounded-xl focus:ring-2 focus:ring-indigo-500 outline-none"
                                  value={draft.location || ""}
                                  onChange={(e) => handleDraftChange(room.facilityCode, "location", e.target.value)}
                                />
                             </div>
                             <div>
                                <label className="flex items-center text-xs font-semibold text-[#64748b] mb-1 tracking-wide uppercase">
                                  <MapPin className="w-3 h-3 mr-1" /> Floor
                                </label>
                                <input
                                  type="text"
                                  className="w-full text-sm p-2 border border-[#e2e8f0] rounded-xl focus:ring-2 focus:ring-indigo-500 outline-none"
                                  value={draft.floor || ""}
                                  onChange={(e) => handleDraftChange(room.facilityCode, "floor", e.target.value)}
                                />
                             </div>
                             <div>
                                <label className="flex items-center text-xs font-semibold text-[#64748b] mb-1 tracking-wide uppercase">
                                  <Users className="w-3 h-3 mr-1" /> Capacity
                                </label>
                                <input
                                  type="number"
                                  className="w-full text-sm p-2 border border-[#e2e8f0] rounded-xl focus:ring-2 focus:ring-indigo-500 outline-none"
                                  value={draft.capacity || ""}
                                  onChange={(e) => handleDraftChange(room.facilityCode, "capacity", parseInt(e.target.value) || 0)}
                                />
                             </div>
                             <div>
                                <label className="flex items-center text-xs font-semibold text-[#64748b] mb-1 tracking-wide uppercase">
                                  <Tag className="w-3 h-3 mr-1" /> Type
                                </label>
                                <select
                                  className="w-full text-sm p-2 border border-[#e2e8f0] rounded-xl focus:ring-2 focus:ring-indigo-500 outline-none"
                                  value={draft.type || ""}
                                  onChange={(e) => handleDraftChange(room.facilityCode, "type", e.target.value)}
                                >
                                  <option value="LECTURE_HALL">Lecture Hall</option>
                                  <option value="LAB">Laboratory</option>
                                  <option value="MEETING_ROOM">Meeting Room</option>
                                  <option value="AUDITORIUM">Auditorium</option>
                                </select>
                             </div>
                          </div>

                           {/* Extracted Schedule */}
                           <div className="mt-4">
                             <h5 className="text-sm font-semibold text-[#334155]">Extracted Sessions (Before 6 PM)</h5>
                             {renderSchedule(room.facilityCode)}
                           </div>
                        </div>
                      </div>
                    </div>
                  );
               })}
             </div>
          </div>
        )}

        {/* Existing / Recognized Rooms */}
        {result.matchedRooms && result.matchedRooms.length > 0 && (
          <div className="border border-[#e2e8f0] bg-white shadow-sm rounded-xl overflow-hidden mt-8">
             <div className="bg-[#f8fafc] px-6 py-4 border-b border-[#e2e8f0]">
               <h3 className="font-semibold text-[#1e293b] flex items-center text-lg">
                  <CheckCircle className="w-5 h-5 mr-2 text-indigo-600" />
                  {result.matchedRooms.length} Established Facilities Ready
               </h3>
               <p className="text-sm text-[#64748b] mt-1">These room schedules will be accurately validated in booking engine queries.</p>
             </div>
             
             <div className="p-6 grid grid-cols-1 xl:grid-cols-2 gap-6">
                {result.matchedRooms.map((rc) => (
                  <div key={rc} className="border border-[#f1f5f9] rounded-2xl p-4 bg-white shadow-sm hover:shadow transition-shadow">
                    <h4 className="text-md font-bold text-[#0f172a] mb-3 flex items-center">
                       <span className="w-2 h-2 rounded-full bg-emerald-500 mr-2"></span>
                       {rc}
                    </h4>
                    {renderSchedule(rc)}
                  </div>
                ))}
             </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default TimetablePreviewPage;
