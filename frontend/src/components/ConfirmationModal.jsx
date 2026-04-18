import React, { useState, useEffect } from "react";

/**
 * ConfirmationModal
 * A secure, high-fidelity modal for sensitive actions.
 * Supports requirement to type a specific word (e.g. "DELETE") to confirm.
 */
export default function ConfirmationModal({ 
  isOpen, 
  onClose, 
  onConfirm, 
  title, 
  message, 
  confirmText = "Confirm", 
  confirmWord, // If provided, user must type this to enable confirm button
  variant = "danger", // 'danger' | 'warning' | 'info'
  isLoading = false
}) {
  const [inputValue, setInputValue] = useState("");
  
  // Reset input when modal opens/closes
  useEffect(() => {
    if (!isOpen) setInputValue("");
  }, [isOpen]);

  if (!isOpen) return null;

  const isConfirmEnabled = !confirmWord || inputValue === confirmWord;

  const themes = {
    danger: {
      bg: "bg-red-50",
      icon: "text-red-600",
      btn: "bg-red-600 hover:bg-red-700 shadow-red-100",
      input: "focus:ring-red-500",
      badge: "bg-red-100 text-red-700"
    },
    warning: {
      bg: "bg-amber-50",
      icon: "text-amber-600",
      btn: "bg-amber-600 hover:bg-amber-700 shadow-amber-100",
      input: "focus:ring-amber-500",
      badge: "bg-amber-100 text-amber-700"
    },
    info: {
      bg: "bg-indigo-50",
      icon: "text-indigo-600",
      btn: "bg-indigo-600 hover:bg-indigo-700 shadow-indigo-100",
      input: "focus:ring-indigo-500",
      badge: "bg-indigo-100 text-indigo-700"
    }
  };

  const theme = themes[variant] || themes.info;

  return (
    <div className="fixed inset-0 z-[100] flex items-center justify-center p-4 sm:p-6 overflow-y-auto">
      {/* Backdrop */}
      <div 
        className="fixed inset-0 bg-gray-900/60 backdrop-blur-sm transition-opacity animate-in fade-in duration-300" 
        onClick={onClose}
      />

      {/* Modal */}
      <div className="relative w-full max-w-md transform overflow-hidden rounded-2xl bg-white p-8 shadow-2xl transition-all animate-in zoom-in-95 duration-300">
        
        {/* Header Icon */}
        <div className={`mx-auto flex h-14 w-14 items-center justify-center rounded-full ${theme.bg} mb-6`}>
          {variant === "danger" && (
            <svg className={`h-8 w-8 ${theme.icon}`} fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.876c1.27 0 2.06-1.323 1.43-2.32L13.43 4.32a1.71 1.71 0 00-2.86 0L3.5 16.68a1.71 1.71 0 001.43 2.32z" />
            </svg>
          )}
          {variant === "warning" && (
            <svg className={`h-8 w-8 ${theme.icon}`} fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          )}
          {variant === "info" && (
            <svg className={`h-8 w-8 ${theme.icon}`} fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          )}
        </div>

        {/* Content */}
        <div className="text-center">
          <h3 className="text-xl font-black text-gray-900 leading-tight">
            {title}
          </h3>
          <div className="mt-3">
            <p className="text-sm text-gray-500 leading-relaxed px-2">
              {message}
            </p>
          </div>
        </div>

        {/* Type to confirm logic */}
        {confirmWord && (
          <div className="mt-8 space-y-3">
            <label className="text-[10px] font-black uppercase tracking-widest text-gray-400">
              Type <span className="text-gray-900 select-all font-black">{confirmWord}</span> to confirm
            </label>
            <input
              type="text"
              autoFocus
              value={inputValue}
              onChange={(e) => setInputValue(e.target.value)}
              placeholder={`Type ${confirmWord} here`}
              className={`block w-full rounded-xl border-gray-200 bg-gray-50 text-sm font-bold placeholder:text-gray-300 ${theme.input} focus:ring-2 focus:border-transparent transition-all`}
            />
          </div>
        )}

        {/* Action Buttons */}
        <div className="mt-10 flex flex-col gap-3">
          <button
            type="button"
            disabled={!isConfirmEnabled || isLoading}
            onClick={onConfirm}
            className={`flex w-full items-center justify-center rounded-xl px-4 py-3.5 text-sm font-black text-white shadow-lg transition-all active:scale-[0.98] disabled:opacity-30 disabled:grayscale disabled:cursor-not-allowed ${theme.btn}`}
          >
            {isLoading ? (
              <svg className="h-5 w-5 animate-spin text-white" fill="none" viewBox="0 0 24 24"><circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle><path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path></svg>
            ) : (
              confirmText
            )}
          </button>
          <button
            type="button"
            onClick={onClose}
            className="flex w-full items-center justify-center rounded-xl bg-white px-4 py-3.5 text-sm font-bold text-gray-600 hover:text-gray-900 border border-gray-200 hover:bg-gray-50 transition-all active:scale-[0.98]"
          >
            Cancel
          </button>
        </div>
      </div>
    </div>
  );
}
