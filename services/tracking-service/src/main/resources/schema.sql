-- services/tracking-service/src/main/resources/schema.sql

-- Tracking Events Table
CREATE TABLE IF NOT EXISTS tracking_events (
                                               id BIGSERIAL PRIMARY KEY,
                                               order_number VARCHAR(50) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    event_description TEXT,
    driver_id VARCHAR(50),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    metadata TEXT,
    INDEX idx_order_number (order_number),
    INDEX idx_driver_id (driver_id),
    INDEX idx_timestamp (timestamp),
    INDEX idx_event_type (event_type)
    );

-- Delivery Tracking Table
CREATE TABLE IF NOT EXISTS delivery_tracking (
                                                 id BIGSERIAL PRIMARY KEY,
                                                 order_number VARCHAR(50) NOT NULL UNIQUE,
    client_id VARCHAR(50) NOT NULL,
    current_status VARCHAR(50) NOT NULL,
    assigned_driver_id VARCHAR(50),
    estimated_delivery_time TIMESTAMP,
    last_known_latitude DOUBLE PRECISION,
    last_known_longitude DOUBLE PRECISION,
    last_location_update TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_order_number (order_number),
    INDEX idx_client_id (client_id),
    INDEX idx_driver_id (assigned_driver_id),
    INDEX idx_status (current_status),
    INDEX idx_updated_at (updated_at)
    );

-- Driver Locations Table
CREATE TABLE IF NOT EXISTS driver_locations (
                                                id BIGSERIAL PRIMARY KEY,
                                                driver_id VARCHAR(50) NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    speed DOUBLE PRECISION,
    heading INTEGER,
    accuracy DOUBLE PRECISION,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_driver_id (driver_id),
    INDEX idx_timestamp (timestamp),
    INDEX idx_driver_timestamp (driver_id, timestamp)
    );