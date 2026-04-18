import { apiClient } from "../../services/apiClient";

export function toLabel(val) {
  if (!val) return "—";
  return val.replace(/_/g, " ");
}

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

export async function hardDeleteFacility(id, force = false) {
  await apiClient.delete(`/v1/facilities/${id}/permanent`, { params: { force } });
}

export async function bulkActionFacilities(ids, action, force = false) {
  await apiClient.post("/v1/facilities/bulk-action", { ids, action, force });
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

export async function fetchFacilityTimetable(id, day) {
  const response = await apiClient.get(`/v1/facilities/${id}/timetable-availability`, {
    params: { day },
  });
  return response.data;
}

export async function uploadFacilityTimetable(file) {
  const formData = new FormData();
  formData.append("file", file);
  
  const response = await apiClient.post("/v1/facilities/timetable/upload", formData, {
    headers: {
      "Content-Type": "multipart/form-data",
    },
  });
  return response.data;
}

export async function batchCreateFacilities(requests) {
  const response = await apiClient.post("/v1/facilities/batch", requests);
  return response.data;
}

export async function fetchFacilityInsights(id) {
  const response = await apiClient.get(`/v1/facilities/${id}/insights`);
  return response.data;
}

export async function fetchFacilityHeatmap(id, startDate, endDate) {
  const response = await apiClient.get(`/v1/facilities/${id}/heatmap`, {
    params: { startDate, endDate },
  });
  return response.data;
}
