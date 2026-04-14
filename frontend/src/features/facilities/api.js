import { apiClient } from "../../services/apiClient";

export async function fetchFacilities(params = {}) {
  const response = await apiClient.get("/v1/facilities", { params });
  return response.data;
}

export async function searchFacilities(params = {}) {
  const response = await apiClient.get("/v1/facilities/search", { params });
  return response.data;
}

export async function fetchFacilityById(id) {
  const response = await apiClient.get(`/v1/facilities/${id}`);
  return response.data;
}

export async function createFacility(payload) {
  const response = await apiClient.post("/v1/facilities", payload);
  return response.data;
}

export async function updateFacility(id, payload) {
  const response = await apiClient.put(`/v1/facilities/${id}`, payload);
  return response.data;
}

export async function markFacilityOutOfService(id) {
  await apiClient.delete(`/v1/facilities/${id}`);
}

export async function fetchFacilityUtilization(id, start, end) {
  const response = await apiClient.get(`/v1/facilities/${id}/utilization`, {
    params: { start, end },
  });
  return response.data;
}

export async function fetchUnderutilizedFacilities(end) {
  const response = await apiClient.get("/v1/facilities/underutilized", {
    params: end ? { end } : {},
  });
  return response.data;
}

export async function fetchFacilitySuggestions(payload) {
  const response = await apiClient.post("/v1/facilities/suggestions", payload);
  return response.data;
}
