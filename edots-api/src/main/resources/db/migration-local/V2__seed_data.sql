-- Flyway Migration V2: Seed Data for EDOTS (H2 Local Compatibility)

-- 1. Seed Delivery Agents (Password: agent123)
INSERT INTO delivery_agents (id, name, email, phone, password_hash, active, created_at)
VALUES 
(1, 'Ramesh Kumar', 'agent1@edots.dev', '+919876543210', '$2a$10$w8T0M0e.t7W0aQ7rCflr9uPZ8R1W5vV7C9n9KjQnJvI6H4n7Y8G2G', true, CURRENT_TIMESTAMP),
(2, 'Suresh Patel', 'agent2@edots.dev', '+919876543211', '$2a$10$w8T0M0e.t7W0aQ7rCflr9uPZ8R1W5vV7C9n9KjQnJvI6H4n7Y8G2G', true, CURRENT_TIMESTAMP);

-- 2. Seed Admin User (Password: admin123)
INSERT INTO admin_users (id, name, email, password_hash, role, created_at)
VALUES 
(1, 'System Administrator', 'admin@edots.dev', '$2a$10$w8T0M0e.t7W0aQ7rCflr9uPZ8R1W5vV7C9n9KjQnJvI6H4n7Y8G2G', 'ADMIN', CURRENT_TIMESTAMP);

-- 3. Seed Admin Configuration
INSERT INTO admin_config (id, config_key, config_value, updated_by, updated_at)
VALUES 
(1, 'max_delivery_attempts', '3', 1, CURRENT_TIMESTAMP),
(2, 'stale_threshold_hours', '48', 1, CURRENT_TIMESTAMP);

-- 4. Seed Carrier Config
INSERT INTO carrier_config (id, carrier_id, carrier_name, api_key_hash, stage_mapping, active, created_at)
VALUES 
(1, 'fastship', 'FastShip Logistics', '7f83b1657ff1fc53b92dc18148a1d65dfc2d4b1fa3d677284addd200126d9069', 
 '{"PICKED_UP": "PICKING", "IN_HUB": "PACKED", "IN_TRANSIT": "DISPATCHED", "OUT_FOR_DELIVERY": "OUT_FOR_DELIVERY", "DELIVERED": "DELIVERED", "FAILED_DELIVERY": "FAILED_ATTEMPT", "RETURNED_TO_ORIGIN": "RETURNED"}', 
 true, CURRENT_TIMESTAMP);

