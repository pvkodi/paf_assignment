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
      <section className="space-y-3 rounded-xl border border-slate-200 bg-white p-6">
        <h2 className="text-xl font-semibold text-slate-900">Approval Queue</h2>
        <p className="text-sm text-slate-600">
          This queue is available only to administrators.
        </p>
      </section>
    );
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center py-10">
        <div className="h-10 w-10 animate-spin rounded-full border-b-2 border-indigo-600" />
      </div>
    );
  }

  return (
    <section className="space-y-5">
      <header className="rounded-xl border border-slate-200 bg-white p-5">
        <h2 className="text-xl font-semibold text-slate-900">Approval Queue</h2>
        <p className="mt-1 text-sm text-slate-600">
          Pending appeals requiring review: {pendingAppeals.length}
        </p>
      </header>

      {error && (
        <div className="rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-700">
          {error}
        </div>
      )}

      {pendingAppeals.length === 0 ? (
        <div className="rounded-xl border border-slate-200 bg-white p-6 text-sm text-slate-600">
          No pending appeals in the queue.
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
              <article key={appealId} className="rounded-xl border border-slate-200 bg-white p-5">
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div>
                    <h3 className="text-sm font-semibold text-slate-900">{userEmail}</h3>
                    <p className="mt-1 text-xs text-slate-500">Submitted: {toDisplayDate(createdAt)}</p>
                  </div>
                  <span className="rounded-full bg-amber-100 px-3 py-1 text-xs font-medium text-amber-700">
                    Pending
                  </span>
                </div>

                <p className="mt-4 rounded-lg bg-slate-50 p-3 text-sm text-slate-700">{reason}</p>

                <textarea
                  value={decisionNotes[appealId] || ""}
                  onChange={(event) => handleNoteChange(appealId, event.target.value)}
                  placeholder="Optional decision note"
                  rows={3}
                  className="mt-4 w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:border-indigo-500 focus:outline-none"
                />

                <div className="mt-4 flex gap-3">
                  <button
                    type="button"
                    disabled={submittingId === appealId}
                    onClick={() => handleDecision(appealId, true)}
                    className="rounded-lg bg-emerald-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-emerald-700 disabled:cursor-not-allowed disabled:opacity-50"
                  >
                    Approve
                  </button>
                  <button
                    type="button"
                    disabled={submittingId === appealId}
                    onClick={() => handleDecision(appealId, false)}
                    className="rounded-lg bg-red-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-red-700 disabled:cursor-not-allowed disabled:opacity-50"
                  >
                    Reject
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
