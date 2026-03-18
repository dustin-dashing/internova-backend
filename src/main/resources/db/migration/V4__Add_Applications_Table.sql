-- Module 6.3.6: Application Lifecycle & Ghosting Prevention
-- Adds applications state machine table with anti-spam and audit fields.

CREATE TABLE applications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id UUID NOT NULL REFERENCES students(user_id) ON DELETE CASCADE,
    vacancy_id UUID NOT NULL REFERENCES vacancies(id) ON DELETE CASCADE,
    status VARCHAR(32) NOT NULL DEFAULT 'APPLIED',
    applied_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_application_student_vacancy UNIQUE (student_id, vacancy_id)
);

CREATE INDEX idx_applications_student_status ON applications(student_id, status);
CREATE INDEX idx_applications_vacancy_status ON applications(vacancy_id, status);
