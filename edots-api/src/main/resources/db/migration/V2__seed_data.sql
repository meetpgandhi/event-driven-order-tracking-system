-- Flyway Migration V2: Seed Data for EDOTS

-- 1. Seed Delivery Agents (Password: agent123 -> $2a$10$w8T0M0e.t7W0aQ7rCflr9uPZ8R1W5vV7C9n9KjQnJvI6H4n7Y8G2G)
INSERT INTO delivery_agents (id, name, email, phone, password_hash, active)
VALUES 
(1, 'Ramesh Kumar', 'agent1@edots.dev', '+919876543210', '$2a$10$w8T0M0e.t7W0aQ7rCflr9uPZ8R1W5vV7C9n9KjQnJvI6H4n7Y8G2G', true),
(2, 'Suresh Patel', 'agent2@edots.dev', '+919876543211', '$2a$10$w8T0M0e.t7W0aQ7rCflr9uPZ8R1W5vV7C9n9KjQnJvI6H4n7Y8G2G', true)
ON CONFLICT (email) DO NOTHING;

-- 2. Seed Admin User (Password: admin123 -> $2a$10$w8T0M0e.t7W0aQ7rCflr9uPZ8R1W5vV7C9n9KjQnJvI6H4n7Y8G2G)
INSERT INTO admin_users (id, name, email, password_hash, role)
VALUES 
(1, 'System Administrator', 'admin@edots.dev', '$2a$10$w8T0M0e.t7W0aQ7rCflr9uPZ8R1W5vV7C9n9KjQnJvI6H4n7Y8G2G', 'ADMIN')
ON CONFLICT (email) DO NOTHING;

-- Reset sequences
SELECT setval('delivery_agents_id_seq', (SELECT COALESCE(MAX(id), 1) FROM delivery_agents));
SELECT setval('admin_users_id_seq', (SELECT COALESCE(MAX(id), 1) FROM admin_users));

-- 3. Seed Admin Configuration
INSERT INTO admin_config (config_key, config_value, updated_by)
VALUES 
('max_delivery_attempts', '3', 1),
('stale_threshold_hours', '48', 1)
ON CONFLICT (config_key) DO UPDATE SET config_value = EXCLUDED.config_value;

-- 4. Seed Carrier Config (Carrier: FastShip, API Key: fastship-api-key-2026 -> SHA256: 7f83b1657ff1fc53b92dc18148a1d65dfc2d4b1fa3d677284addd200126d9069)
INSERT INTO carrier_config (carrier_id, carrier_name, api_key_hash, stage_mapping, active)
VALUES 
('fastship', 'FastShip Logistics', '7f83b1657ff1fc53b92dc18148a1d65dfc2d4b1fa3d677284addd200126d9069', 
 '{"PICKED_UP": "PICKING", "IN_HUB": "PACKED", "IN_TRANSIT": "DISPATCHED", "OUT_FOR_DELIVERY": "OUT_FOR_DELIVERY", "DELIVERED": "DELIVERED", "FAILED_DELIVERY": "FAILED_ATTEMPT", "RETURNED_TO_ORIGIN": "RETURNED"}'::jsonb, 
 true)
ON CONFLICT (carrier_id) DO NOTHING;

-- 5. Seed Notification Rules for all 8 stages
INSERT INTO notification_rules (event_type, channels, template_body, active, created_by)
VALUES 
('ORDER_PLACED', '["EMAIL", "IN_APP"]'::jsonb, 'Your order {{orderReference}} has been confirmed and placed.', true, 1),
('PICKING', '["IN_APP"]'::jsonb, 'Items for your order {{orderReference}} are currently being picked at our warehouse.', true, 1),
('PACKED', '["EMAIL", "IN_APP"]'::jsonb, 'Your order {{orderReference}} is packed and ready for dispatch.', true, 1),
('DISPATCHED', '["EMAIL", "SMS", "IN_APP"]'::jsonb, 'Your order {{orderReference}} has been dispatched with our delivery partner.', true, 1),
('OUT_FOR_DELIVERY', '["EMAIL", "SMS", "IN_APP"]'::jsonb, 'Your order {{orderReference}} is out for delivery with agent {{agentName}}. Prepare OTP for delivery confirmation.', true, 1),
('DELIVERED', '["EMAIL", "SMS", "IN_APP"]'::jsonb, 'Your order {{orderReference}} has been successfully delivered. Thank you for shopping with EDOTS!', true, 1),
('FAILED_ATTEMPT', '["EMAIL", "SMS", "IN_APP"]'::jsonb, 'We attempted to deliver your order {{orderReference}}, but delivery failed (Reason: {{reasonCode}}). Next delivery scheduled for {{nextSlot}}.', true, 1),
('RETURNED', '["EMAIL", "SMS", "IN_APP"]'::jsonb, 'Your order {{orderReference}} has been returned to the warehouse after exceeding delivery attempts.', true, 1);

