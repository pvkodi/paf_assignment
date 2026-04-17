-- V7__add_geofencing_to_facility.sql
-- Adds WiFi and GPS geofencing columns to facility table for location-based check-in verification

-- Add WiFi geofencing columns
ALTER TABLE public.facility 
ADD COLUMN IF NOT EXISTS wifi_ssid VARCHAR(64);

ALTER TABLE public.facility 
ADD COLUMN IF NOT EXISTS wifi_mac_address VARCHAR(17);

-- Add GPS geofencing columns
ALTER TABLE public.facility 
ADD COLUMN IF NOT EXISTS facility_latitude DOUBLE PRECISION;

ALTER TABLE public.facility 
ADD COLUMN IF NOT EXISTS facility_longitude DOUBLE PRECISION;

-- Add geofence radius (in meters) - default 100m
ALTER TABLE public.facility 
ADD COLUMN IF NOT EXISTS geofence_radius_meters INTEGER DEFAULT 100;

-- Create index on wifi_ssid for faster lookups
CREATE INDEX IF NOT EXISTS idx_facility_wifi_ssid ON public.facility(wifi_ssid);

-- Create index on facility GPS coordinates for range queries
CREATE INDEX IF NOT EXISTS idx_facility_gps ON public.facility(facility_latitude, facility_longitude);

-- Insert test facility WiFi configuration
-- Facility ID: 0071200e-b8de-4f66-b6ba-1d41c5fabb7c
-- WiFi SSID: SLT-Fiber-5G_6df8
-- WiFi MAC (BSSID): b4:0f:3b:64:6d:f8
UPDATE public.facility 
SET wifi_ssid = 'SLT-Fiber-5G_6df8',
    wifi_mac_address = 'b4:0f:3b:64:6d:f8',
    geofence_radius_meters = 100
WHERE id = '0071200e-b8de-4f66-b6ba-1d41c5fabb7c';

-- Add comment for documentation
COMMENT ON COLUMN public.facility.wifi_ssid IS 'WiFi network SSID (name) for geofencing check-in verification';
COMMENT ON COLUMN public.facility.wifi_mac_address IS 'WiFi access point MAC address (BSSID) for optional stricter verification';
COMMENT ON COLUMN public.facility.facility_latitude IS 'GPS latitude coordinate for GPS-based geofencing (fallback if WiFi unavailable)';
COMMENT ON COLUMN public.facility.facility_longitude IS 'GPS longitude coordinate for GPS-based geofencing';
COMMENT ON COLUMN public.facility.geofence_radius_meters IS 'GPS verification radius in meters (default 100m)';
