-- Operation service baseline seed.
-- Password hash below is the existing project BCrypt hash used by local seed users.
-- The insert style is compatible with PostgreSQL and H2 PostgreSQL mode.

INSERT INTO "USER" (id, created_at, email, login_provider, password_hash, provider_user_id, role, status)
SELECT id, CURRENT_TIMESTAMP, email, login_provider, password_hash, provider_user_id, role, status
FROM (VALUES
    ('00000000-0000-0000-0000-000000000001', 'admin@tayota.com', 'LOCAL', '$2a$10$IfWx2TdC1dE3SiLalrnWme3XWtVe3ZBAIfoQQrAsO0XAVOJhgTAWK', NULL, 'ADMIN', 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000002', 'manager@tayota.com', 'LOCAL', '$2a$10$IfWx2TdC1dE3SiLalrnWme3XWtVe3ZBAIfoQQrAsO0XAVOJhgTAWK', NULL, 'MANAGER', 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000003', 'advisor@tayota.com', 'LOCAL', '$2a$10$IfWx2TdC1dE3SiLalrnWme3XWtVe3ZBAIfoQQrAsO0XAVOJhgTAWK', NULL, 'SERVICE_ADVISOR', 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000004', 'mechanic@tayota.com', 'LOCAL', '$2a$10$IfWx2TdC1dE3SiLalrnWme3XWtVe3ZBAIfoQQrAsO0XAVOJhgTAWK', NULL, 'MECHANIC', 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000005', 'customer@tayota.com', 'LOCAL', '$2a$10$IfWx2TdC1dE3SiLalrnWme3XWtVe3ZBAIfoQQrAsO0XAVOJhgTAWK', NULL, 'USER', 'ACTIVE')
) AS seed(id, email, login_provider, password_hash, provider_user_id, role, status)
WHERE NOT EXISTS (SELECT 1 FROM "USER" existing WHERE existing.id = seed.id);

INSERT INTO "USER_PROFILE" (user_id, fullname, phone, gender, birth_date, address, avatar_url)
SELECT user_id, fullname, phone, gender, birth_date, address, avatar_url
FROM (VALUES
    ('00000000-0000-0000-0000-000000000001', 'Tayota Admin', '0901000001', TRUE, DATE '1988-01-01', 'Tayota Head Office', '/default-avatar.png'),
    ('00000000-0000-0000-0000-000000000002', 'Tayota Manager', '0901000002', TRUE, DATE '1990-01-01', 'Tayota Head Office', '/default-avatar.png'),
    ('00000000-0000-0000-0000-000000000003', 'Tayota Service Advisor', '0901000003', TRUE, DATE '1992-01-01', 'Tayota District 1', '/default-avatar.png'),
    ('00000000-0000-0000-0000-000000000004', 'Tayota Mechanic', '0901000004', TRUE, DATE '1993-01-01', 'Tayota District 1', '/default-avatar.png'),
    ('00000000-0000-0000-0000-000000000005', 'Tayota Customer', '0901000005', TRUE, DATE '1998-01-01', 'Ho Chi Minh City', '/default-avatar.png')
) AS seed(user_id, fullname, phone, gender, birth_date, address, avatar_url)
WHERE NOT EXISTS (SELECT 1 FROM "USER_PROFILE" existing WHERE existing.user_id = seed.user_id);

INSERT INTO "DEALERSHIP" (id, name, address, car_quantity, accessory_quantity, latitude, longitude, place_id, phone, operating_hours, is_active, created_at)
SELECT id, name, address, car_quantity, accessory_quantity, latitude, longitude, place_id, phone, operating_hours, is_active, CURRENT_TIMESTAMP
FROM (VALUES
    ('10000000-0000-0000-0000-000000000001', 'Tayota District 1', '12 Le Duan, District 1, Ho Chi Minh City', 2, 2, 10.78123456, 106.70234567, 'tayota-district-1', '02811112222', '08:00 - 18:00', TRUE)
) AS seed(id, name, address, car_quantity, accessory_quantity, latitude, longitude, place_id, phone, operating_hours, is_active)
WHERE NOT EXISTS (SELECT 1 FROM "DEALERSHIP" existing WHERE existing.id = seed.id);

