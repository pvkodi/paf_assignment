import React, { useState } from "react";
import { FacilitySearch } from "../features/facilities";
import { BookingForm } from "../features/bookings";

/**
 * BookingPage
 * Main page for facility search and booking form
 * Layout:
 * - Search filters and facility cards grid at the top
 * - Booking form below where user can fill details for selected facility
 * Users can click "Select & Book" on any facility card to select it in the form
 */
function BookingPage() {
  const [selectedFacility, setSelectedFacility] = useState(null);

  const handleFacilitySelected = (facility) => {
    setSelectedFacility(facility);
    // Scroll to form
    setTimeout(() => {
      document
        .getElementById("booking-form-section")
        ?.scrollIntoView({ behavior: "smooth" });
    }, 100);
  };

  const handleBookingCreated = (booking) => {
    // Show success and reset
    setSelectedFacility(null);
    // Could navigate to pending bookings or show confirmation
  };

  const handleCancel = () => {
    setSelectedFacility(null);
  };

  return (
    <div className="space-y-8">
      {/* Header */}
      <div>
        <h1 className="text-3xl font-bold text-gray-900">Book a Facility</h1>
        <p className="text-gray-600 mt-2">
          Search for available facilities below, select one from the cards, and
          fill out your booking details
        </p>
      </div>

      {/* Facility Search & Cards Section */}
      <div>
        <FacilitySearch onFacilitySelected={handleFacilitySelected} />
      </div>

      {/* Booking Form Section */}
      <div id="booking-form-section">
        {selectedFacility ? (
          <div className="bg-white rounded-lg shadow p-6 border-l-4 border-blue-600">
            <div className="mb-4">
              <h2 className="text-2xl font-bold text-gray-900">
                Complete Your Booking
              </h2>
              <p className="text-gray-600 mt-1">
                Selected:{" "}
                <span className="font-semibold">{selectedFacility.name}</span>{" "}
                (Capacity: {selectedFacility.capacity})
              </p>
            </div>
            <BookingForm
              facility={selectedFacility}
              onBookingCreated={handleBookingCreated}
              onCancel={handleCancel}
            />
          </div>
        ) : (
          <div className="bg-gray-50 rounded-lg border-2 border-dashed border-gray-300 p-12 text-center">
            <svg
              className="mx-auto h-12 w-12 text-gray-400 mb-4"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M12 6v6m0 0v6m0-6h6m0 0h6m0 0h6M6 12a6 6 0 11-12 0 6 6 0 0112 0z"
              />
            </svg>
            <h3 className="text-lg font-medium text-gray-900 mb-2">
              Select a Facility
            </h3>
            <p className="text-gray-600">
              Choose a facility from the search results above to begin your
              booking
            </p>
          </div>
        )}
      </div>
    </div>
  );
}

export default BookingPage;
