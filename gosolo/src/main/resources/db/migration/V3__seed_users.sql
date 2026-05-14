CREATE EXTENSION IF NOT EXISTS pgcrypto;

INSERT INTO users (name, email, password, role, manager_id) VALUES
    ('Admin', 'admin@gosolo.pt', crypt('admin123', gen_salt('bf', 10)), 'ADMIN', NULL);

INSERT INTO users (name, email, password, role, manager_id) VALUES
    ('Manager', 'manager@gosolo.pt', crypt('manager123', gen_salt('bf', 10)), 'MANAGER', NULL);

INSERT INTO users (name, email, password, role, manager_id)
SELECT 'Collaborator', 'collab@gosolo.pt', crypt('collab123', gen_salt('bf', 10)), 'COLLABORATOR', id
FROM users WHERE email = 'manager@gosolo.pt';