INSERT INTO "SERVICE_ADVISOR" (id, dealership_id)
SELECT id, dealership_id
FROM (VALUES
    ('00000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000001')
) AS seed(id, dealership_id)
WHERE NOT EXISTS (SELECT 1 FROM "SERVICE_ADVISOR" existing WHERE existing.id = seed.id);

INSERT INTO "MECHANIC" (id, dealership_id, specialty, average_rating, is_active)
SELECT id, dealership_id, specialty, average_rating, is_active
FROM (VALUES
    ('00000000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-000000000001', 'General maintenance', 4.80, TRUE)
) AS seed(id, dealership_id, specialty, average_rating, is_active)
WHERE NOT EXISTS (SELECT 1 FROM "MECHANIC" existing WHERE existing.id = seed.id);

INSERT INTO "CAR_STYLE" (id, name, description)
SELECT id, name, description
FROM (VALUES
    ('20000000-0000-0000-0000-000000000001', 'Sedan', 'Comfortable passenger cars for city and family usage'),
    ('20000000-0000-0000-0000-000000000002', 'SUV', 'High-clearance cars for family and long-distance trips')
) AS seed(id, name, description)
WHERE NOT EXISTS (SELECT 1 FROM "CAR_STYLE" existing WHERE existing.id = seed.id);

INSERT INTO "CAR_SERIES" (id, car_style_id, name, description, created_at)
SELECT id, car_style_id, name, description, CURRENT_TIMESTAMP
FROM (VALUES
    ('21000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001', 'Vios', 'Compact sedan series'),
    ('21000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000002', 'Corolla Cross', 'Urban SUV series')
) AS seed(id, car_style_id, name, description)
WHERE NOT EXISTS (SELECT 1 FROM "CAR_SERIES" existing WHERE existing.id = seed.id);

INSERT INTO "CAR_VERSION" (id, car_series_id, name, sale_percent, model_year, video_url, created_at)
SELECT id, car_series_id, name, sale_percent, model_year, video_url, CURRENT_TIMESTAMP
FROM (VALUES
    ('22000000-0000-0000-0000-000000000001', '21000000-0000-0000-0000-000000000001', 'Vios 1.5G CVT', 0.00, 2026, 'https://example.com/videos/vios-15g.mp4'),
    ('22000000-0000-0000-0000-000000000002', '21000000-0000-0000-0000-000000000002', 'Corolla Cross 1.8V', 0.00, 2026, 'https://example.com/videos/corolla-cross-18v.mp4')
) AS seed(id, car_series_id, name, sale_percent, model_year, video_url)
WHERE NOT EXISTS (SELECT 1 FROM "CAR_VERSION" existing WHERE existing.id = seed.id);

INSERT INTO "CAR_SPECIFICATION" (car_version_id, origin, fuel, number_of_seats, length, width, height, capacity, cylinder_capacity, cylinder, gearbox, maximum_speed, acceleration, torque, gross_weight_allowance, trademarks)
SELECT car_version_id, origin, fuel, number_of_seats, length, width, height, capacity, cylinder_capacity, cylinder, gearbox, maximum_speed, acceleration, torque, gross_weight_allowance, trademarks
FROM (VALUES
    ('22000000-0000-0000-0000-000000000001', 'Vietnam', 'Gasoline', 5, 4425, 1730, 1475, 42, '1496 cc', 4, 'CVT', 180, '11.0s', '140 Nm', 1550, 'Toyota'),
    ('22000000-0000-0000-0000-000000000002', 'Thailand', 'Gasoline', 5, 4460, 1825, 1620, 47, '1798 cc', 4, 'CVT', 185, '10.5s', '172 Nm', 1850, 'Toyota')
) AS seed(car_version_id, origin, fuel, number_of_seats, length, width, height, capacity, cylinder_capacity, cylinder, gearbox, maximum_speed, acceleration, torque, gross_weight_allowance, trademarks)
WHERE NOT EXISTS (SELECT 1 FROM "CAR_SPECIFICATION" existing WHERE existing.car_version_id = seed.car_version_id);

