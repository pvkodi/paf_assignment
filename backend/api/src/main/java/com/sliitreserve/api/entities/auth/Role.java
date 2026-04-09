package com.sliitreserve.api.entities.auth;

/**
 * User role enumeration for RBAC (Role-Based Access Control).
 *
 * <p><b>Roles</b> (from FR-002):
 * <ul>
 *   <li><b>USER</b>: Regular campus user; can book facilities, limited by quota/peak-hour
 *       policies; requires lecturer + admin approval.
 *   <li><b>LECTURER</b>: Faculty/instructor; can book facilities with fewer approvals; can
 *       approve USER bookings.
 *   <li><b>TECHNICIAN</b>: Maintenance/support staff; can handle maintenance tickets; cannot
 *       book facilities.
 *   <li><b>FACILITY_MANAGER</b>: Facility operations coordinator; can approve high-capacity
 *       facility bookings; manage facility status.
 *   <li><b>ADMIN</b>: System administrator; full access; can approve bookings, manage users,
 *       escalate tickets, view analytics.
 * </ul>
 *
 * <p>Users can have multiple roles. Policy resolution treats multi-role users with the most
 * permissive policy (FR-042).
 *
 * @see User for the user entity
 */
public enum Role {
  USER("User"),
  LECTURER("Lecturer"),
  TECHNICIAN("Technician"),
  FACILITY_MANAGER("Facility Manager"),
  ADMIN("Administrator");

  private final String displayName;

  Role(String displayName) {
    this.displayName = displayName;
  }

  public String getDisplayName() {
    return displayName;
  }
}
