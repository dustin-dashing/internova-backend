-- 1. Identity Core (Users)
CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 2. Organizational Hierarchy
CREATE TABLE universities (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    location VARCHAR(255)
);

CREATE TABLE faculties (
    id UUID PRIMARY KEY,
    university_id UUID NOT NULL REFERENCES universities(id),
    name VARCHAR(255) NOT NULL
);

CREATE TABLE departments (
    id UUID PRIMARY KEY,
    faculty_id UUID NOT NULL REFERENCES faculties(id),
    name VARCHAR(255) NOT NULL
);

-- 3. Role-Specific Tables (Joined-Table Inheritance)
CREATE TABLE students (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    department_id UUID NOT NULL REFERENCES departments(id),
    student_id_number VARCHAR(50) NOT NULL UNIQUE,
    course VARCHAR(255),
    cv_url VARCHAR(255),
    profile_completion DOUBLE PRECISION DEFAULT 0.0
);

CREATE TABLE companies (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    company_name VARCHAR(255) NOT NULL,
    registration_number VARCHAR(50) NOT NULL UNIQUE,
    industry VARCHAR(255),
    is_verified BOOLEAN DEFAULT FALSE
);

CREATE TABLE academic_supervisors (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    department_id UUID NOT NULL REFERENCES departments(id),
    staff_id VARCHAR(50) NOT NULL UNIQUE
);

-- 4. Relationship Links
CREATE TABLE supervision_relationships (
    id UUID PRIMARY KEY,
    student_id UUID NOT NULL REFERENCES students(user_id),
    supervisor_id UUID NOT NULL REFERENCES academic_supervisors(user_id),
    academic_year VARCHAR(20),
    semester VARCHAR(20)
);

CREATE TABLE department_partnerships (
    id UUID PRIMARY KEY,
    department_id UUID NOT NULL REFERENCES departments(id),
    company_id UUID NOT NULL REFERENCES companies(user_id),
    partnership_type VARCHAR(50),
    established_at DATE DEFAULT CURRENT_DATE
);