INSERT INTO "EXTERIOR_COLOR" (id, color_name)
SELECT id, color_name
FROM (VALUES
    ('23000000-0000-0000-0000-000000000001', 'White Pearl'),
    ('23000000-0000-0000-0000-000000000002', 'Attitude Black')
) AS seed(id, color_name)
WHERE NOT EXISTS (SELECT 1 FROM "EXTERIOR_COLOR" existing WHERE existing.id = seed.id);

INSERT INTO "INTERIOR_COLOR" (id, color_name)
SELECT id, color_name
FROM (VALUES
    ('24000000-0000-0000-0000-000000000001', 'Black'),
    ('24000000-0000-0000-0000-000000000002', 'Beige')
) AS seed(id, color_name)
WHERE NOT EXISTS (SELECT 1 FROM "INTERIOR_COLOR" existing WHERE existing.id = seed.id);

INSERT INTO "CAR_PRICE" (car_version_id, exterior_color_id, interior_color_id, price, ex_image_url, in_image_url)
SELECT car_version_id, exterior_color_id, interior_color_id, price, ex_image_url, in_image_url
FROM (VALUES
    ('22000000-0000-0000-0000-000000000001', '23000000-0000-0000-0000-000000000001', '24000000-0000-0000-0000-000000000001', 545000000.00, '/images/cars/vios-white.png', '/images/cars/interior-black.png'),
    ('22000000-0000-0000-0000-000000000002', '23000000-0000-0000-0000-000000000002', '24000000-0000-0000-0000-000000000002', 860000000.00, '/images/cars/corolla-cross-black.png', '/images/cars/interior-beige.png')
) AS seed(car_version_id, exterior_color_id, interior_color_id, price, ex_image_url, in_image_url)
WHERE NOT EXISTS (
    SELECT 1 FROM "CAR_PRICE" existing
    WHERE existing.car_version_id = seed.car_version_id
      AND existing.exterior_color_id = seed.exterior_color_id
      AND existing.interior_color_id = seed.interior_color_id
);

INSERT INTO "CAR_GALLERY" (id, car_version_id, image_url)
SELECT id, car_version_id, image_url
FROM (VALUES
    ('25000000-0000-0000-0000-000000000001', '22000000-0000-0000-0000-000000000001', '/images/cars/vios-gallery-1.png'),
    ('25000000-0000-0000-0000-000000000002', '22000000-0000-0000-0000-000000000002', '/images/cars/corolla-cross-gallery-1.png')
) AS seed(id, car_version_id, image_url)
WHERE NOT EXISTS (SELECT 1 FROM "CAR_GALLERY" existing WHERE existing.id = seed.id);

INSERT INTO "CAR_ARTICLE" (id, car_version_id, type, title, content, image_url)
SELECT id, car_version_id, type, title, content, image_url
FROM (VALUES
    ('26000000-0000-0000-0000-000000000001', '22000000-0000-0000-0000-000000000001', 'FEATURE', 'Vios 1.5G CVT overview', 'Reliable sedan for daily commuting.', '/images/articles/vios-overview.png'),
    ('26000000-0000-0000-0000-000000000002', '22000000-0000-0000-0000-000000000002', 'FEATURE', 'Corolla Cross 1.8V overview', 'Flexible SUV for family trips.', '/images/articles/corolla-cross-overview.png')
) AS seed(id, car_version_id, type, title, content, image_url)
WHERE NOT EXISTS (SELECT 1 FROM "CAR_ARTICLE" existing WHERE existing.id = seed.id);

