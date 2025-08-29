-- Drop tables if they exist (for H2 database)
DROP TABLE IF EXISTS deliveries;
DROP TABLE IF EXISTS drivers;

-- Create drivers table
CREATE TABLE drivers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    driver_id VARCHAR(255) NOT NULL UNIQUE,
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    phone_number VARCHAR(20) NOT NULL,
    license_number VARCHAR(50) NOT NULL UNIQUE,
    vehicle_type VARCHAR(20) NOT NULL,
    vehicle_number VARCHAR(50),
    status VARCHAR(20) NOT NULL,
    password VARCHAR(255) NOT NULL,
    profile_picture VARCHAR(500),
    current_latitude DOUBLE,
    current_longitude DOUBLE,
    last_location_update TIMESTAMP,
    total_deliveries INT DEFAULT 0,
    completed_deliveries INT DEFAULT 0,
    failed_deliveries INT DEFAULT 0,
    rating DOUBLE DEFAULT 0.0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    last_login_at TIMESTAMP,
    active BOOLEAN DEFAULT TRUE,
    verified BOOLEAN DEFAULT FALSE
);

-- Create deliveries table (if needed)
CREATE TABLE deliveries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_number VARCHAR(255) NOT NULL UNIQUE,
    client_id VARCHAR(255) NOT NULL,
    driver_id BIGINT,
    pickup_address VARCHAR(500) NOT NULL,
    delivery_address VARCHAR(500) NOT NULL,
    package_description VARCHAR(1000) NOT NULL,
    priority VARCHAR(20),
    status VARCHAR(20) NOT NULL,
    scheduled_date TIMESTAMP,
    assigned_at TIMESTAMP,
    picked_up_at TIMESTAMP,
    delivered_at TIMESTAMP,
    recipient_name VARCHAR(255),
    recipient_phone VARCHAR(20),
    special_instructions VARCHAR(1000),
    proof_of_delivery_photo VARCHAR(500),
    digital_signature VARCHAR(500),
    delivery_notes VARCHAR(1000),
    failure_reason VARCHAR(1000),
    pickup_latitude DOUBLE,
    pickup_longitude DOUBLE,
    delivery_latitude DOUBLE,
    delivery_longitude DOUBLE,
    route_sequence INT,
    route_id VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    FOREIGN KEY (driver_id) REFERENCES drivers(id)
);