DROP TABLE IF EXISTS "ACCESSORY_INVENTORY";
ALTER TABLE "DEALERSHIP" DROP COLUMN IF EXISTS accessory_quantity;

INSERT INTO "USER" (id, created_at, email, login_provider, password_hash, provider_user_id, role, status)
VALUES
    ('00000000-0000-0000-0000-000000000001'::uuid, CURRENT_TIMESTAMP, 'admin@tayota.com', 'LOCAL', '$2a$10$DisRh1o1St0wbkblcWXebea67jmF2/xB2IAXA/4Ir7K0kQ.9bGXbG', NULL, 'ADMIN', 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000002'::uuid, CURRENT_TIMESTAMP, 'manager@tayota.com', 'LOCAL', '$2a$10$DisRh1o1St0wbkblcWXebea67jmF2/xB2IAXA/4Ir7K0kQ.9bGXbG', NULL, 'MANAGER', 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000003'::uuid, CURRENT_TIMESTAMP, 'advisor@tayota.com', 'LOCAL', '$2a$10$DisRh1o1St0wbkblcWXebea67jmF2/xB2IAXA/4Ir7K0kQ.9bGXbG', NULL, 'SERVICE_ADVISOR', 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000004'::uuid, CURRENT_TIMESTAMP, 'mechanic@tayota.com', 'LOCAL', '$2a$10$DisRh1o1St0wbkblcWXebea67jmF2/xB2IAXA/4Ir7K0kQ.9bGXbG', NULL, 'MECHANIC', 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000005'::uuid, CURRENT_TIMESTAMP, 'customer@tayota.com', 'LOCAL', '$2a$10$DisRh1o1St0wbkblcWXebea67jmF2/xB2IAXA/4Ir7K0kQ.9bGXbG', NULL, 'USER', 'ACTIVE')
ON CONFLICT (id) DO NOTHING;

INSERT INTO "USER_PROFILE" (user_id, fullname, phone, gender, birth_date, address, avatar_url)
VALUES
    ('00000000-0000-0000-0000-000000000001'::uuid, 'Tayota Admin', '0901000001', TRUE, DATE '1988-01-01', 'Tayota Head Office', '/default-avatar.png'),
    ('00000000-0000-0000-0000-000000000002'::uuid, 'Tayota Manager', '0901000002', TRUE, DATE '1990-01-01', 'Tayota Head Office', '/default-avatar.png'),
    ('00000000-0000-0000-0000-000000000003'::uuid, 'Tayota Service Advisor', '0901000003', TRUE, DATE '1992-01-01', 'Tayota District 1', '/default-avatar.png'),
    ('00000000-0000-0000-0000-000000000004'::uuid, 'Tayota Mechanic', '0901000004', TRUE, DATE '1993-01-01', 'Tayota District 1', '/default-avatar.png'),
    ('00000000-0000-0000-0000-000000000005'::uuid, 'Tayota Customer', '0901000005', TRUE, DATE '1998-01-01', 'Ho Chi Minh City', '/default-avatar.png')
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO "DEALERSHIP" (id, name, address, car_quantity, latitude, longitude, place_id, phone, operating_hours, is_active, created_at)
VALUES
    ('10000000-0000-0000-0000-000000000001'::uuid, 'Tayota District 1', '12 Le Duan, District 1, Ho Chi Minh City', 2, 10.78123456, 106.70234567, 'tayota-district-1', '02811112222', '08:00 - 18:00', TRUE, CURRENT_TIMESTAMP)
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

