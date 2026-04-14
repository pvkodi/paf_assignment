import apiClient from "../../services/apiClient";

export async function getRecommendations(payload) {
  const resp = await apiClient.post(`/v1/bookings/recommendations`, payload);
  return resp.data;
}
