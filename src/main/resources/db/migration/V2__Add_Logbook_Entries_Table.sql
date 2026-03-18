-- V2__Add_Logbook_Entries_Table.sql
-- Module 6.3.3: Logbook & Supervision Logic
-- Adds the logbook_entries table to track daily work submissions with temporal constraints

CREATE TABLE logbook_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id UUID NOT NULL REFERENCES students(user_id) ON DELETE CASCADE,
    entry_date DATE NOT NULL,
    content TEXT NOT NULL,
    tags VARCHAR(500),
    submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_stamped BOOLEAN DEFAULT FALSE,
    stamped_at TIMESTAMP,
    supervisor_remarks TEXT,
    UNIQUE(student_id, entry_date),
    CONSTRAINT chk_logbook_entry_date CHECK (entry_date <= CURRENT_DATE)
);

CREATE INDEX idx_logbook_student_date ON logbook_entries(student_id, entry_date DESC);
