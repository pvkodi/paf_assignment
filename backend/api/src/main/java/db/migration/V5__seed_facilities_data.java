package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.UUID;

public class V5__seed_facilities_data extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        InputStream is = getClass().getResourceAsStream("/db/migration/seed_facilities.csv");
        if (is == null) {
            // No CSV present on classpath - skip
            return;
        }

        Connection conn = context.getConnection();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            // Skip header
            String header = reader.readLine();
            if (header == null) return;

            String sql = "INSERT INTO public.facility (" +
                    "id, facility_code, name, type, capacity, location, building, floor, " +
                    "status, availability_start, availability_end, created_at, updated_at, facility_type, " +
                    "brand, model, serial_number, maintenance_schedule, lab_type, sports_type, " +
                    "stage_type, sound_system, av_enabled, catering_allowed, wheelchair_accessible" +
                    ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                    "ON CONFLICT (id) DO NOTHING";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                String line;
                int batch = 0;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().isEmpty()) continue;
                    String[] parts = line.split(",", -1);

                    String id = parts.length > 0 ? parts[0].trim() : null;
                    String facilityCode = parts.length > 1 ? parts[1].trim() : null;
                    String name = parts.length > 2 ? parts[2].trim() : null;
                    String type = parts.length > 3 ? parts[3].trim() : null;
                    String facilityType = parts.length > 4 ? parts[4].trim() : null;
                    String status = parts.length > 5 ? parts[5].trim() : null;
                    String capacityStr = parts.length > 6 ? parts[6].trim() : "";
                    int capacity = 1;
                    try { capacity = Integer.parseInt(capacityStr); } catch (Exception ignored) { capacity = 1; }

                    String availabilityStart = parts.length > 7 ? parts[7].trim() : null;
                    String availabilityEnd = parts.length > 8 ? parts[8].trim() : null;
                    String brand = parts.length > 9 ? parts[9].trim() : null;
                    if (brand == null || brand.isEmpty()) brand = "N/A";
                    String model = parts.length > 10 ? nullOrEmpty(parts[10]) : null;
                    String serial = parts.length > 11 ? nullOrEmpty(parts[11]) : null;
                    String avStr = parts.length > 12 ? parts[12].trim() : null;
                    String cateringStr = parts.length > 13 ? parts[13].trim() : null;
                    String wheelStr = parts.length > 14 ? parts[14].trim() : null;

                    Boolean av = parseBoolean(avStr);
                    Boolean catering = parseBoolean(cateringStr);
                    Boolean wheel = parseBoolean(wheelStr);

                    // Bind parameters (matching the INSERT above)
                    ps.setObject(1, UUID.fromString(id));
                    ps.setString(2, facilityCode);
                    ps.setString(3, name);
                    ps.setString(4, type);
                    ps.setInt(5, capacity);
                    ps.setNull(6, Types.VARCHAR); // location
                    ps.setNull(7, Types.VARCHAR); // building
                    ps.setNull(8, Types.VARCHAR); // floor
                    ps.setString(9, status);
                    if (availabilityStart == null || availabilityStart.isEmpty()) {
                        ps.setNull(10, Types.TIME);
                    } else {
                        ps.setTime(10, java.sql.Time.valueOf(availabilityStart));
                    }
                    if (availabilityEnd == null || availabilityEnd.isEmpty()) {
                        ps.setNull(11, Types.TIME);
                    } else {
                        ps.setTime(11, java.sql.Time.valueOf(availabilityEnd));
                    }
                    ps.setString(12, facilityType);
                    ps.setString(13, brand);
                    if (model == null) ps.setNull(14, Types.VARCHAR); else ps.setString(14, model);
                    if (serial == null) ps.setNull(15, Types.VARCHAR); else ps.setString(15, serial);
                    ps.setNull(16, Types.VARCHAR); // maintenance_schedule
                    ps.setNull(17, Types.VARCHAR); // lab_type
                    ps.setNull(18, Types.VARCHAR); // sports_type
                    ps.setNull(19, Types.VARCHAR); // stage_type
                    ps.setNull(20, Types.VARCHAR); // sound_system
                    if (av == null) ps.setNull(21, Types.BOOLEAN); else ps.setBoolean(21, av);
                    if (catering == null) ps.setNull(22, Types.BOOLEAN); else ps.setBoolean(22, catering);
                    if (wheel == null) ps.setNull(23, Types.BOOLEAN); else ps.setBoolean(23, wheel);

                    ps.addBatch();
                    batch++;
                    if (batch % 500 == 0) ps.executeBatch();
                }
                if (batch % 500 != 0) ps.executeBatch();
            }
        }
    }

    private static String nullOrEmpty(String s) {
        if (s == null) return null;
        s = s.trim();
        return s.isEmpty() ? null : s;
    }

    private static Boolean parseBoolean(String s) {
        if (s == null) return null;
        s = s.trim().toLowerCase();
        if (s.equals("t") || s.equals("true") || s.equals("1")) return true;
        if (s.equals("f") || s.equals("false") || s.equals("0")) return false;
        return null;
    }
}