INSERT INTO "CAR" (vin_id, car_version_id, dealership_id, engine_number, owner_user_id, status, producted_year, created_at)
SELECT vin_id, car_version_id, dealership_id, engine_number, owner_user_id, status, producted_year, CURRENT_TIMESTAMP
FROM (VALUES
    ('TAYOTA00000000001', '22000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', 'ENG-TAYOTA-0001', NULL, 'IN_STOCK', TIMESTAMP '2026-01-01 00:00:00'),
    ('TAYOTA00000000002', '22000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001', 'ENG-TAYOTA-0002', '00000000-0000-0000-0000-000000000005', 'SOLD', TIMESTAMP '2026-01-01 00:00:00')
) AS seed(vin_id, car_version_id, dealership_id, engine_number, owner_user_id, status, producted_year)
WHERE NOT EXISTS (SELECT 1 FROM "CAR" existing WHERE existing.vin_id = seed.vin_id);

INSERT INTO "ACCESSORY" (id, model, brand, price, description, use_content, reminder_content, type)
SELECT id, model, brand, price, description, use_content, reminder_content, type
FROM (VALUES
    ('30000000-0000-0000-0000-000000000001', 'Dash Camera Basic', 'Tayota', 2500000.00, 'Front dash camera package', 'Install on windshield and connect to vehicle power.', 'Check recording status monthly.', 'ELECTRONIC'),
    ('30000000-0000-0000-0000-000000000002', 'Floor Mat Standard', 'Tayota', 1200000.00, 'Durable all-weather floor mat', 'Place mats in correct seating position.', 'Clean with water and dry before reuse.', 'INTERIOR')
) AS seed(id, model, brand, price, description, use_content, reminder_content, type)
WHERE NOT EXISTS (SELECT 1 FROM "ACCESSORY" existing WHERE existing.id = seed.id);

INSERT INTO "CAR_ACCESSORY" (car_version_id, accessory_id)
SELECT car_version_id, accessory_id
FROM (VALUES
    ('22000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001'),
    ('22000000-0000-0000-0000-000000000002', '30000000-0000-0000-0000-000000000002')
) AS seed(car_version_id, accessory_id)
WHERE NOT EXISTS (
    SELECT 1 FROM "CAR_ACCESSORY" existing
    WHERE existing.car_version_id = seed.car_version_id
      AND existing.accessory_id = seed.accessory_id
);

INSERT INTO "ACCESSORY_INVENTORY" (dealership_id, accessory_id, quantity, last_updated)
SELECT dealership_id, accessory_id, quantity, CURRENT_TIMESTAMP
FROM (VALUES
    ('10000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001', 10),
    ('10000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000002', 20)
) AS seed(dealership_id, accessory_id, quantity)
WHERE NOT EXISTS (
    SELECT 1 FROM "ACCESSORY_INVENTORY" existing
    WHERE existing.dealership_id = seed.dealership_id
      AND existing.accessory_id = seed.accessory_id
);

INSERT INTO "SERVICE_TIME_SLOT" (id, dealership_id, appointment_type, start_time, end_time, is_active, created_at, updated_at)
SELECT id, dealership_id, appointment_type, start_time, end_time, is_active, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (VALUES
    ('40000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', 'TEST_DRIVE', TIME '09:00:00', TIME '10:00:00', TRUE),
    ('40000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001', 'TEST_DRIVE', TIME '14:00:00', TIME '15:00:00', TRUE),
    ('40000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000001', 'SERVICE', TIME '10:00:00', TIME '11:00:00', TRUE),
    ('40000000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-000000000001', 'SERVICE', TIME '15:00:00', TIME '16:00:00', TRUE)
) AS seed(id, dealership_id, appointment_type, start_time, end_time, is_active)
WHERE NOT EXISTS (SELECT 1 FROM "SERVICE_TIME_SLOT" existing WHERE existing.id = seed.id);