-- 5. Seed Notification Rules for all 8 stages
INSERT INTO notification_rules (id, event_type, channels, template_body, active, created_by, created_at, updated_at)
VALUES 
(1, 'ORDER_PLACED', '["EMAIL", "IN_APP"]', 'Your order {{orderReference}} has been confirmed and placed.', true, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 'PICKING', '["IN_APP"]', 'Items for your order {{orderReference}} are currently being picked at our warehouse.', true, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 'PACKED', '["EMAIL", "IN_APP"]', 'Your order {{orderReference}} is packed and ready for dispatch.', true, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(4, 'DISPATCHED', '["EMAIL", "SMS", "IN_APP"]', 'Your order {{orderReference}} has been dispatched with our delivery partner.', true, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(5, 'OUT_FOR_DELIVERY', '["EMAIL", "SMS", "IN_APP"]', 'Your order {{orderReference}} is out for delivery with agent {{agentName}}. Prepare OTP for delivery confirmation.', true, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(6, 'DELIVERED', '["EMAIL", "SMS", "IN_APP"]', 'Your order {{orderReference}} has been successfully delivered. Thank you for shopping with EDOTS!', true, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(7, 'FAILED_ATTEMPT', '["EMAIL", "SMS", "IN_APP"]', 'We attempted to deliver your order {{orderReference}}, but delivery failed (Reason: {{reasonCode}}). Next delivery scheduled for {{nextSlot}}.', true, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(8, 'RETURNED', '["EMAIL", "SMS", "IN_APP"]', 'Your order {{orderReference}} has been returned to the warehouse after exceeding delivery attempts.', true, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 6. Seed Sample Orders
INSERT INTO orders (id, order_reference, customer_name, customer_email, customer_phone, current_stage, assigned_agent_id, attempt_count, max_attempts, created_at, updated_at)
VALUES 
(1, 'ORD-1001', 'Aarav Sharma', 'aarav@example.com', '+919111111111', 'ORDER_PLACED', NULL, 0, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 'ORD-1002', 'Priya Verma', 'priya@example.com', '+919222222222', 'DISPATCHED', 1, 0, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 'ORD-1003', 'Rohan Mehta', 'rohan@example.com', '+919333333333', 'OUT_FOR_DELIVERY', 1, 0, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(4, 'ORD-1004', 'Ananya Gupta', 'ananya@example.com', '+919444444444', 'FAILED_ATTEMPT', 1, 1, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(5, 'ORD-1005', 'Vikram Singh', 'vikram@example.com', '+919555555555', 'DELIVERED', 1, 0, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(6, 'ORD-2026-0001', 'Neha Kapoor', 'neha@example.com', '+919666666666', 'ORDER_PLACED', NULL, 0, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(7, 'ORD-2026-0002', 'Karan Johar', 'karan@example.com', '+919777777777', 'PICKING', NULL, 0, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(8, 'ORD-2026-0003', 'Simran Kaur', 'simran@example.com', '+919888888888', 'PACKED', NULL, 0, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(9, 'ORD-2026-0009', 'Aditya Roy', 'aditya@example.com', '+919999999999', 'DISPATCHED', 2, 0, 3, DATEADD('HOUR', -50, CURRENT_TIMESTAMP), DATEADD('HOUR', -50, CURRENT_TIMESTAMP));

-- 7. Seed Initial Order Events for Sample Orders
INSERT INTO order_events (id, order_id, event_type, previous_stage, new_stage, actor_id, actor_type, created_at)
VALUES 
(1, 1, 'ORDER_PLACED', NULL, 'ORDER_PLACED', 'SYSTEM', 'SYSTEM', CURRENT_TIMESTAMP),
(2, 2, 'ORDER_PLACED', NULL, 'ORDER_PLACED', 'SYSTEM', 'SYSTEM', CURRENT_TIMESTAMP),
(3, 2, 'DISPATCHED', 'ORDER_PLACED', 'DISPATCHED', 'warehouse-dispatcher', 'SYSTEM', CURRENT_TIMESTAMP),
(4, 3, 'ORDER_PLACED', NULL, 'ORDER_PLACED', 'SYSTEM', 'SYSTEM', CURRENT_TIMESTAMP),
(5, 3, 'DISPATCHED', 'ORDER_PLACED', 'DISPATCHED', 'warehouse-dispatcher', 'SYSTEM', CURRENT_TIMESTAMP),
(6, 3, 'OUT_FOR_DELIVERY', 'DISPATCHED', 'OUT_FOR_DELIVERY', 'agent1@edots.dev', 'AGENT', CURRENT_TIMESTAMP),
(7, 4, 'OUT_FOR_DELIVERY', 'DISPATCHED', 'OUT_FOR_DELIVERY', 'agent1@edots.dev', 'AGENT', CURRENT_TIMESTAMP),
(8, 4, 'FAILED_ATTEMPT', 'OUT_FOR_DELIVERY', 'FAILED_ATTEMPT', 'agent1@edots.dev', 'AGENT', CURRENT_TIMESTAMP),
(9, 5, 'OUT_FOR_DELIVERY', 'DISPATCHED', 'OUT_FOR_DELIVERY', 'agent1@edots.dev', 'AGENT', CURRENT_TIMESTAMP),
(10, 5, 'DELIVERED', 'OUT_FOR_DELIVERY', 'DELIVERED', 'agent1@edots.dev', 'AGENT', CURRENT_TIMESTAMP),
(11, 9, 'DISPATCHED', 'PACKED', 'DISPATCHED', 'warehouse-dispatcher', 'SYSTEM', DATEADD('HOUR', -50, CURRENT_TIMESTAMP));

-- 8. Advance Auto-increment sequence pointers in H2
ALTER TABLE delivery_agents ALTER COLUMN id RESTART WITH 20;
ALTER TABLE admin_users ALTER COLUMN id RESTART WITH 20;
ALTER TABLE admin_config ALTER COLUMN id RESTART WITH 20;
ALTER TABLE carrier_config ALTER COLUMN id RESTART WITH 20;
ALTER TABLE notification_rules ALTER COLUMN id RESTART WITH 20;
ALTER TABLE orders ALTER COLUMN id RESTART WITH 100;
ALTER TABLE order_events ALTER COLUMN id RESTART WITH 100;
