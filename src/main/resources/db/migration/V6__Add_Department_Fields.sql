-- Migration: Add code and description columns to departments table
-- This migration should be added as V6 if not already present in the db/migration folder

ALTER TABLE departments ADD COLUMN code VARCHAR(255);
ALTER TABLE departments ADD COLUMN description TEXT;
