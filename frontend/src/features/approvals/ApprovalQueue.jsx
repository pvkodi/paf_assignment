import React, { useCallback, useEffect, useMemo, useState } from "react";
import { apiClient } from "../../services/apiClient";
import { useAuth } from "../../contexts/AuthContext";

function toDisplayDate(value) {
  if (!value) {
    return "-";
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "-";
  }

  return date.toLocaleString();
}

function getAppealField(appeal, camelKey, snakeKey) {
  if (!appeal || typeof appeal !== "object") {
    return null;
  }

  if (appeal[camelKey] !== undefined && appeal[camelKey] !== null) {
    return appeal[camelKey];
  }

  if (appeal[snakeKey] !== undefined && appeal[snakeKey] !== null) {
    return appeal[snakeKey];
  }

  return null;
}

export function ApprovalQueue() {
  const { hasRole } = useAuth();
  const isAdmin = hasRole("ADMIN");

  const [appeals, setAppeals] = useState([]);
  const [decisionNotes, setDecisionNotes] = useState({});
  const [loading, setLoading] = useState(true);
  const [submittingId, setSubmittingId] = useState(null);
  const [error, setError] = useState(null);

  const pendingAppeals = useMemo(
    () => appeals.filter((appeal) => getAppealField(appeal, "status", "status") === "SUBMITTED"),
    [appeals],
  );

  const fetchAppeals = useCallback(async () => {
    if (!isAdmin) {
      setLoading(false);
      return;
    }

    try {
      setLoading(true);
      setError(null);

      const response = await apiClient.get("/v1/appeals");
      const payload = response?.data;
      setAppeals(Array.isArray(payload) ? payload : []);
    } catch (requestError) {
      console.error("Failed to load approval queue", requestError);
      setError("Failed to load approval queue. Please retry.");
    } finally {
      setLoading(false);
    }
  }, [isAdmin]);

  useEffect(() => {
    fetchAppeals();
  }, [fetchAppeals]);

  const handleNoteChange = (appealId, value) => {
    setDecisionNotes((prev) => ({
      ...prev,
      [appealId]: value,
    }));
  };

  const handleDecision = async (appealId, approved) => {
    try {
      setSubmittingId(appealId);
      setError(null);

      const note = (decisionNotes[appealId] || "").trim();
      const actionPath = approved ? "approve" : "reject";

      await apiClient.post(`/v1/appeals/${appealId}/${actionPath}`, {
        approved,
        decision: note.length > 0 ? note : null,
      });

      await fetchAppeals();
    } catch (requestError) {
      console.error("Failed to process appeal decision", requestError);
      setError("Could not submit the decision. Check permissions and try again.");
    } finally {
      setSubmittingId(null);
    }
  };

  if (!isAdmin) {
    return (
      <section className="space-y-3 rounded-2xl border border-[#e2e8f0] bg-white p-6 shadow-sm">
        <h2 className="text-xl font-bold text-[#0f172a] tracking-tight">Approval Queue</h2>
        <p className="text-sm font-medium text-[#64748b]">
          This queue is available only to administrators.
        </p>
      </section>
    );
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center py-10">
        <div className="h-10 w-10 animate-spin rounded-full border-4 border-indigo-200 border-t-indigo-600" />
      </div>
    );
  }

  return (
    <section className="space-y-6">
      <header className="rounded-2xl border border-[#e2e8f0] bg-white p-6 shadow-sm flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-bold text-[#0f172a] tracking-tight">Approval Queue</h2>
          <p className="mt-1 text-sm font-medium text-[#64748b]">
            Pending appeals requiring review: <span className="font-bold text-[#0f172a]">{pendingAppeals.length}</span>
          </p>
        </div>
        <div className="w-12 h-12 bg-[#f8fafc] rounded-2xl border border-[#e2e8f0] flex items-center justify-center shadow-sm">
          <svg className="w-6 h-6 text-[#6366f1]" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
        </div>
      </header>

      {error && (
        <div className="rounded-xl border border-[#fca5a5] bg-[#fef2f2] p-4 text-sm font-semibold text-[#991b1b] flex items-center gap-3">
          <svg className="w-5 h-5 text-[#ef4444]" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
          {error}
        </div>
      )}

      {pendingAppeals.length === 0 ? (
        <div className="rounded-2xl border border-[#e2e8f0] bg-[#f8fafc] p-10 text-center shadow-sm">
          <div className="w-16 h-16 bg-white border border-[#e2e8f0] rounded-full flex items-center justify-center mx-auto mb-4">
            <svg className="w-8 h-8 text-[#94a3b8]" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M5 13l4 4L19 7"></path></svg>
          </div>
          <p className="text-lg font-bold text-[#0f172a]">No pending appeals</p>
          <p className="text-sm text-[#64748b] mt-1">The approval queue is currently empty.</p>
        </div>
      ) : (
        <div className="space-y-4">
          {pendingAppeals.map((appeal) => {
            const appealId = getAppealField(appeal, "id", "id");
            const userEmail = getAppealField(appeal, "userEmail", "user_email") || "Unknown user";
            const reason = getAppealField(appeal, "reason", "reason") || "No reason provided";
            const createdAt =
              getAppealField(appeal, "createdAt", "created_at") ||
              getAppealField(appeal, "updatedAt", "updated_at");

            return (
              <article key={appealId} className="rounded-2xl border border-[#e2e8f0] bg-white p-6 shadow-sm transition-all hover:shadow-md">
                <div className="flex flex-wrap items-start justify-between gap-4">
                  <div>
                    <h3 className="text-lg font-bold text-[#0f172a]">{userEmail}</h3>
                    <p className="mt-1 text-xs font-semibold text-[#64748b] uppercase tracking-wider flex items-center gap-1.5">
                      <svg className="w-3.5 h-3.5 text-[#94a3b8]" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
                      Submitted: {toDisplayDate(createdAt)}
                    </p>
                  </div>
                  <span className="rounded-full bg-[#fffbeb] px-3 py-1.5 text-[10px] font-bold text-[#b45309] uppercase tracking-wider border border-[#fde68a]">
                    Pending Review
                  </span>
                </div>

                <div className="mt-5">
                  <p className="text-[10px] font-bold text-[#94a3b8] uppercase tracking-wider mb-2">Appeal Reason</p>
                  <p className="rounded-xl bg-[#f8fafc] border border-[#e2e8f0] p-4 text-sm font-medium text-[#475569] leading-relaxed">
                    {reason}
                  </p>
                </div>

                <div className="mt-5">
                  <label className="text-[10px] font-bold text-[#94a3b8] uppercase tracking-wider mb-2 block">Decision Note (Optional)</label>
                  <textarea
                    value={decisionNotes[appealId] || ""}
                    onChange={(event) => handleNoteChange(appealId, event.target.value)}
                    placeholder="Enter notes regarding your decision..."
                    rows={3}
                    className="w-full rounded-xl border border-[#e2e8f0] bg-white px-4 py-3 text-sm font-medium focus:border-transparent focus:outline-none focus:ring-2 focus:ring-[#6366f1] transition-all resize-none shadow-sm placeholder:text-[#94a3b8]"
                  />
                </div>

                <div className="mt-5 flex gap-3">
                  <button
                    type="button"
                    disabled={submittingId === appealId}
                    onClick={() => handleDecision(appealId, true)}
                    className="flex-1 rounded-xl bg-[#10b981] px-4 py-2.5 text-sm font-bold text-white transition-all hover:bg-[#059669] shadow-sm disabled:cursor-not-allowed disabled:opacity-50 flex items-center justify-center gap-2"
                  >
                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M5 13l4 4L19 7"></path></svg>
                    Approve Appeal
                  </button>
                  <button
                    type="button"
                    disabled={submittingId === appealId}
                    onClick={() => handleDecision(appealId, false)}
                    className="flex-1 rounded-xl bg-white border border-[#fca5a5] px-4 py-2.5 text-sm font-bold text-[#ef4444] transition-all hover:bg-[#fef2f2] shadow-sm disabled:cursor-not-allowed disabled:opacity-50 flex items-center justify-center gap-2"
                  >
                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12"></path></svg>
                    Reject Appeal
                  </button>
                </div>
              </article>
            );
          })}
        </div>
      )}
    </section>
  );
}

export default ApprovalQueue;
