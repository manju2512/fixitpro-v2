-- V2__seed_reference_data.sql
-- Reference data only - required for the app to function, not sample/demo data

INSERT INTO role (name) VALUES ('CUSTOMER'), ('TECHNICIAN'), ('ADMIN');

INSERT INTO service_type (name, description, base_price) VALUES
    ('Electrician', 'Wiring, fixtures, panel and outlet repairs', 499.00),
    ('Plumber', 'Leaks, pipe fitting, fixture installation', 449.00),
    ('Carpenter', 'Furniture repair, fittings, woodwork', 399.00);
