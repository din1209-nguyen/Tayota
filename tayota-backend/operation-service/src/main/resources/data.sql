INSERT INTO "USER" (id, created_at, email, login_provider, password_hash, provider_user_id, role, status)
VALUES
    ('00000000-0000-0000-0000-000000000001'::uuid, CURRENT_TIMESTAMP, 'admin@tayota.com', 'LOCAL', '$2a$10$IfWx2TdC1dE3SiLalrnWme3XWtVe3ZBAIfoQQrAsO0XAVOJhgTAWK', NULL, 'ADMIN', 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000002'::uuid, CURRENT_TIMESTAMP, 'manager@tayota.com', 'LOCAL', '$2a$10$IfWx2TdC1dE3SiLalrnWme3XWtVe3ZBAIfoQQrAsO0XAVOJhgTAWK', NULL, 'MANAGER', 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000003'::uuid, CURRENT_TIMESTAMP, 'advisor@tayota.com', 'LOCAL', '$2a$10$IfWx2TdC1dE3SiLalrnWme3XWtVe3ZBAIfoQQrAsO0XAVOJhgTAWK', NULL, 'SERVICE_ADVISOR', 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000004'::uuid, CURRENT_TIMESTAMP, 'mechanic@tayota.com', 'LOCAL', '$2a$10$IfWx2TdC1dE3SiLalrnWme3XWtVe3ZBAIfoQQrAsO0XAVOJhgTAWK', NULL, 'MECHANIC', 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000005'::uuid, CURRENT_TIMESTAMP, 'customer@tayota.com', 'LOCAL', '$2a$10$IfWx2TdC1dE3SiLalrnWme3XWtVe3ZBAIfoQQrAsO0XAVOJhgTAWK', NULL, 'USER', 'ACTIVE')
ON CONFLICT (id) DO NOTHING;

