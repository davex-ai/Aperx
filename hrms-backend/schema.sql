CREATE TYPE user_role AS ENUM ('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_EMPLOYEE');
CREATE TYPE leave_status AS ENUM ('PENDING', 'APPROVED', 'REJECTED');
CREATE TYPE leave_type AS ENUM ('ANNUAL', 'SICK', 'MATERNITY', 'CASUAL');
CREATE TYPE job_status AS ENUM ('OPEN', 'CLOSED', 'ARCHIVED');
CREATE TYPE application_status AS ENUM ('APPLIED', 'SCREENING', 'INTERVIEW', 'OFFERED', 'HIRED', 'REJECTED');
CREATE TYPE payroll_status AS ENUM ('DRAFT', 'PROCESSED', 'PAID');
CREATE TYPE document_type AS ENUM ('PAYSLIP', 'TAX_FORM', 'EMPLOYMENT_LETTER', 'RESUME', 'MEDICAL_CERTIFICATE');
CREATE TYPE pay_type AS ENUM ('HOURLY', 'SALARIED');
CREATE TYPE timesheet_status AS ENUM ('OPEN', 'SUBMITTED', 'APPROVED', 'REJECTED');
CREATE TYPE education_level AS ENUM ('HIGH_SCHOOL', 'ASSOCIATE', 'BACHELORS', 'MASTERS', 'DOCTORATE', 'OTHER');
CREATE TYPE report_category AS ENUM ('HARASSMENT', 'DISCRIMINATION', 'SAFETY', 'ETHICS_VIOLATION', 'FINANCIAL_MISCONDUCT', 'OTHER');
CREATE TYPE report_status AS ENUM ('OPEN', 'UNDER_REVIEW', 'RESOLVED', 'DISMISSED');