-- 6. Seed Sample Orders across different lifecycle stages
INSERT INTO orders (id, order_reference, customer_name, customer_email, customer_phone, current_stage, assigned_agent_id, attempt_count, max_attempts, created_at, updated_at)
VALUES 
(1, 'ORD-2026-0001', 'Aarav Sharma', 'aarav@example.com', '+919111111111', 'ORDER_PLACED', NULL, 0, 3, NOW() - INTERVAL '2 hours', NOW() - INTERVAL '2 hours'),
(2, 'ORD-2026-0002', 'Priya Verma', 'priya@example.com', '+919222222222', 'PICKING', NULL, 0, 3, NOW() - INTERVAL '4 hours', NOW() - INTERVAL '1 hour'),
(3, 'ORD-2026-0003', 'Rohan Mehta', 'rohan@example.com', '+919333333333', 'PACKED', NULL, 0, 3, NOW() - INTERVAL '6 hours', NOW() - INTERVAL '3 hours'),
(4, 'ORD-2026-0004', 'Ananya Gupta', 'ananya@example.com', '+919444444444', 'DISPATCHED', 1, 0, 3, NOW() - INTERVAL '8 hours', NOW() - INTERVAL '4 hours'),
(5, 'ORD-2026-0005', 'Vikram Singh', 'vikram@example.com', '+919555555555', 'OUT_FOR_DELIVERY', 1, 0, 3, NOW() - INTERVAL '10 hours', NOW() - INTERVAL '30 minutes'),
(6, 'ORD-2026-0006', 'Neha Kapoor', 'neha@example.com', '+919666666666', 'DELIVERED', 1, 0, 3, NOW() - INTERVAL '1 day', NOW() - INTERVAL '6 hours'),
(7, 'ORD-2026-0007', 'Karan Johar', 'karan@example.com', '+919777777777', 'FAILED_ATTEMPT', 2, 1, 3, NOW() - INTERVAL '1 day', NOW() - INTERVAL '2 hours'),
(8, 'ORD-2026-0008', 'Simran Kaur', 'simran@example.com', '+919888888888', 'RETURNED', 2, 3, 3, NOW() - INTERVAL '3 days', NOW() - INTERVAL '1 day'),
(9, 'ORD-2026-0009', 'Aditya Roy', 'aditya@example.com', '+919999999999', 'DISPATCHED', 2, 0, 3, NOW() - INTERVAL '50 hours', NOW() - INTERVAL '50 hours') -- STALE ORDER (>48h)
ON CONFLICT (order_reference) DO NOTHING;

SELECT setval('orders_id_seq', (SELECT COALESCE(MAX(id), 1) FROM orders));

-- 7. Seed Initial Order Events for Sample Orders
INSERT INTO order_events (order_id, event_type, previous_stage, new_stage, actor_id, actor_type, created_at)
VALUES 
(1, 'ORDER_PLACED', NULL, 'ORDER_PLACED', 'SYSTEM', 'SYSTEM', NOW() - INTERVAL '2 hours'),
(2, 'ORDER_PLACED', NULL, 'ORDER_PLACED', 'SYSTEM', 'SYSTEM', NOW() - INTERVAL '4 hours'),
(2, 'PICKING', 'ORDER_PLACED', 'PICKING', 'warehouse-operator-1', 'SYSTEM', NOW() - INTERVAL '1 hour'),
(3, 'ORDER_PLACED', NULL, 'ORDER_PLACED', 'SYSTEM', 'SYSTEM', NOW() - INTERVAL '6 hours'),
(3, 'PICKING', 'ORDER_PLACED', 'PICKING', 'warehouse-operator-1', 'SYSTEM', NOW() - INTERVAL '4 hours'),
(3, 'PACKED', 'PICKING', 'PACKED', 'warehouse-operator-2', 'SYSTEM', NOW() - INTERVAL '3 hours'),
(4, 'ORDER_PLACED', NULL, 'ORDER_PLACED', 'SYSTEM', 'SYSTEM', NOW() - INTERVAL '8 hours'),
(4, 'PICKING', 'ORDER_PLACED', 'PICKING', 'warehouse-operator-1', 'SYSTEM', NOW() - INTERVAL '6 hours'),
(4, 'PACKED', 'PICKING', 'PACKED', 'warehouse-operator-2', 'SYSTEM', NOW() - INTERVAL '5 hours'),
(4, 'DISPATCHED', 'PACKED', 'DISPATCHED', 'warehouse-dispatcher', 'SYSTEM', NOW() - INTERVAL '4 hours'),
(5, 'ORDER_PLACED', NULL, 'ORDER_PLACED', 'SYSTEM', 'SYSTEM', NOW() - INTERVAL '10 hours'),
(5, 'PICKING', 'ORDER_PLACED', 'PICKING', 'warehouse-operator-1', 'SYSTEM', NOW() - INTERVAL '8 hours'),
(5, 'PACKED', 'PICKING', 'PACKED', 'warehouse-operator-2', 'SYSTEM', NOW() - INTERVAL '6 hours'),
(5, 'DISPATCHED', 'PACKED', 'DISPATCHED', 'warehouse-dispatcher', 'SYSTEM', NOW() - INTERVAL '4 hours'),
(5, 'OUT_FOR_DELIVERY', 'DISPATCHED', 'OUT_FOR_DELIVERY', 'agent1@edots.dev', 'AGENT', NOW() - INTERVAL '30 minutes'),
(6, 'DELIVERED', 'OUT_FOR_DELIVERY', 'DELIVERED', 'agent1@edots.dev', 'AGENT', NOW() - INTERVAL '6 hours'),
(7, 'FAILED_ATTEMPT', 'OUT_FOR_DELIVERY', 'FAILED_ATTEMPT', 'agent2@edots.dev', 'AGENT', NOW() - INTERVAL '2 hours'),
(8, 'RETURNED', 'FAILED_ATTEMPT', 'RETURNED', 'SYSTEM', 'SYSTEM', NOW() - INTERVAL '1 day'),
(9, 'DISPATCHED', 'PACKED', 'DISPATCHED', 'warehouse-dispatcher', 'SYSTEM', NOW() - INTERVAL '50 hours');
