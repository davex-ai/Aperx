CREATE TYPE user_role AS ENUM ('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_EMPLOYEE');
CREATE TYPE leave_status AS ENUM ('PENDING', 'APPROVED', 'REJECTED');
CREATE TYPE leave_type AS ENUM ('ANNUAL', 'SICK', 'MATERNITY', 'CASUAL');
CREATE TYPE job_status AS ENUM ('OPEN', 'CLOSED', 'ARCHIVED');
CREATE TYPE application_status AS ENUM ('APPLIED', 'SCREENING', 'INTERVIEW', 'OFFERED', 'REJECTED');
CREATE TYPE payroll_status AS ENUM ('DRAFT', 'PROCESSED', 'PAID');
CREATE TYPE document_type AS ENUM ('PAYSLIP', 'TAX_FORM', 'EMPLOYMENT_LETTER', 'RESUME', 'MEDICAL_CERTIFICATE');

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(150) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role user_role NOT NULL DEFAULT 'ROLE_EMPLOYEE',
    is_active BOOLEAN DEFAULT TRUE,
    must_change_password BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE employees (
    id BIGSERIAL PRIMARY KEY,
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
    title VARCHAR(100) NOT NULL,
    description TEXT NOT NULL,
    department VARCHAR(50) NOT NULL,
    status job_status DEFAULT 'OPEN',
    posted_by BIGINT REFERENCES employees(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE applications (
    id BIGSERIAL PRIMARY KEY,
    job_id BIGINT NOT NULL REFERENCES job_postings(id) ON DELETE CASCADE,
    candidate_name VARCHAR(100) NOT NULL,
    candidate_email VARCHAR(150) NOT NULL,
    resume_url VARCHAR(255) NOT NULL,
    status application_status DEFAULT 'APPLIED',
    applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE payroll_runs (
    id BIGSERIAL PRIMARY KEY,
    period_month INTEGER NOT NULL,
    period_year INTEGER NOT NULL,
    status payroll_status DEFAULT 'DRAFT',
    processed_by BIGINT REFERENCES employees(id),
    processed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (period_month, period_year)
);

CREATE TABLE payslips (
    id BIGSERIAL PRIMARY KEY,
    payroll_run_id BIGINT NOT NULL REFERENCES payroll_runs(id) ON DELETE CASCADE,
    employee_id BIGINT NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    gross_salary DECIMAL(12,2) NOT NULL,
    tax_deduction DECIMAL(12,2) NOT NULL DEFAULT 0,
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

CREATE INDEX idx_employees_manager ON employees(manager_id);
CREATE INDEX idx_leave_requests_employee ON leave_requests(employee_id);
CREATE INDEX idx_leave_requests_status ON leave_requests(status);
CREATE INDEX idx_applications_job ON applications(job_id);
CREATE INDEX idx_payslips_employee ON payslips(employee_id);
