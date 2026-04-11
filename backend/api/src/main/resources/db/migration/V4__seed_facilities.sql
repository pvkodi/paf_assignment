-- Smart Campus Operations Hub - Seed Facilities
-- Initial data load with sample facilities of each type

INSERT INTO facility (
    id, facility_code, name, type, capacity, location, building, floor, status,
    availability_start, availability_end, disc_type, 
    av_equipment, wheelchair_accessible
) VALUES 
    (gen_random_uuid(), 'LH-001', 'Engineering Lecture Hall', 'LECTURE_HALL', 120, 'Main Campus', 'Building A', '2', 'ACTIVE', '08:00', '17:00', 'LectureHall', 'Projector, Whiteboard, Sound System', true),
    (gen_random_uuid(), 'LH-002', 'Science Amphitheatre', 'LECTURE_HALL', 200, 'Main Campus', 'Building B', '1', 'ACTIVE', '08:00', '18:00', 'LectureHall', 'Advanced Projector, Wireless Mic, Smart Board', true);

INSERT INTO facility (
    id, facility_code, name, type, capacity, location, building, floor, status,
    availability_start, availability_end, disc_type,
    lab_type, software_list, safety_equipment
) VALUES
    (gen_random_uuid(), 'LAB-001', 'Computer Science Lab', 'LAB', 40, 'Main Campus', 'Building C', '3', 'ACTIVE', '08:00', '17:00', 'Lab', 'Computer Lab', 'Visual Studio, Python, Java, VSCode', 'Fire Extinguisher, First Aid Kit, Emergency Shower'),
    (gen_random_uuid(), 'LAB-002', 'Chemistry Lab', 'LAB', 30, 'Main Campus', 'Building C', '4', 'ACTIVE', '08:00', '17:00', 'Lab', 'Chemistry Lab', 'ChemLab Software, MATLAB', 'Fume Hood, PPE Kit, Eyewash Station, Emergency Cart'),
    (gen_random_uuid(), 'LAB-003', 'Electronics Lab', 'LAB', 25, 'Main Campus', 'Building D', '2', 'ACTIVE', '08:00', '18:00', 'Lab', 'Electronics Lab', 'Multimeter Software, Oscilloscope Control', 'Circuit Breakers, First Aid Kit, Fire Extinguisher');

INSERT INTO facility (
    id, facility_code, name, type, capacity, location, building, floor, status,
    availability_start, availability_end, disc_type,
    av_enabled, catering_allowed
) VALUES
    (gen_random_uuid(), 'MR-001', 'Executive Conference Room', 'MEETING_ROOM', 20, 'Main Campus', 'Building A', '5', 'ACTIVE', '08:00', '20:00', 'MeetingRoom', true, true),
    (gen_random_uuid(), 'MR-002', 'Brainstorm Studio', 'MEETING_ROOM', 12, 'Main Campus', 'Building B', '3', 'ACTIVE', '08:00', '19:00', 'MeetingRoom', true, false),
    (gen_random_uuid(), 'MR-003', 'Board Room', 'MEETING_ROOM', 16, 'Main Campus', 'Building A', '6', 'ACTIVE', '09:00', '21:00', 'MeetingRoom', true, true);

INSERT INTO facility (
    id, facility_code, name, type, capacity, location, building, floor, status,
    availability_start, availability_end, disc_type,
    stage_type, sound_system
) VALUES
    (gen_random_uuid(), 'AUD-001', 'Grand Auditorium', 'AUDITORIUM', 500, 'Main Campus', 'Building E', '1', 'ACTIVE', '08:00', '23:00', 'Auditorium', 'Full Stage with Backdrop', 'Dolby Atmos 7.1 Surround'),
    (gen_random_uuid(), 'AUD-002', 'Mini Auditorium', 'AUDITORIUM', 150, 'Main Campus', 'Building F', '2', 'ACTIVE', '08:00', '22:00', 'Auditorium', 'Elevated Platform', 'Premium Stereo Sound System');

INSERT INTO facility (
    id, facility_code, name, type, capacity, location, building, floor, status,
    availability_start, availability_end, disc_type,
    brand, model, serial_number, maintenance_schedule
) VALUES
    (gen_random_uuid(), 'EQ-001', 'High-Speed 3D Printer', 'EQUIPMENT', 1, 'Main Campus', 'Building C', '5', 'ACTIVE', '08:00', '17:00', 'Equipment', 'Ultimaker', 'S5 Pro', 'UM-S5-2024-001', 'Monthly maintenance, Quarterly calibration'),
    (gen_random_uuid(), 'EQ-002', 'Scanning Electron Microscope', 'EQUIPMENT', 2, 'Main Campus', 'Building D', '3', 'ACTIVE', '08:00', '17:00', 'Equipment', 'ZEISS', 'Sigma 300 VP', 'ZEISS-SM-2024-001', 'Quarterly service, Annual calibration'),
    (gen_random_uuid(), 'EQ-003', 'Dual-Axis CNC Router', 'EQUIPMENT', 1, 'Main Campus', 'Building C', '6', 'ACTIVE', '08:00', '17:00', 'Equipment', 'Shopbot', 'PRT96 XL', 'SB-CNC-2024-001', 'Bi-weekly maintenance, Annual inspection');

INSERT INTO facility (
    id, facility_code, name, type, capacity, location, building, floor, status,
    availability_start, availability_end, disc_type,
    sports_type, equipment_available
) VALUES
    (gen_random_uuid(), 'SF-001', 'Indoor Basketball Court', 'SPORTS_FACILITY', 50, 'Sports Complex', 'Sports Building', '1', 'ACTIVE', '08:00', '21:00', 'SportsFacility', 'Basketball', 'Professional Hoops, LED Lighting, Sound System'),
    (gen_random_uuid(), 'SF-002', 'Multi-Purpose Gym', 'SPORTS_FACILITY', 80, 'Sports Complex', 'Sports Building', '1', 'ACTIVE', '06:00', '21:00', 'SportsFacility', 'General Fitness', 'Free Weights, Cardio Equipment, Strength Machines, Mirrors'),
    (gen_random_uuid(), 'SF-003', 'Tennis Court Complex', 'SPORTS_FACILITY', 30, 'Sports Complex', 'Outdoor Area', 'Ground', 'ACTIVE', '08:00', '20:00', 'SportsFacility', 'Tennis', 'Professional Courts (4), Ball Machine, Automated Scoring System');
