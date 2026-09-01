-- Run this once before starting the application.
CREATE DATABASE IF NOT EXISTS resource_booking_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- Optional: dedicated application user
-- CREATE USER 'booking_app'@'%' IDENTIFIED BY 'change_me';
-- GRANT ALL PRIVILEGES ON resource_booking_db.* TO 'booking_app'@'%';
-- FLUSH PRIVILEGES;

USE resource_booking_db;
-- Tables are created automatically by Hibernate (spring.jpa.hibernate.ddl-auto=update)
-- and seed data is inserted by com.example.booking.config.DataSeeder on startup.
