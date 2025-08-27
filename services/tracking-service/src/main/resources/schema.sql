-- services/tracking-service/src/main/resources/schema.sql

-- H2 compatible schema for tracking service

-- Drop tables if they exist (for clean restart)
DROP TABLE IF EXISTS tracking_events;
DROP TABLE IF EXISTS delivery_tracking;
DROP TABLE IF EXISTS delivery_locations;
DROP TABLE IF EXISTS driver_locations;

-- Create tracking events table
CREATE TABLE tracking_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_number VARCHAR(50) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    event_description TEXT,
    driver_id VARCHAR(50),
    latitude DOUBLE,
    longitude DOUBLE,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    metadata TEXT
);

-- Create delivery tracking table
CREATE TABLE delivery_tracking (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_number VARCHAR(50) NOT NULL UNIQUE,
    client_id VARCHAR(50) NOT NULL,
    current_status VARCHAR(50) NOT NULL,
    assigned_driver_id VARCHAR(50),
    estimated_delivery_time TIMESTAMP,
    last_known_latitude DOUBLE,
    last_known_longitude DOUBLE,
    last_location_update TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create delivery locations table
CREATE TABLE delivery_locations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    delivery_id VARCHAR(50) NOT NULL,
    latitude DOUBLE NOT NULL,
    longitude DOUBLE NOT NULL,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    address TEXT,
    location_type VARCHAR(20) NOT NULL DEFAULT 'WAYPOINT'
);

-- Create driver locations table
CREATE TABLE driver_locations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    driver_id VARCHAR(50) NOT NULL,
    latitude DOUBLE NOT NULL,
    longitude DOUBLE NOT NULL,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    speed DOUBLE DEFAULT 0,
    heading DOUBLE DEFAULT 0,
    accuracy DOUBLE DEFAULT 0
);

-- Create indexes for tracking_events
CREATE INDEX idx_tracking_order_number ON tracking_events(order_number);
CREATE INDEX idx_tracking_driver_id ON tracking_events(driver_id);
CREATE INDEX idx_tracking_timestamp ON tracking_events(timestamp);
CREATE INDEX idx_tracking_event_type ON tracking_events(event_type);

-- Create indexes for delivery_tracking
CREATE INDEX idx_delivery_tracking_order_number ON delivery_tracking(order_number);
CREATE INDEX idx_delivery_tracking_client_id ON delivery_tracking(client_id);
CREATE INDEX idx_delivery_tracking_driver_id ON delivery_tracking(assigned_driver_id);
CREATE INDEX idx_delivery_tracking_status ON delivery_tracking(current_status);
CREATE INDEX idx_delivery_tracking_updated_at ON delivery_tracking(updated_at);

-- Create indexes for delivery_locations
CREATE INDEX idx_delivery_locations_delivery_id ON delivery_locations(delivery_id);
CREATE INDEX idx_delivery_locations_timestamp ON delivery_locations(timestamp);

-- Create indexes for driver_locations
CREATE INDEX idx_driver_locations_driver_id ON driver_locations(driver_id);
CREATE INDEX idx_driver_locations_timestamp ON driver_locations(timestamp);