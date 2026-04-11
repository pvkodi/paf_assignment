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
    return "bg-emerald-100 text-emerald-700";
  }
  if (status === "REJECTED") {
    return "bg-red-100 text-red-700";
  }
  return "bg-amber-100 text-amber-700";
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
      <div className="flex items-center justify-center py-10">
        <div className="h-10 w-10 animate-spin rounded-full border-b-2 border-indigo-600" />
      </div>
    );
  }

  return (
    <section className="space-y-5">
      <header className="rounded-xl border border-slate-200 bg-white p-5">
        <h2 className="text-xl font-semibold text-slate-900">Appeals</h2>
        <p className="mt-1 text-sm text-slate-600">
          {isAdmin
            ? "Review submitted appeals from suspended users."
            : "Submit and track suspension appeals."}
        </p>
      </header>

      {error && (
        <div className="rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-700">
          {error}
        </div>
      )}

      {message && (
        <div className="rounded-lg border border-emerald-200 bg-emerald-50 p-3 text-sm text-emerald-700">
          {message}
        </div>
      )}

      {!isAdmin && (
        <form onSubmit={submitAppeal} className="rounded-xl border border-slate-200 bg-white p-5">
          <label htmlFor="appealReason" className="mb-2 block text-sm font-medium text-slate-700">
            Why should this suspension be reviewed?
          </label>
          <textarea
            id="appealReason"
            value={reason}
            onChange={(event) => setReason(event.target.value)}
            rows={4}
            maxLength={1000}
            placeholder="Provide details and supporting context"
            className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:border-indigo-500 focus:outline-none"
          />
          <div className="mt-4 flex items-center justify-between">
            <p className="text-xs text-slate-500">{reason.length}/1000</p>
            <button
              type="submit"
              disabled={submitting}
              className="rounded-lg bg-indigo-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-indigo-700 disabled:cursor-not-allowed disabled:opacity-50"
            >
              Submit Appeal
            </button>
          </div>
        </form>
      )}

      <section className="rounded-xl border border-slate-200 bg-white p-5">
        <h3 className="text-sm font-semibold uppercase tracking-wide text-slate-500">
          {isAdmin ? "Pending Review" : "My Appeals"}
        </h3>

        {appeals.length === 0 ? (
          <p className="mt-4 text-sm text-slate-600">No appeals found.</p>
        ) : (
          <div className="mt-4 space-y-3">
            {appeals.map((appeal) => {
              const id = readField(appeal, "id", "id");
              const status = readField(appeal, "status", "status") || "SUBMITTED";
              const createdAt = readField(appeal, "createdAt", "created_at");
              const reviewedAt = readField(appeal, "reviewedAt", "reviewed_at");
              const submittedReason = readField(appeal, "reason", "reason") || "-";
              const decision = readField(appeal, "decision", "decision");
              const reviewedBy = readField(appeal, "reviewedByUserEmail", "reviewed_by_user_email");

              return (
                <article key={id} className="rounded-lg border border-slate-200 p-4">
                  <div className="flex flex-wrap items-center justify-between gap-2">
                    <p className="text-xs text-slate-500">ID: {id}</p>
                    <span className={`rounded-full px-3 py-1 text-xs font-medium ${statusClasses(status)}`}>
                      {status}
                    </span>
                  </div>

                  <p className="mt-3 text-sm text-slate-700">{submittedReason}</p>

                  <div className="mt-3 grid gap-2 text-xs text-slate-500 sm:grid-cols-2">
                    <p>Submitted: {formatDateTime(createdAt)}</p>
                    <p>Reviewed: {formatDateTime(reviewedAt)}</p>
                    <p>Reviewer: {reviewedBy || "-"}</p>
                    <p>Decision: {decision || "-"}</p>
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