INSERT INTO "USER_PROFILE" (user_id, fullname, phone, gender, birth_date, address, avatar_url)
VALUES
    ('00000000-0000-0000-0000-000000000001'::uuid, 'Tayota Admin', '0901000001', TRUE, DATE '1988-01-01', 'Tayota Head Office', '/default-avatar.png'),
    ('00000000-0000-0000-0000-000000000002'::uuid, 'Tayota Manager', '0901000002', TRUE, DATE '1990-01-01', 'Tayota Head Office', '/default-avatar.png'),
    ('00000000-0000-0000-0000-000000000003'::uuid, 'Tayota Service Advisor', '0901000003', TRUE, DATE '1992-01-01', 'Tayota District 1', '/default-avatar.png'),
    ('00000000-0000-0000-0000-000000000004'::uuid, 'Tayota Mechanic', '0901000004', TRUE, DATE '1993-01-01', 'Tayota District 1', '/default-avatar.png'),
    ('00000000-0000-0000-0000-000000000005'::uuid, 'Tayota Customer', '0901000005', TRUE, DATE '1998-01-01', 'Ho Chi Minh City', '/default-avatar.png')
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO "DEALERSHIP" (id, name, address, car_quantity, accessory_quantity, latitude, longitude, place_id, phone, operating_hours, is_active, created_at)
VALUES
    ('10000000-0000-0000-0000-000000000001'::uuid, 'Tayota District 1', '12 Le Duan, District 1, Ho Chi Minh City', 2, 2, 10.78123456, 106.70234567, 'tayota-district-1', '02811112222', '08:00 - 18:00', TRUE, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

INSERT INTO "SERVICE_ADVISOR" (id, dealership_id)
VALUES
    ('00000000-0000-0000-0000-000000000003'::uuid, '10000000-0000-0000-0000-000000000001'::uuid)
ON CONFLICT (id) DO NOTHING;

INSERT INTO "MECHANIC" (id, dealership_id, specialty, average_rating, is_active)
VALUES
    ('00000000-0000-0000-0000-000000000004'::uuid, '10000000-0000-0000-0000-000000000001'::uuid, 'General maintenance', 4.80, TRUE)
ON CONFLICT (id) DO NOTHING;

INSERT INTO "CAR_STYLE" (id, name, description)
VALUES
    ('20000000-0000-0000-0000-000000000001'::uuid, 'Sedan', 'Comfortable passenger cars for city and family usage'),
    ('20000000-0000-0000-0000-000000000002'::uuid, 'SUV', 'High-clearance cars for family and long-distance trips')
ON CONFLICT (id) DO NOTHING;

INSERT INTO "CAR_SERIES" (id, car_style_id, name, description, created_at)
VALUES
    ('21000000-0000-0000-0000-000000000001'::uuid, '20000000-0000-0000-0000-000000000001'::uuid, 'Vios', 'Compact sedan series', CURRENT_TIMESTAMP),
    ('21000000-0000-0000-0000-000000000002'::uuid, '20000000-0000-0000-0000-000000000002'::uuid, 'Corolla Cross', 'Urban SUV series', CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

INSERT INTO "CAR_VERSION" (id, car_series_id, name, sale_percent, model_year, video_url, created_at)
VALUES
    ('22000000-0000-0000-0000-000000000001'::uuid, '21000000-0000-0000-0000-000000000001'::uuid, 'Vios 1.5G CVT', 0.00, 2026, 'https://example.com/videos/vios-15g.mp4', CURRENT_TIMESTAMP),
    ('22000000-0000-0000-0000-000000000002'::uuid, '21000000-0000-0000-0000-000000000002'::uuid, 'Corolla Cross 1.8V', 0.00, 2026, 'https://example.com/videos/corolla-cross-18v.mp4', CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

INSERT INTO "CAR_SPECIFICATION" (car_version_id, origin, fuel, number_of_seats, length, width, height, capacity, cylinder_capacity, cylinder, gearbox, maximum_speed, acceleration, torque, gross_weight_allowance, trademarks)
VALUES
    ('22000000-0000-0000-0000-000000000001'::uuid, 'Vietnam', 'Gasoline', 5, 4425, 1730, 1475, 42, '1496 cc', 4, 'CVT', 180, '11.0s', '140 Nm', 1550, 'Toyota'),
    ('22000000-0000-0000-0000-000000000002'::uuid, 'Thailand', 'Gasoline', 5, 4460, 1825, 1620, 47, '1798 cc', 4, 'CVT', 185, '10.5s', '172 Nm', 1850, 'Toyota')
ON CONFLICT (car_version_id) DO NOTHING;

INSERT INTO "EXTERIOR_COLOR" (id, color_name)
VALUES
    ('23000000-0000-0000-0000-000000000001'::uuid, 'White Pearl'),
    ('23000000-0000-0000-0000-000000000002'::uuid, 'Attitude Black')
ON CONFLICT (id) DO NOTHING;

INSERT INTO "INTERIOR_COLOR" (id, color_name)
VALUES
    ('24000000-0000-0000-0000-000000000001'::uuid, 'Black'),
    ('24000000-0000-0000-0000-000000000002'::uuid, 'Beige')
ON CONFLICT (id) DO NOTHING;

INSERT INTO "CAR_PRICE" (car_version_id, exterior_color_id, interior_color_id, price, ex_image_url, in_image_url)
VALUES
    ('22000000-0000-0000-0000-000000000001'::uuid, '23000000-0000-0000-0000-000000000001'::uuid, '24000000-0000-0000-0000-000000000001'::uuid, 545000000.00, '/images/cars/vios-white.png', '/images/cars/interior-black.png'),
    ('22000000-0000-0000-0000-000000000002'::uuid, '23000000-0000-0000-0000-000000000002'::uuid, '24000000-0000-0000-0000-000000000002'::uuid, 860000000.00, '/images/cars/corolla-cross-black.png', '/images/cars/interior-beige.png')
ON CONFLICT (car_version_id, exterior_color_id, interior_color_id) DO NOTHING;

INSERT INTO "CAR_GALLERY" (id, car_version_id, image_url)
VALUES
    ('25000000-0000-0000-0000-000000000001'::uuid, '22000000-0000-0000-0000-000000000001'::uuid, '/images/cars/vios-gallery-1.png'),
    ('25000000-0000-0000-0000-000000000002'::uuid, '22000000-0000-0000-0000-000000000002'::uuid, '/images/cars/corolla-cross-gallery-1.png')
ON CONFLICT (id) DO NOTHING;

INSERT INTO "CAR_ARTICLE" (id, car_version_id, type, title, content, image_url)
VALUES
    ('26000000-0000-0000-0000-000000000001'::uuid, '22000000-0000-0000-0000-000000000001'::uuid, 'FEATURE', 'Vios 1.5G CVT overview', 'Reliable sedan for daily commuting.', '/images/articles/vios-overview.png'),
    ('26000000-0000-0000-0000-000000000002'::uuid, '22000000-0000-0000-0000-000000000002'::uuid, 'FEATURE', 'Corolla Cross 1.8V overview', 'Flexible SUV for family trips.', '/images/articles/corolla-cross-overview.png')
ON CONFLICT (id) DO NOTHING;

INSERT INTO "CAR" (vin_id, car_version_id, dealership_id, engine_number, owner_user_id, status, producted_year, created_at)
VALUES
    ('TAYOTA00000000001', '22000000-0000-0000-0000-000000000001'::uuid, '10000000-0000-0000-0000-000000000001'::uuid, 'ENG-TAYOTA-0001', NULL, 'IN_STOCK', TIMESTAMP '2026-01-01 00:00:00', CURRENT_TIMESTAMP),
    ('TAYOTA00000000002', '22000000-0000-0000-0000-000000000002'::uuid, '10000000-0000-0000-0000-000000000001'::uuid, 'ENG-TAYOTA-0002', '00000000-0000-0000-0000-000000000005'::uuid, 'SOLD', TIMESTAMP '2026-01-01 00:00:00', CURRENT_TIMESTAMP)
ON CONFLICT (vin_id) DO NOTHING;

INSERT INTO "ACCESSORY" (id, model, brand, price, description, use_content, reminder_content, type)
VALUES
    ('30000000-0000-0000-0000-000000000001'::uuid, 'Dash Camera Basic', 'Tayota', 2500000.00, 'Front dash camera package', 'Install on windshield and connect to vehicle power.', 'Check recording status monthly.', 'ELECTRONIC'),
    ('30000000-0000-0000-0000-000000000002'::uuid, 'Floor Mat Standard', 'Tayota', 1200000.00, 'Durable all-weather floor mat', 'Place mats in correct seating position.', 'Clean with water and dry before reuse.', 'INTERIOR')
ON CONFLICT (id) DO NOTHING;

INSERT INTO "CAR_ACCESSORY" (car_version_id, accessory_id)
VALUES
    ('22000000-0000-0000-0000-000000000001'::uuid, '30000000-0000-0000-0000-000000000001'::uuid),
    ('22000000-0000-0000-0000-000000000002'::uuid, '30000000-0000-0000-0000-000000000002'::uuid)
ON CONFLICT (car_version_id, accessory_id) DO NOTHING;

INSERT INTO "ACCESSORY_INVENTORY" (dealership_id, accessory_id, quantity, last_updated)
VALUES
    ('10000000-0000-0000-0000-000000000001'::uuid, '30000000-0000-0000-0000-000000000001'::uuid, 10, CURRENT_TIMESTAMP),
    ('10000000-0000-0000-0000-000000000001'::uuid, '30000000-0000-0000-0000-000000000002'::uuid, 20, CURRENT_TIMESTAMP)
ON CONFLICT (dealership_id, accessory_id) DO NOTHING;

INSERT INTO "SERVICE_TIME_SLOT" (id, dealership_id, appointment_type, start_time, end_time, is_active, created_at, updated_at)
VALUES
    ('40000000-0000-0000-0000-000000000001'::uuid, '10000000-0000-0000-0000-000000000001'::uuid, 'TEST_DRIVE', TIME '09:00:00', TIME '10:00:00', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('40000000-0000-0000-0000-000000000002'::uuid, '10000000-0000-0000-0000-000000000001'::uuid, 'TEST_DRIVE', TIME '14:00:00', TIME '15:00:00', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('40000000-0000-0000-0000-000000000003'::uuid, '10000000-0000-0000-0000-000000000001'::uuid, 'SERVICE', TIME '10:00:00', TIME '11:00:00', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('40000000-0000-0000-0000-000000000004'::uuid, '10000000-0000-0000-0000-000000000001'::uuid, 'SERVICE', TIME '15:00:00', TIME '16:00:00', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;
