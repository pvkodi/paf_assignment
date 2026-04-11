package com.sliitreserve.api.entities.auth;

/**
 * Enumeration of suspension appeal statuses.
 *
 * <p><b>Status Lifecycle</b>:
 * <ul>
 *   <li><b>SUBMITTED</b>: Appeal just created by user; awaiting admin review
 *   <li><b>APPROVED</b>: Admin reviewed and approved; user's suspension is lifted
 *   <li><b>REJECTED</b>: Admin reviewed and rejected; user's suspension remains
 * </ul>
 *
 * @see SuspensionAppeal for entity using this status
 */
public enum SuspensionAppealStatus {
  SUBMITTED("Submitted"),
  APPROVED("Approved"),
  REJECTED("Rejected");

  private final String displayName;

  SuspensionAppealStatus(String displayName) {
    this.displayName = displayName;
  }

  public String getDisplayName() {
    return displayName;
  }
}
