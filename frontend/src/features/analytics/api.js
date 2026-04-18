import { apiClient } from "../../services/apiClient";

// Fetch campus-wide utilization analytics for a date range
export async function fetchCampusUtilization(from, to) {
  const response = await apiClient.get("/v1/analytics/utilization", {
    params: { from, to },
  });
  return response.data;
}

// Fetch live campus vacancy/occupancy status
export async function fetchRealTimeStatus() {
  const response = await apiClient.get("/v1/analytics/real-time-status");
  return response.data;
}

export default { fetchCampusUtilization, fetchRealTimeStatus };
