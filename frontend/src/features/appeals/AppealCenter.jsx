import React, { useEffect, useState } from "react";
import { apiClient } from "../../services/apiClient";
import { useAuth } from "../../contexts/AuthContext";

function readField(item, camelKey, snakeKey) {
  if (!item || typeof item !== "object") {
    return null;
  }

  if (item[camelKey] !== undefined && item[camelKey] !== null) {
    return item[camelKey];
  }

  if (item[snakeKey] !== undefined && item[snakeKey] !== null) {
    return item[snakeKey];
  }

  return null;
}

function formatDateTime(value) {
  if (!value) {
    return "-";
  }

  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) {
    return "-";
  }

  return parsed.toLocaleString();
}

function statusClasses(status) {
  if (status === "APPROVED") {
    return "bg-[#e8f5e9] text-[#1b5e20] border-[#c8e6c9]";
  }
  if (status === "REJECTED") {
    return "bg-[#fef2f2] text-[#991b1b] border-[#fca5a5]";
  }
  return "bg-[#fffbeb] text-[#b45309] border-[#fde68a]";
}

export function AppealCenter() {
  const { hasRole } = useAuth();
  const isAdmin = hasRole("ADMIN");

  const [appeals, setAppeals] = useState([]);
  const [reason, setReason] = useState("");
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [message, setMessage] = useState(null);
  const [error, setError] = useState(null);

  const fetchAppeals = async () => {
    try {
      setLoading(true);
      setError(null);

      const response = await apiClient.get("/v1/appeals");
      const payload = response?.data;
      setAppeals(Array.isArray(payload) ? payload : []);
    } catch (requestError) {
      console.error("Failed to load appeals", requestError);
      setError("Failed to load appeals. Please refresh and try again.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAppeals();
  }, []);

  const submitAppeal = async (event) => {
    event.preventDefault();

    const trimmedReason = reason.trim();
    if (trimmedReason.length < 5) {
      setError("Appeal reason must be at least 5 characters.");
      return;
    }

    try {
      setSubmitting(true);
      setError(null);
      setMessage(null);

      await apiClient.post("/v1/appeals", {
        reason: trimmedReason,
      });

      setReason("");
      setMessage("Appeal submitted successfully.");
      await fetchAppeals();
    } catch (requestError) {
      console.error("Failed to submit appeal", requestError);
      setError("Failed to submit appeal. If a pending appeal exists, wait for review.");
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="flex flex-col items-center justify-center py-12">
        <div className="w-10 h-10 rounded-full border-4 border-indigo-200 border-t-indigo-600 animate-spin mb-4" />
        <p className="text-sm font-medium text-[#64748b]">Loading appeals...</p>
      </div>
    );
  }

  return (
    <section className="space-y-6">
      <header className="rounded-2xl border border-[#e2e8f0] bg-white p-6 shadow-sm flex items-start justify-between">
        <div>
          <h2 className="text-2xl font-bold text-[#0f172a] tracking-tight">Appeals Center</h2>
          <p className="mt-1 text-sm font-medium text-[#64748b]">
            {isAdmin
              ? "Review and manage submitted appeals from suspended users."
              : "Submit a new appeal to lift a suspension and track your history."}
          </p>
        </div>
        <div className="w-12 h-12 bg-[#f8fafc] rounded-2xl border border-[#e2e8f0] flex items-center justify-center shadow-sm shrink-0">
          <svg className="w-6 h-6 text-[#6366f1]" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z"></path></svg>
        </div>
      </header>

      {error && (
        <div className="rounded-xl border border-[#fca5a5] bg-[#fef2f2] p-4 text-sm font-semibold text-[#991b1b] flex items-center gap-3 shadow-sm">
          <svg className="w-5 h-5 text-[#ef4444]" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
          {error}
        </div>
      )}

      {message && (
        <div className="rounded-xl border border-[#dcfce3] bg-[#f0fdf4] p-4 text-sm font-semibold text-[#166534] flex items-center gap-3 shadow-sm">
          <svg className="w-5 h-5 text-[#16a34a]" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
          {message}
        </div>
      )}

      {!isAdmin && (
        <form onSubmit={submitAppeal} className="rounded-2xl border border-[#e2e8f0] bg-white p-6 shadow-sm">
          <label htmlFor="appealReason" className="mb-3 block text-sm font-bold text-[#0f172a] uppercase tracking-wide">
            New Appeal Request
          </label>
          <p className="text-xs text-[#64748b] mb-4 font-medium leading-relaxed">
            Please provide a detailed explanation of why your suspension should be lifted. Include any relevant context or supporting information.
          </p>
          <textarea
            id="appealReason"
            value={reason}
            onChange={(event) => setReason(event.target.value)}
            rows={4}
            maxLength={1000}
            placeholder="Type your explanation here..."
            className="w-full rounded-xl border border-[#e2e8f0] bg-[#f8fafc] px-4 py-3 text-sm font-medium focus:bg-white focus:border-transparent focus:outline-none focus:ring-2 focus:ring-[#6366f1] transition-all resize-none shadow-sm"
          />
          <div className="mt-4 flex items-center justify-between">
            <p className="text-[10px] font-bold text-[#94a3b8] uppercase tracking-wider">{reason.length} / 1000 characters</p>
            <button
              type="submit"
              disabled={submitting}
              className="rounded-xl bg-[#0f172a] px-6 py-2.5 text-sm font-bold text-white transition-all hover:bg-[#1e293b] shadow-sm disabled:cursor-not-allowed disabled:opacity-50 flex items-center gap-2"
            >
              {submitting ? (
                <>
                  <div className="w-4 h-4 rounded-full border-2 border-white/30 border-t-white animate-spin" />
                  Submitting...
                </>
              ) : (
                <>
                  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8"></path></svg>
                  Submit Appeal
                </>
              )}
            </button>
          </div>
        </form>
      )}

      <section className="rounded-2xl border border-[#e2e8f0] bg-white p-6 shadow-sm">
        <h3 className="text-[10px] font-bold uppercase tracking-widest text-[#94a3b8] mb-5 border-b border-[#e2e8f0] pb-3">
          {isAdmin ? "Appeal Submissions" : "Appeal History"}
        </h3>

        {appeals.length === 0 ? (
          <div className="text-center py-8">
            <div className="w-12 h-12 bg-[#f8fafc] rounded-full flex items-center justify-center mx-auto mb-3">
              <svg className="w-6 h-6 text-[#94a3b8]" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"></path></svg>
            </div>
            <p className="text-sm font-bold text-[#0f172a]">No appeals found</p>
            <p className="text-xs font-medium text-[#64748b] mt-1">There are no appeals to display at this time.</p>
          </div>
        ) : (
          <div className="mt-4 space-y-4">
            {appeals.map((appeal) => {
              const id = readField(appeal, "id", "id");
              const status = readField(appeal, "status", "status") || "SUBMITTED";
              const createdAt = readField(appeal, "createdAt", "created_at");
              const reviewedAt = readField(appeal, "reviewedAt", "reviewed_at");
              const submittedReason = readField(appeal, "reason", "reason") || "-";
              const decision = readField(appeal, "decision", "decision");
              const reviewedBy = readField(appeal, "reviewedByUserEmail", "reviewed_by_user_email");

              return (
                <article key={id} className="rounded-xl border border-[#e2e8f0] bg-[#f8fafc] p-5 transition-all hover:shadow-sm">
                  <div className="flex flex-wrap items-center justify-between gap-2 border-b border-[#e2e8f0] pb-3 mb-3">
                    <p className="text-[10px] font-bold uppercase tracking-wider text-[#94a3b8] font-mono">ID: {id}</p>
                    <span className={`rounded-full px-2.5 py-1 text-[10px] font-bold uppercase tracking-wider border ${statusClasses(status)}`}>
                      {status}
                    </span>
                  </div>

                  <div className="mb-4">
                    <p className="text-[10px] font-bold text-[#94a3b8] uppercase tracking-wider mb-1.5">Submitted Reason</p>
                    <p className="text-sm font-medium text-[#0f172a] leading-relaxed bg-white border border-[#e2e8f0] p-3 rounded-lg">
                      {submittedReason}
                    </p>
                  </div>

                  <div className="grid gap-x-4 gap-y-3 sm:grid-cols-2 lg:grid-cols-4 bg-white p-3 rounded-lg border border-[#e2e8f0]">
                    <div>
                      <p className="text-[10px] font-bold text-[#94a3b8] uppercase tracking-wider mb-0.5">Submitted On</p>
                      <p className="text-xs font-semibold text-[#475569]">{formatDateTime(createdAt)}</p>
                    </div>
                    {status !== "SUBMITTED" && (
                      <>
                        <div>
                          <p className="text-[10px] font-bold text-[#94a3b8] uppercase tracking-wider mb-0.5">Reviewed On</p>
                          <p className="text-xs font-semibold text-[#475569]">{formatDateTime(reviewedAt)}</p>
                        </div>
                        <div>
                          <p className="text-[10px] font-bold text-[#94a3b8] uppercase tracking-wider mb-0.5">Reviewed By</p>
                          <p className="text-xs font-semibold text-[#475569] truncate">{reviewedBy || "-"}</p>
                        </div>
                        <div className="sm:col-span-2 lg:col-span-1">
                          <p className="text-[10px] font-bold text-[#94a3b8] uppercase tracking-wider mb-0.5">Decision Note</p>
                          <p className="text-xs font-semibold text-[#475569] italic truncate" title={decision || ""}>
                            {decision || "No note provided"}
                          </p>
                        </div>
                      </>
                    )}
                  </div>
                </article>
              );
            })}
          </div>
        )}
      </section>
    </section>
  );
}

export default AppealCenter;
