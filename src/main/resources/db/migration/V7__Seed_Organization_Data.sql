-- Migration: Seed initial organization data
-- Includes a university, faculties, and departments to support registration and discovery

-- 1. Insert University
INSERT INTO universities (id, name, location)
VALUES ('7b2e3f4a-8d1c-4b5e-9f0a-1b2c3d4e5f6a', 'Internova University of Technology', 'Nairobi, Kenya');

-- 2. Insert Faculties
INSERT INTO faculties (id, university_id, name)
VALUES 
('a1b2c3d4-e5f6-4a1b-2c3d-e5f6a1b2c3d4', '7b2e3f4a-8d1c-4b5e-9f0a-1b2c3d4e5f6a', 'Faculty of Engineering and Technology'),
('b2c3d4e5-f6a1-4b2c-3d4e-5f6a1b2cd4e5', '7b2e3f4a-8d1c-4b5e-9f0a-1b2c3d4e5f6a', 'Faculty of Business and Management');

-- 3. Insert Departments
INSERT INTO departments (id, faculty_id, name, code, description)
VALUES 
-- Engineering Departments
('c3d4e5f6-a1b2-4c3d-4e5f-6a1b2c3d4e5f', 'a1b2c3d4-e5f6-4a1b-2c3d-e5f6a1b2c3d4', 'Computer Science', 'CS', 'Department of Computer Science and Informatics'),
('d4e5f6a1-b2c3-4d4e-5f6a-1b2c3d4e5f6a', 'a1b2c3d4-e5f6-4a1b-2c3d-e5f6a1b2c3d4', 'Software Engineering', 'SE', 'Department of Software Engineering and Development'),
-- Business Departments
('e5f6a1b2-c3d4-4e5f-6a1b-2c3d4e5f6a1b', 'b2c3d4e5-f6a1-4b2c-3d4e-5f6a1b2cd4e5', 'Business Administration', 'BA', 'Department of Business Administration and Management'),
('f6a1b2c3-d4e5-4f6a-1b2c-3d4e5f6a1b2c', 'b2c3d4e5-f6a1-4b2c-3d4e-5f6a1b2cd4e5', 'Accounting and Finance', 'AF', 'Department of Accounting, Finance and Economics');