INSERT INTO "CAR_ARTICLE" (id, car_version_id, type, title, content, image_url, is_published, created_at, updated_at)
VALUES
    ('26000000-0000-0000-0000-000000000001'::uuid, '22000000-0000-0000-0000-000000000001'::uuid, 'FEATURE', 'Vios 1.5G CVT overview', 'Reliable sedan for daily commuting.', '/images/articles/vios-overview.png', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('26000000-0000-0000-0000-000000000002'::uuid, '22000000-0000-0000-0000-000000000002'::uuid, 'FEATURE', 'Corolla Cross 1.8V overview', 'Flexible SUV for family trips.', '/images/articles/corolla-cross-overview.png', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
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

INSERT INTO "SERVICE_TIME_SLOT" (id, dealership_id, appointment_type, start_time, end_time, is_active, created_at, updated_at)
VALUES
    ('40000000-0000-0000-0000-000000000001'::uuid, '10000000-0000-0000-0000-000000000001'::uuid, 'TEST_DRIVE', TIME '09:00:00', TIME '10:00:00', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('40000000-0000-0000-0000-000000000002'::uuid, '10000000-0000-0000-0000-000000000001'::uuid, 'TEST_DRIVE', TIME '14:00:00', TIME '15:00:00', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('40000000-0000-0000-0000-000000000003'::uuid, '10000000-0000-0000-0000-000000000001'::uuid, 'SERVICE', TIME '10:00:00', TIME '11:00:00', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('40000000-0000-0000-0000-000000000004'::uuid, '10000000-0000-0000-0000-000000000001'::uuid, 'SERVICE', TIME '15:00:00', TIME '16:00:00', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

INSERT INTO "USER" (id, created_at, email, login_provider, password_hash, provider_user_id, role, status)
VALUES
    ('00000000-0000-0000-0000-000000000006'::uuid, CURRENT_TIMESTAMP, 'assistant@tayota.vn', 'LOCAL', '$2a$10$DisRh1o1St0wbkblcWXebea67jmF2/xB2IAXA/4Ir7K0kQ.9bGXbG', NULL, 'ASSISTANT', 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000007'::uuid, CURRENT_TIMESTAMP, 'admin@tayota.vn', 'LOCAL', '$2a$10$DisRh1o1St0wbkblcWXebea67jmF2/xB2IAXA/4Ir7K0kQ.9bGXbG', NULL, 'ADMIN', 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000008'::uuid, CURRENT_TIMESTAMP, 'mechanic@tayota.vn', 'LOCAL', '$2a$10$DisRh1o1St0wbkblcWXebea67jmF2/xB2IAXA/4Ir7K0kQ.9bGXbG', NULL, 'MECHANIC', 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000009'::uuid, CURRENT_TIMESTAMP, 'user@tayota.vn', 'LOCAL', '$2a$10$DisRh1o1St0wbkblcWXebea67jmF2/xB2IAXA/4Ir7K0kQ.9bGXbG', NULL, 'USER', 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000010'::uuid, CURRENT_TIMESTAMP, 'admin.demo@tayota.com', 'LOCAL', '$2a$10$DisRh1o1St0wbkblcWXebea67jmF2/xB2IAXA/4Ir7K0kQ.9bGXbG', NULL, 'ADMIN', 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000011'::uuid, CURRENT_TIMESTAMP, 'manager.demo@tayota.com', 'LOCAL', '$2a$10$DisRh1o1St0wbkblcWXebea67jmF2/xB2IAXA/4Ir7K0kQ.9bGXbG', NULL, 'MANAGER', 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000012'::uuid, CURRENT_TIMESTAMP, 'advisor.demo@tayota.com', 'LOCAL', '$2a$10$DisRh1o1St0wbkblcWXebea67jmF2/xB2IAXA/4Ir7K0kQ.9bGXbG', NULL, 'SERVICE_ADVISOR', 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000013'::uuid, CURRENT_TIMESTAMP, 'assistant.demo@tayota.com', 'LOCAL', '$2a$10$DisRh1o1St0wbkblcWXebea67jmF2/xB2IAXA/4Ir7K0kQ.9bGXbG', NULL, 'ASSISTANT', 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000014'::uuid, CURRENT_TIMESTAMP, 'mechanic.demo@tayota.com', 'LOCAL', '$2a$10$DisRh1o1St0wbkblcWXebea67jmF2/xB2IAXA/4Ir7K0kQ.9bGXbG', NULL, 'MECHANIC', 'ACTIVE'),
    ('00000000-0000-0000-0000-000000000015'::uuid, CURRENT_TIMESTAMP, 'customer.demo@tayota.com', 'LOCAL', '$2a$10$DisRh1o1St0wbkblcWXebea67jmF2/xB2IAXA/4Ir7K0kQ.9bGXbG', NULL, 'USER', 'ACTIVE')
ON CONFLICT (id) DO NOTHING;

INSERT INTO "USER_PROFILE" (user_id, fullname, phone, gender, birth_date, address, avatar_url)
VALUES
    ('00000000-0000-0000-0000-000000000006'::uuid, 'Tayota Assistant', '0902000006', TRUE, DATE '1994-02-02', 'Tayota Phu My Hung', '/default-avatar.png'),
    ('00000000-0000-0000-0000-000000000007'::uuid, 'Tayota Admin VN', '0902000007', TRUE, DATE '1987-02-02', 'Tayota Head Office', '/default-avatar.png'),
    ('00000000-0000-0000-0000-000000000008'::uuid, 'Tayota Mechanic VN', '0902000008', TRUE, DATE '1991-02-02', 'Tayota Thu Duc', '/default-avatar.png'),
    ('00000000-0000-0000-0000-000000000009'::uuid, 'Tayota User', '0902000009', TRUE, DATE '1999-02-02', 'Ho Chi Minh City', '/default-avatar.png'),
    ('00000000-0000-0000-0000-000000000010'::uuid, 'Tayota Demo Admin', '0903000010', TRUE, DATE '1988-01-01', 'Tayota Head Office', '/default-avatar.png'),
    ('00000000-0000-0000-0000-000000000011'::uuid, 'Tayota Demo Manager', '0903000011', TRUE, DATE '1989-02-01', 'Tayota Head Office', '/default-avatar.png'),
    ('00000000-0000-0000-0000-000000000012'::uuid, 'Tayota Demo Advisor', '0903000012', TRUE, DATE '1991-03-01', 'Tayota District 1', '/default-avatar.png'),
    ('00000000-0000-0000-0000-000000000013'::uuid, 'Tayota Demo Assistant', '0903000013', FALSE, DATE '1993-04-01', 'Tayota District 1', '/default-avatar.png'),
    ('00000000-0000-0000-0000-000000000014'::uuid, 'Tayota Demo Mechanic', '0903000014', TRUE, DATE '1992-05-01', 'Tayota District 1', '/default-avatar.png'),
    ('00000000-0000-0000-0000-000000000015'::uuid, 'Tayota Demo Customer', '0903000015', FALSE, DATE '1997-06-01', 'Ho Chi Minh City', '/default-avatar.png')
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO "DEALERSHIP" (id, name, address, car_quantity, latitude, longitude, place_id, phone, operating_hours, is_active, created_at)
VALUES
    ('10000000-0000-0000-0000-000000000002'::uuid, 'Tayota Phu My Hung', '105 Nguyen Luong Bang, District 7, Ho Chi Minh City', 18, 10.72981234, 106.70381234, 'tayota-phu-my-hung', '02822223333', '08:00 - 18:30', TRUE, CURRENT_TIMESTAMP),
    ('10000000-0000-0000-0000-000000000003'::uuid, 'Tayota Thu Duc', '22 Vo Van Ngan, Thu Duc City, Ho Chi Minh City', 22, 10.85061234, 106.77131234, 'tayota-thu-duc', '02833334444', '08:00 - 18:00', TRUE, CURRENT_TIMESTAMP),
    ('10000000-0000-0000-0000-000000000004'::uuid, 'Tayota Hanoi West', '68 Le Van Luong, Thanh Xuan, Hanoi', 16, 21.00761234, 105.80161234, 'tayota-hanoi-west', '02444445555', '08:00 - 18:00', TRUE, CURRENT_TIMESTAMP),
    ('10000000-0000-0000-0000-000000000005'::uuid, 'Tayota Da Nang', '09 Nguyen Van Linh, Hai Chau, Da Nang', 14, 16.06041234, 108.22191234, 'tayota-da-nang', '02365556666', '08:00 - 17:30', TRUE, CURRENT_TIMESTAMP),
    ('10000000-0000-0000-0000-000000000006'::uuid, 'Tayota Can Tho', '188 30/4 Street, Ninh Kieu, Can Tho', 12, 10.03321234, 105.78361234, 'tayota-can-tho', '02926667777', '08:00 - 17:30', TRUE, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

INSERT INTO "MECHANIC" (id, dealership_id, specialty, average_rating, is_active)
VALUES
    ('00000000-0000-0000-0000-000000000008'::uuid, '10000000-0000-0000-0000-000000000003'::uuid, 'Hybrid diagnostics', 4.90, TRUE),
    ('00000000-0000-0000-0000-000000000014'::uuid, '10000000-0000-0000-0000-000000000001'::uuid, 'General maintenance', 4.80, TRUE)
ON CONFLICT (id) DO NOTHING;

INSERT INTO "SERVICE_ADVISOR" (id, dealership_id)
VALUES
    ('00000000-0000-0000-0000-000000000012'::uuid, '10000000-0000-0000-0000-000000000001'::uuid)
ON CONFLICT (id) DO NOTHING;

INSERT INTO "CAR_STYLE" (id, name, description)
VALUES
    ('20000000-0000-0000-0000-000000000003'::uuid, 'Hybrid', 'Efficient electrified driving for city and long-distance usage'),
    ('20000000-0000-0000-0000-000000000004'::uuid, 'MPV', 'Flexible multi-purpose cars for families and business transport'),
    ('20000000-0000-0000-0000-000000000005'::uuid, 'Luxury', 'Premium technology, comfort and executive presence')
ON CONFLICT (id) DO NOTHING;

INSERT INTO "CAR_SERIES" (id, car_style_id, name, description, created_at)
VALUES
    ('21000000-0000-0000-0000-000000000003'::uuid, '20000000-0000-0000-0000-000000000001'::uuid, 'Camry', 'Executive sedan series', CURRENT_TIMESTAMP),
    ('21000000-0000-0000-0000-000000000004'::uuid, '20000000-0000-0000-0000-000000000002'::uuid, 'Fortuner', 'Seven-seat SUV series', CURRENT_TIMESTAMP),
    ('21000000-0000-0000-0000-000000000005'::uuid, '20000000-0000-0000-0000-000000000003'::uuid, 'Yaris Cross', 'Compact hybrid crossover series', CURRENT_TIMESTAMP),
    ('21000000-0000-0000-0000-000000000006'::uuid, '20000000-0000-0000-0000-000000000004'::uuid, 'Innova Cross', 'Family MPV series', CURRENT_TIMESTAMP),
    ('21000000-0000-0000-0000-000000000007'::uuid, '20000000-0000-0000-0000-000000000005'::uuid, 'Land Cruiser Prado', 'Premium adventure SUV series', CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

INSERT INTO "CAR_VERSION" (id, car_series_id, name, sale_percent, model_year, video_url, created_at)
VALUES
    ('22000000-0000-0000-0000-000000000003'::uuid, '21000000-0000-0000-0000-000000000003'::uuid, 'Camry 2.0Q', 0.00, 2026, 'https://example.com/videos/camry-20q.mp4', CURRENT_TIMESTAMP),
    ('22000000-0000-0000-0000-000000000004'::uuid, '21000000-0000-0000-0000-000000000003'::uuid, 'Camry 2.5HV', 0.00, 2026, 'https://example.com/videos/camry-25hv.mp4', CURRENT_TIMESTAMP),
    ('22000000-0000-0000-0000-000000000005'::uuid, '21000000-0000-0000-0000-000000000004'::uuid, 'Fortuner Legender 2.4AT', 0.00, 2026, 'https://example.com/videos/fortuner-legender.mp4', CURRENT_TIMESTAMP),
    ('22000000-0000-0000-0000-000000000006'::uuid, '21000000-0000-0000-0000-000000000005'::uuid, 'Yaris Cross Hybrid', 0.00, 2026, 'https://example.com/videos/yaris-cross-hybrid.mp4', CURRENT_TIMESTAMP),
    ('22000000-0000-0000-0000-000000000007'::uuid, '21000000-0000-0000-0000-000000000006'::uuid, 'Innova Cross 2.0V', 0.00, 2026, 'https://example.com/videos/innova-cross.mp4', CURRENT_TIMESTAMP),
    ('22000000-0000-0000-0000-000000000008'::uuid, '21000000-0000-0000-0000-000000000007'::uuid, 'Land Cruiser Prado VX', 0.00, 2026, 'https://example.com/videos/prado-vx.mp4', CURRENT_TIMESTAMP),
    ('22000000-0000-0000-0000-000000000009'::uuid, '21000000-0000-0000-0000-000000000002'::uuid, 'Corolla Cross HEV', 0.00, 2026, 'https://example.com/videos/corolla-cross-hev.mp4', CURRENT_TIMESTAMP),
    ('22000000-0000-0000-0000-000000000010'::uuid, '21000000-0000-0000-0000-000000000001'::uuid, 'Vios 1.5E CVT', 0.00, 2026, 'https://example.com/videos/vios-15e.mp4', CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

INSERT INTO "CAR_SPECIFICATION" (car_version_id, origin, fuel, number_of_seats, length, width, height, capacity, cylinder_capacity, cylinder, gearbox, maximum_speed, acceleration, torque, gross_weight_allowance, trademarks)
VALUES
    ('22000000-0000-0000-0000-000000000003'::uuid, 'Thailand', 'Gasoline', 5, 4885, 1840, 1445, 60, '1987 cc', 4, 'CVT', 210, '9.5s', '206 Nm', 2100, 'Toyota'),
    ('22000000-0000-0000-0000-000000000004'::uuid, 'Thailand', 'Hybrid', 5, 4885, 1840, 1445, 50, '2487 cc', 4, 'e-CVT', 220, '8.3s', '221 Nm', 2140, 'Toyota'),
    ('22000000-0000-0000-0000-000000000005'::uuid, 'Indonesia', 'Diesel', 7, 4795, 1855, 1835, 80, '2393 cc', 4, '6AT', 180, '11.2s', '400 Nm', 2610, 'Toyota'),
    ('22000000-0000-0000-0000-000000000006'::uuid, 'Indonesia', 'Hybrid', 5, 4310, 1770, 1615, 36, '1490 cc', 3, 'e-CVT', 170, '11.0s', '121 Nm', 1690, 'Toyota'),
    ('22000000-0000-0000-0000-000000000007'::uuid, 'Indonesia', 'Gasoline', 7, 4755, 1850, 1795, 52, '1987 cc', 4, 'CVT', 180, '10.8s', '205 Nm', 2210, 'Toyota'),
    ('22000000-0000-0000-0000-000000000008'::uuid, 'Japan', 'Gasoline', 7, 4925, 1980, 1935, 80, '2393 cc', 4, '8AT', 190, '9.7s', '430 Nm', 2950, 'Toyota'),
    ('22000000-0000-0000-0000-000000000009'::uuid, 'Thailand', 'Hybrid', 5, 4460, 1825, 1620, 43, '1798 cc', 4, 'e-CVT', 180, '10.0s', '142 Nm', 1850, 'Toyota'),
    ('22000000-0000-0000-0000-000000000010'::uuid, 'Vietnam', 'Gasoline', 5, 4425, 1730, 1475, 42, '1496 cc', 4, 'CVT', 180, '11.5s', '140 Nm', 1550, 'Toyota')
ON CONFLICT (car_version_id) DO NOTHING;

INSERT INTO "CAR_PRICE" (car_version_id, exterior_color_id, interior_color_id, price, ex_image_url, in_image_url)
VALUES
    ('22000000-0000-0000-0000-000000000003'::uuid, '23000000-0000-0000-0000-000000000001'::uuid, '24000000-0000-0000-0000-000000000001'::uuid, 1220000000.00, '/images/cars/camry-white.png', '/images/cars/interior-black.png'),
    ('22000000-0000-0000-0000-000000000004'::uuid, '23000000-0000-0000-0000-000000000002'::uuid, '24000000-0000-0000-0000-000000000002'::uuid, 1530000000.00, '/images/cars/camry-hybrid-black.png', '/images/cars/interior-beige.png'),
    ('22000000-0000-0000-0000-000000000005'::uuid, '23000000-0000-0000-0000-000000000002'::uuid, '24000000-0000-0000-0000-000000000001'::uuid, 1350000000.00, '/images/cars/fortuner-black.png', '/images/cars/interior-black.png'),
    ('22000000-0000-0000-0000-000000000006'::uuid, '23000000-0000-0000-0000-000000000001'::uuid, '24000000-0000-0000-0000-000000000002'::uuid, 765000000.00, '/images/cars/yaris-cross-white.png', '/images/cars/interior-beige.png'),
    ('22000000-0000-0000-0000-000000000007'::uuid, '23000000-0000-0000-0000-000000000001'::uuid, '24000000-0000-0000-0000-000000000001'::uuid, 995000000.00, '/images/cars/innova-cross-white.png', '/images/cars/interior-black.png'),
    ('22000000-0000-0000-0000-000000000008'::uuid, '23000000-0000-0000-0000-000000000002'::uuid, '24000000-0000-0000-0000-000000000001'::uuid, 3480000000.00, '/images/cars/prado-black.png', '/images/cars/interior-black.png'),
    ('22000000-0000-0000-0000-000000000009'::uuid, '23000000-0000-0000-0000-000000000001'::uuid, '24000000-0000-0000-0000-000000000002'::uuid, 955000000.00, '/images/cars/corolla-cross-hev-white.png', '/images/cars/interior-beige.png'),
    ('22000000-0000-0000-0000-000000000010'::uuid, '23000000-0000-0000-0000-000000000001'::uuid, '24000000-0000-0000-0000-000000000001'::uuid, 498000000.00, '/images/cars/vios-e-white.png', '/images/cars/interior-black.png')
ON CONFLICT (car_version_id, exterior_color_id, interior_color_id) DO NOTHING;

INSERT INTO "CAR_GALLERY" (id, car_version_id, image_url)
VALUES
    ('25000000-0000-0000-0000-000000000003'::uuid, '22000000-0000-0000-0000-000000000003'::uuid, '/images/cars/camry-gallery-1.png'),
    ('25000000-0000-0000-0000-000000000004'::uuid, '22000000-0000-0000-0000-000000000004'::uuid, '/images/cars/camry-hybrid-gallery-1.png'),
    ('25000000-0000-0000-0000-000000000005'::uuid, '22000000-0000-0000-0000-000000000005'::uuid, '/images/cars/fortuner-gallery-1.png'),
    ('25000000-0000-0000-0000-000000000006'::uuid, '22000000-0000-0000-0000-000000000006'::uuid, '/images/cars/yaris-cross-gallery-1.png'),
    ('25000000-0000-0000-0000-000000000007'::uuid, '22000000-0000-0000-0000-000000000007'::uuid, '/images/cars/innova-cross-gallery-1.png'),
    ('25000000-0000-0000-0000-000000000008'::uuid, '22000000-0000-0000-0000-000000000008'::uuid, '/images/cars/prado-gallery-1.png')
ON CONFLICT (id) DO NOTHING;

INSERT INTO "CAR" (vin_id, car_version_id, dealership_id, engine_number, owner_user_id, status, producted_year, created_at)
VALUES
    ('TAYOTA00000000003', '22000000-0000-0000-0000-000000000003'::uuid, '10000000-0000-0000-0000-000000000002'::uuid, 'ENG-TAYOTA-0003', NULL, 'IN_STOCK', TIMESTAMP '2026-01-01 00:00:00', CURRENT_TIMESTAMP),
    ('TAYOTA00000000004', '22000000-0000-0000-0000-000000000004'::uuid, '10000000-0000-0000-0000-000000000003'::uuid, 'ENG-TAYOTA-0004', NULL, 'IN_STOCK', TIMESTAMP '2026-01-01 00:00:00', CURRENT_TIMESTAMP),
    ('TAYOTA00000000005', '22000000-0000-0000-0000-000000000005'::uuid, '10000000-0000-0000-0000-000000000004'::uuid, 'ENG-TAYOTA-0005', NULL, 'IN_STOCK', TIMESTAMP '2026-01-01 00:00:00', CURRENT_TIMESTAMP),
    ('TAYOTA00000000006', '22000000-0000-0000-0000-000000000006'::uuid, '10000000-0000-0000-0000-000000000005'::uuid, 'ENG-TAYOTA-0006', NULL, 'IN_STOCK', TIMESTAMP '2026-01-01 00:00:00', CURRENT_TIMESTAMP),
    ('TAYOTA00000000007', '22000000-0000-0000-0000-000000000007'::uuid, '10000000-0000-0000-0000-000000000006'::uuid, 'ENG-TAYOTA-0007', NULL, 'IN_STOCK', TIMESTAMP '2026-01-01 00:00:00', CURRENT_TIMESTAMP)
ON CONFLICT (vin_id) DO NOTHING;

INSERT INTO "SERVICE_TIME_SLOT" (id, dealership_id, appointment_type, start_time, end_time, is_active, created_at, updated_at)
VALUES
    ('40000000-0000-0000-0000-000000000005'::uuid, '10000000-0000-0000-0000-000000000002'::uuid, 'TEST_DRIVE', TIME '08:30:00', TIME '09:30:00', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('40000000-0000-0000-0000-000000000006'::uuid, '10000000-0000-0000-0000-000000000002'::uuid, 'TEST_DRIVE', TIME '10:30:00', TIME '11:30:00', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('40000000-0000-0000-0000-000000000007'::uuid, '10000000-0000-0000-0000-000000000002'::uuid, 'SERVICE', TIME '13:30:00', TIME '14:30:00', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('40000000-0000-0000-0000-000000000008'::uuid, '10000000-0000-0000-0000-000000000003'::uuid, 'TEST_DRIVE', TIME '09:30:00', TIME '10:30:00', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('40000000-0000-0000-0000-000000000009'::uuid, '10000000-0000-0000-0000-000000000003'::uuid, 'SERVICE', TIME '14:30:00', TIME '15:30:00', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('40000000-0000-0000-0000-000000000010'::uuid, '10000000-0000-0000-0000-000000000004'::uuid, 'TEST_DRIVE', TIME '09:00:00', TIME '10:00:00', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('40000000-0000-0000-0000-000000000011'::uuid, '10000000-0000-0000-0000-000000000005'::uuid, 'SERVICE', TIME '10:00:00', TIME '11:00:00', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('40000000-0000-0000-0000-000000000012'::uuid, '10000000-0000-0000-0000-000000000006'::uuid, 'TEST_DRIVE', TIME '15:00:00', TIME '16:00:00', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

INSERT INTO "CAR" (vin_id, car_version_id, dealership_id, engine_number, owner_user_id, status, producted_year, created_at)
VALUES
    ('TAYOTA00000000008', '22000000-0000-0000-0000-000000000009'::uuid, '10000000-0000-0000-0000-000000000001'::uuid, 'ENG-TAYOTA-0008', '00000000-0000-0000-0000-000000000015'::uuid, 'SOLD', TIMESTAMP '2026-01-01 00:00:00', CURRENT_TIMESTAMP)
ON CONFLICT (vin_id) DO NOTHING;

INSERT INTO "GUEST_INFORMATION" (id, full_name, email, phone)
VALUES
    ('50000000-0000-0000-0000-000000000001'::uuid, 'Nguyen Van Guest', 'guest.customer@example.com', '0911000001'),
    ('50000000-0000-0000-0000-000000000002'::uuid, 'Tran Mai Guest', 'guest.service@example.com', '0911000002')
ON CONFLICT (id) DO NOTHING;

INSERT INTO "APPOINTMENT" (id, user_id, car_version_id, vin_id, dealership_id, mechanic_id, guest_information_id, type, status, scheduled_start_at, scheduled_end_at, notes, confirmed_at, completed_at, canceled_at, expired_at, cancel_reason, created_at, updated_at)
VALUES
    ('51000000-0000-0000-0000-000000000001'::uuid, '00000000-0000-0000-0000-000000000015'::uuid, '22000000-0000-0000-0000-000000000006'::uuid, NULL, '10000000-0000-0000-0000-000000000001'::uuid, NULL, NULL, 'TEST_DRIVE', 'COMPLETED', TIMESTAMP '2026-05-10 02:00:00', TIMESTAMP '2026-05-10 03:00:00', 'Customer compared hybrid models.', TIMESTAMP '2026-05-08 03:00:00', TIMESTAMP '2026-05-10 03:00:00', NULL, NULL, NULL, TIMESTAMP '2026-05-07 03:00:00', TIMESTAMP '2026-05-10 03:00:00'),
    ('51000000-0000-0000-0000-000000000002'::uuid, '00000000-0000-0000-0000-000000000015'::uuid, NULL, 'TAYOTA00000000008', '10000000-0000-0000-0000-000000000001'::uuid, '00000000-0000-0000-0000-000000000014'::uuid, NULL, 'SERVICE', 'COMPLETED', TIMESTAMP '2026-05-18 03:00:00', TIMESTAMP '2026-05-18 05:00:00', 'Periodic maintenance at 10,000 km.', TIMESTAMP '2026-05-17 02:00:00', TIMESTAMP '2026-05-18 05:00:00', NULL, NULL, NULL, TIMESTAMP '2026-05-16 02:00:00', TIMESTAMP '2026-05-18 05:00:00'),
    ('51000000-0000-0000-0000-000000000003'::uuid, '00000000-0000-0000-0000-000000000015'::uuid, '22000000-0000-0000-0000-000000000004'::uuid, NULL, '10000000-0000-0000-0000-000000000002'::uuid, NULL, NULL, 'TEST_DRIVE', 'CONFIRMED', TIMESTAMP '2026-06-05 02:30:00', TIMESTAMP '2026-06-05 03:30:00', 'Customer wants to test Camry Hybrid.', TIMESTAMP '2026-05-24 03:00:00', NULL, NULL, NULL, NULL, TIMESTAMP '2026-05-24 02:00:00', TIMESTAMP '2026-05-24 03:00:00'),
    ('51000000-0000-0000-0000-000000000004'::uuid, NULL, '22000000-0000-0000-0000-000000000005'::uuid, NULL, '10000000-0000-0000-0000-000000000004'::uuid, NULL, '50000000-0000-0000-0000-000000000001'::uuid, 'TEST_DRIVE', 'PENDING', TIMESTAMP '2026-06-10 02:00:00', TIMESTAMP '2026-06-10 03:00:00', 'Guest requested seven-seat SUV.', NULL, NULL, NULL, NULL, NULL, TIMESTAMP '2026-05-25 04:00:00', TIMESTAMP '2026-05-25 04:00:00')
ON CONFLICT (id) DO NOTHING;

INSERT INTO "SERVICE" (id, user_id, guest_information_id, vin_id, mechanic_id, dealership_id, appointment_id, mileage_at_service, status, total_amount, vehicle_condition, notes, receiving_at, processing_at, completed_at, canceled_at, expired_at, cancel_reason, created_at, updated_at)
VALUES
    ('52000000-0000-0000-0000-000000000001'::uuid, '00000000-0000-0000-0000-000000000015'::uuid, NULL, 'TAYOTA00000000008', '00000000-0000-0000-0000-000000000014'::uuid, '10000000-0000-0000-0000-000000000001'::uuid, '51000000-0000-0000-0000-000000000002'::uuid, 10125, 'COMPLETED', 1950000.00, 'Vehicle in good condition, normal tire wear.', 'Changed oil and inspected brake system.', TIMESTAMP '2026-05-18 03:00:00', TIMESTAMP '2026-05-18 03:15:00', TIMESTAMP '2026-05-18 05:00:00', NULL, NULL, NULL, TIMESTAMP '2026-05-18 03:00:00', TIMESTAMP '2026-05-18 05:00:00')
ON CONFLICT (id) DO NOTHING;

INSERT INTO "SERVICE_ITEM" (id, service_id, item_type, accessory_id, item_name, quantity, unit_price, billing_type, final_price, note, created_at)
VALUES
    ('53000000-0000-0000-0000-000000000001'::uuid, '52000000-0000-0000-0000-000000000001'::uuid, 'LABOR', NULL, 'Periodic maintenance labor', 1, 450000.00, 'NORMAL', 450000.00, '10,000 km inspection.', TIMESTAMP '2026-05-18 03:20:00'),
    ('53000000-0000-0000-0000-000000000002'::uuid, '52000000-0000-0000-0000-000000000001'::uuid, 'PART', NULL, 'Engine oil and filter package', 1, 1500000.00, 'NORMAL', 1500000.00, 'OEM consumables.', TIMESTAMP '2026-05-18 03:20:00')
ON CONFLICT (id) DO NOTHING;

INSERT INTO "CHAT_SESSION" (id, user_id, guest_id, assigned_staff_id, status, closed_at, resolved_at, created_at, updated_at)
VALUES
    ('54000000-0000-0000-0000-000000000001'::uuid, '00000000-0000-0000-0000-000000000015'::uuid, NULL, '00000000-0000-0000-0000-000000000013'::uuid, 'RESOLVED', NULL, TIMESTAMP '2026-05-20 05:25:00', TIMESTAMP '2026-05-20 05:00:00', TIMESTAMP '2026-05-20 05:25:00'),
    ('54000000-0000-0000-0000-000000000002'::uuid, NULL, 'guest-web-0001', '00000000-0000-0000-0000-000000000013'::uuid, 'CHATTING', NULL, NULL, TIMESTAMP '2026-05-25 05:00:00', TIMESTAMP '2026-05-25 05:05:00')
ON CONFLICT (id) DO NOTHING;

INSERT INTO "CHAT_MESSAGE" (id, session_id, sender_id, sender_type, content, created_at)
VALUES
    ('55000000-0000-0000-0000-000000000001'::uuid, '54000000-0000-0000-0000-000000000001'::uuid, '00000000-0000-0000-0000-000000000015'::uuid, 'CUSTOMER', 'When should I bring the car for periodic maintenance?', TIMESTAMP '2026-05-20 05:00:00'),
    ('55000000-0000-0000-0000-000000000002'::uuid, '54000000-0000-0000-0000-000000000001'::uuid, '00000000-0000-0000-0000-000000000013'::uuid, 'ASSISTANT', 'Your maintenance appointment is confirmed for May 18.', TIMESTAMP '2026-05-20 05:10:00'),
    ('55000000-0000-0000-0000-000000000003'::uuid, '54000000-0000-0000-0000-000000000002'::uuid, NULL, 'CUSTOMER', 'I would like a quotation for Fortuner.', TIMESTAMP '2026-05-25 05:00:00'),
    ('55000000-0000-0000-0000-000000000004'::uuid, '54000000-0000-0000-0000-000000000002'::uuid, '00000000-0000-0000-0000-000000000013'::uuid, 'ASSISTANT', 'I can arrange a test drive and send the quotation.', TIMESTAMP '2026-05-25 05:05:00')
ON CONFLICT (id) DO NOTHING;

INSERT INTO "NOTIFICATION" (id, user_id, sender_id, type, title, content, is_read, read_at, created_at)
VALUES
    ('56000000-0000-0000-0000-000000000001'::uuid, '00000000-0000-0000-0000-000000000015'::uuid, NULL, 'APPOINTMENT', 'Test drive confirmed', 'Your Camry Hybrid test drive appointment has been confirmed.', FALSE, NULL, TIMESTAMP '2026-05-24 03:00:00'),
    ('56000000-0000-0000-0000-000000000002'::uuid, '00000000-0000-0000-0000-000000000015'::uuid, '00000000-0000-0000-0000-000000000014'::uuid, 'SERVICE', 'Service completed', 'Your vehicle is ready for collection after periodic maintenance.', TRUE, TIMESTAMP '2026-05-18 06:00:00', TIMESTAMP '2026-05-18 05:00:00'),
    ('56000000-0000-0000-0000-000000000003'::uuid, '00000000-0000-0000-0000-000000000014'::uuid, NULL, 'SERVICE', 'New assigned job', 'A maintenance service ticket was assigned to you.', TRUE, TIMESTAMP '2026-05-18 03:05:00', TIMESTAMP '2026-05-18 03:00:00')
ON CONFLICT (id) DO NOTHING;

INSERT INTO "CUSTOMER_REVIEW" (id, review_type, status, review_token, token_expires_at, submitted_at, appointment_id, service_id, user_id, dealership_id, service_rating, service_comment, mechanic_id, mechanic_rating, mechanic_comment, created_at)
VALUES
    ('57000000-0000-0000-0000-000000000001'::uuid, 'TEST_DRIVE', 'SUBMITTED', 'demo-review-test-drive-0001', TIMESTAMP '2026-06-10 03:00:00', TIMESTAMP '2026-05-10 05:00:00', '51000000-0000-0000-0000-000000000001'::uuid, NULL, '00000000-0000-0000-0000-000000000015'::uuid, '10000000-0000-0000-0000-000000000001'::uuid, 5, 'Comfortable test drive experience.', NULL, NULL, NULL, TIMESTAMP '2026-05-10 04:00:00'),
    ('57000000-0000-0000-0000-000000000002'::uuid, 'SERVICE', 'SUBMITTED', 'demo-review-service-0001', TIMESTAMP '2026-06-18 05:00:00', TIMESTAMP '2026-05-18 07:00:00', NULL, '52000000-0000-0000-0000-000000000001'::uuid, '00000000-0000-0000-0000-000000000015'::uuid, '10000000-0000-0000-0000-000000000001'::uuid, 5, 'Quick and transparent maintenance service.', '00000000-0000-0000-0000-000000000014'::uuid, 5, 'Mechanic explained every completed item.', TIMESTAMP '2026-05-18 06:00:00')
ON CONFLICT (id) DO NOTHING;

INSERT INTO "APPOINTMENT_HOLIDAY" (id, dealership_id, holiday_date, reason, is_active, created_at, updated_at)
VALUES
    ('58000000-0000-0000-0000-000000000001'::uuid, '10000000-0000-0000-0000-000000000001'::uuid, DATE '2026-09-02', 'National holiday', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('58000000-0000-0000-0000-000000000002'::uuid, '10000000-0000-0000-0000-000000000002'::uuid, DATE '2026-09-02', 'National holiday', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;