CREATE TABLE companies (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    slug VARCHAR(63) UNIQUE NOT NULL,
    billing_email VARCHAR(150),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    email VARCHAR(150) UNIQUE,
    personal_email VARCHAR(150) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role user_role NOT NULL DEFAULT 'ROLE_EMPLOYEE',
    is_active BOOLEAN DEFAULT TRUE,
    must_change_password BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE employees (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    user_id BIGINT UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    manager_id BIGINT REFERENCES employees(id) ON DELETE SET NULL,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    phone_number VARCHAR(20),
    department VARCHAR(50),
    job_title VARCHAR(50),
    salary_grade VARCHAR(20),
    hire_date DATE NOT NULL,
    salary_amount DECIMAL(12,2) NOT NULL,
    pay_type pay_type NOT NULL DEFAULT 'SALARIED',
    hourly_rate DECIMAL(10,2),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE bank_accounts (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT UNIQUE NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    bank_name VARCHAR(100) NOT NULL,
    account_holder_name VARCHAR(100) NOT NULL,
    account_number VARCHAR(50) NOT NULL,
    routing_number VARCHAR(50),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE emergency_contacts (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    contact_name VARCHAR(100) NOT NULL,
    relationship VARCHAR(50),
    phone_number VARCHAR(20) NOT NULL,
    email VARCHAR(150)
);

CREATE TABLE leave_requests (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    type leave_type NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    reason TEXT,
    certificate_url VARCHAR(255),
    status leave_status DEFAULT 'PENDING',
    approved_by BIGINT REFERENCES employees(id),
    review_comment TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE leave_balances (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    type leave_type NOT NULL,
    year INTEGER NOT NULL,
    total_days DOUBLE PRECISION NOT NULL,
    used_days DOUBLE PRECISION NOT NULL DEFAULT 0,
    UNIQUE (employee_id, type, year)
);

CREATE TABLE job_postings (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    title VARCHAR(100) NOT NULL,
    description TEXT NOT NULL,
    department VARCHAR(50) NOT NULL,
    status job_status DEFAULT 'OPEN',
    posted_by BIGINT REFERENCES employees(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE job_questions (
    id BIGSERIAL PRIMARY KEY,
    job_posting_id BIGINT NOT NULL REFERENCES job_postings(id) ON DELETE CASCADE,
    question_text VARCHAR(300) NOT NULL,
    is_required BOOLEAN DEFAULT TRUE,
    display_order INTEGER DEFAULT 0
);

CREATE TABLE applications (
    id BIGSERIAL PRIMARY KEY,
    job_id BIGINT NOT NULL REFERENCES job_postings(id) ON DELETE CASCADE,
    candidate_name VARCHAR(100) NOT NULL,
    candidate_email VARCHAR(150) NOT NULL,
    candidate_phone VARCHAR(30),
    resume_file_path VARCHAR(255) NOT NULL,
    resume_file_name VARCHAR(255),
    why_join TEXT,
    availability VARCHAR(100),
    years_of_experience DOUBLE PRECISION,
    highest_education education_level,
    status application_status DEFAULT 'APPLIED',
    applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE application_answers (
    id BIGSERIAL PRIMARY KEY,
    application_id BIGINT NOT NULL REFERENCES applications(id) ON DELETE CASCADE,
    job_question_id BIGINT NOT NULL REFERENCES job_questions(id) ON DELETE CASCADE,
    answer_text TEXT NOT NULL
);

CREATE TABLE anonymous_reports (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    submitted_by_employee_id BIGINT NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    display_handle VARCHAR(50) NOT NULL,
    title VARCHAR(150) NOT NULL,
    body TEXT NOT NULL,
    category report_category NOT NULL,
    status report_status NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE anonymous_report_comments (
    id BIGSERIAL PRIMARY KEY,
    report_id BIGINT NOT NULL REFERENCES anonymous_reports(id) ON DELETE CASCADE,
    commenter_employee_id BIGINT NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    body TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE payroll_runs (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    period_month INTEGER NOT NULL,
    period_year INTEGER NOT NULL,
    status payroll_status DEFAULT 'DRAFT',
    processed_by BIGINT REFERENCES employees(id),
    processed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (company_id, period_month, period_year)
);

CREATE TABLE payslips (
    id BIGSERIAL PRIMARY KEY,
    payroll_run_id BIGINT NOT NULL REFERENCES payroll_runs(id) ON DELETE CASCADE,
    employee_id BIGINT NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    gross_salary DECIMAL(12,2) NOT NULL,
    tax_deduction DECIMAL(12,2) NOT NULL DEFAULT 0,
    social_security_deduction DECIMAL(12,2) NOT NULL DEFAULT 0,
    bonus_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    hours_worked DOUBLE PRECISION,
    other_deductions DECIMAL(12,2) NOT NULL DEFAULT 0,
    net_salary DECIMAL(12,2) NOT NULL,
    pdf_path VARCHAR(255)
);

CREATE TABLE employee_documents (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    type document_type NOT NULL,
    title VARCHAR(150) NOT NULL,
    file_path VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE verification_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE announcements (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    author_id BIGINT NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    title VARCHAR(150) NOT NULL,
    body TEXT NOT NULL,
    is_pinned BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE announcement_comments (
    id BIGSERIAL PRIMARY KEY,
    announcement_id BIGINT NOT NULL REFERENCES announcements(id) ON DELETE CASCADE,
    author_id BIGINT NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    body TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE time_entries (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    clock_in_at TIMESTAMP NOT NULL,
    clock_in_lat DOUBLE PRECISION,
    clock_in_lng DOUBLE PRECISION,
    clock_out_at TIMESTAMP,
    clock_out_lat DOUBLE PRECISION,
    clock_out_lng DOUBLE PRECISION,
    notes VARCHAR(255)
);

CREATE TABLE weekly_timesheets (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    week_start_date DATE NOT NULL,
    total_hours DOUBLE PRECISION NOT NULL DEFAULT 0,
    status timesheet_status NOT NULL DEFAULT 'OPEN',
    submitted_at TIMESTAMP,
    reviewed_by BIGINT REFERENCES employees(id),
    review_comment TEXT,
    reviewed_at TIMESTAMP,
    UNIQUE (employee_id, week_start_date)
);

CREATE INDEX idx_users_company ON users(company_id);
CREATE INDEX idx_employees_company ON employees(company_id);
CREATE INDEX idx_employees_manager ON employees(manager_id);
CREATE INDEX idx_job_postings_company ON job_postings(company_id);
CREATE INDEX idx_payroll_runs_company ON payroll_runs(company_id);
CREATE INDEX idx_leave_requests_employee ON leave_requests(employee_id);
CREATE INDEX idx_leave_requests_status ON leave_requests(status);
CREATE INDEX idx_applications_job ON applications(job_id);
CREATE INDEX idx_job_questions_job ON job_questions(job_posting_id);
CREATE INDEX idx_application_answers_application ON application_answers(application_id);
CREATE INDEX idx_anonymous_reports_company ON anonymous_reports(company_id);
CREATE INDEX idx_anonymous_report_comments_report ON anonymous_report_comments(report_id);
CREATE INDEX idx_payslips_employee ON payslips(employee_id);
CREATE INDEX idx_announcements_company ON announcements(company_id);
CREATE INDEX idx_announcement_comments_announcement ON announcement_comments(announcement_id);
CREATE INDEX idx_time_entries_employee ON time_entries(employee_id);
CREATE INDEX idx_weekly_timesheets_employee ON weekly_timesheets(employee_id);
CREATE INDEX idx_weekly_timesheets_status ON weekly_timesheets(status);
