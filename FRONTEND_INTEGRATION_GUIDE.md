# Internova Frontend Integration Guide

> This guide provides everything you need to integrate the Internova backend REST API with your Netlify-hosted frontend.

## base URL

```
Production: https://your-backend-domain.com
Development: http://localhost:8080
```

## Authentication Flow

### 1. User Registration

```javascript
// POST /api/v1/auth/register

// Student Registration
const studentRegistration = {
  email: "student@university.edu",
  password: "securePassword123",
  role: "STUDENT",  // "STUDENT" or "COMPANY_REP"
  studentIdNumber: "STU2024001",
  departmentId: "550e8400-e29b-41d4-a716-446655440000"  // UUID
};

// Company Registration
const companyRegistration = {
  email: "company@company.com",
  password: "securePassword123",
  role: "COMPANY_REP",
  companyName: "Tech Solutions Inc",
  registrationNumber: "REG123456",
  industry: "Technology"
};

// Response: { message: "Registration successful. Please check your status." }
```

### 2. User Login

```javascript
// POST /api/v1/auth/login

const loginData = {
  email: "user@example.com",
  password: "password123"
};

// Automatically sets JWT in HttpOnly cookie "JWT"
// Response: { message: "Login successful" }
```

### 3. Get Current User

```javascript
// GET /api/v1/auth/me
// Headers: Cookie: JWT=<token>

// Response:
{
  email: "student@university.edu",
  role: "STUDENT",
  status: "ACTIVE"
}
```

### 4. Refresh Token

```javascript
// POST /api/v1/auth/refresh
// Headers: Cookie: JWT=<token>

// Refreshes JWT token in cookie
// Response: { message: "Token refreshed successfully" }
```

### 5. Logout

```javascript
// POST /api/v1/auth/logout
// Headers: Cookie: JWT=<token>

// Response: { message: "Logged out successfully" }
```

## Vacancy Management

### Get All Vacancies

```javascript
// GET /api/v1/vacancies?page=0&size=10

// Response:
{
  vacancies: [
    {
      id: "uuid",
      title: "Junior Developer",
      description: "We are looking for...",
      requirements: "3+ years experience",
      location: "New York, NY",
      companyName: "TechCorp",
      industry: "Technology",
      isActive: true,
      createdAt: "2024-03-11T10:30:00"
    }
  ],
  totalPages: 5,
  totalElements: 50
}
```

### Search Vacancies

```javascript
// GET /api/v1/vacancies/search?keyword=developer&page=0&size=10

// Returns matching vacancies with pagination
```

### Get Vacancy Discovery Feed (for Students)

```javascript
// GET /api/v1/vacancies/feed/{departmentId}
// Headers: Cookie: JWT=<token>

// Returns vacancies ranked by partnership type for the student's department
```

### Post New Vacancy (Company Only)

```javascript
// POST /api/v1/vacancies
// Headers: Cookie: JWT=<token>, Content-Type: application/json

const vacancyData = {
  title: "Senior Developer",
  description: "Full-time position...",
  requirements: "5+ years experience, expertise in React",
  location: "San Francisco, CA"
};

// Response:
{
  id: "uuid",
  title: "Senior Developer",
  description: "Full-time position...",
  requirements: "5+ years experience, expertise in React",
  location: "San Francisco, CA",
  companyName: "TechCorp",
  industry: "Technology",
  isActive: true,
  createdAt: "2024-03-11T10:30:00"
}
```

## Application Management

### Apply to Vacancy (Student)

```javascript
// POST /api/v1/applications
// Headers: Cookie: JWT=<token>, Content-Type: application/json

const applicationData = {
  vacancyId: "vacancy-uuid-here"
};

// Response:
{
  id: "app-uuid",
  vacancyTitle: "Junior Developer",
  companyName: "TechCorp",
  status: "APPLIED",
  appliedAt: "2024-03-11T10:30:00",
  updatedAt: "2024-03-11T10:30:00"
}

// Possible Errors:
// 400: "Profile must be at least 60% complete to apply."
// 400: "You already applied to this vacancy."
```

### Get My Applications (Student)

```javascript
// GET /api/v1/applications?page=0&size=10
// Headers: Cookie: JWT=<token>

// Response:
{
  applications: [
    {
      id: "app-uuid",
      vacancyTitle: "Junior Developer",
      companyName: "TechCorp",
      status: "APPLIED",
      appliedAt: "2024-03-11T10:30:00",
      updatedAt: "2024-03-11T10:30:00"
    }
  ],
  totalPages: 2,
  totalElements: 15
}
```

### Update Application Status (Company/Admin)

```javascript
// PATCH /api/v1/applications/{applicationId}/status?status=ACCEPTED
// Headers: Cookie: JWT=<token>

// Possible statuses: APPLIED, PENDING, ACCEPTED, REJECTED, WITHDRAWN

// Response: { message: "Application status updated" }
```

### Withdraw Application (Student)

```javascript
// DELETE /api/v1/applications/{applicationId}
// Headers: Cookie: JWT=<token>

// Note: Cannot withdraw ACCEPTED applications
// Response: { message: "Application withdrawn successfully" }
```

## Logbook Management

### Submit Logbook Entry (Student)

```javascript
// POST /api/v1/logbooks
// Headers: Cookie: JWT=<token>, Content-Type: application/json

const logbookData = {
  entryDate: "2024-03-10",  // ISO date format
  content: "Today I worked on implementing the user authentication module...",
  tags: "authentication,backend,security"
};

// Response:
{
  id: "entry-uuid",
  entryDate: "2024-03-10",
  content: "Today I worked on implementing...",
  tags: "authentication,backend,security",
  submittedAt: "2024-03-11T10:30:00",
  isStamped: false,
  supervisorRemarks: null
}

// Possible Errors:
// 400: "Log already exists for this date."
// 400: "Submission window closed for this date (48-hour rule)."
```

### Get My Logbook Entries (Student)

```javascript
// GET /api/v1/logbooks
// Headers: Cookie: JWT=<token>

// Response:
{
  entries: [
    {
      id: "entry-uuid",
      entryDate: "2024-03-10",
      content: "...",
      tags: "auth,backend,security",
      submittedAt: "2024-03-11T10:30:00",
      isStamped: false,
      supervisorRemarks: null
    }
  ],
  count: 25
}
```

### Get Compliance Status (Student)

```javascript
// GET /api/v1/logbooks/compliance/status
// Headers: Cookie: JWT=<token>

// Response:
{
  complianceStatus: "GREEN",  // GREEN, YELLOW, or RED
  statusColor: "#00cc00",
  message: "You are compliant with logbook requirements"
}
```

### Update Logbook Entry (Student)

```javascript
// PUT /api/v1/logbooks/{entryId}
// Headers: Cookie: JWT=<token>, Content-Type: application/json

const updateData = {
  entryDate: "2024-03-10",
  content: "Updated content...",
  tags: "updated,tags"
};

// Response: Updated LogbookResponse
```

### Delete Logbook Entry (Student)

```javascript
// DELETE /api/v1/logbooks/{entryId}
// Headers: Cookie: JWT=<token>

// Response: { message: "Logbook entry deleted successfully" }
```

## Student Profile

### Get Student Profile

```javascript
// GET /api/v1/students/profile
// Headers: Cookie: JWT=<token>

// Response:
{
  id: "student-uuid",
  email: "student@university.edu",
  studentIdNumber: "STU2024001",
  course: "Computer Science",
  cvUrl: "https://storage.example.com/cv.pdf",
  profileCompletion: 75.5,
  department: "Computer Science Department",
  status: "ACTIVE"
}
```

### Update Student Profile

```javascript
// PUT /api/v1/students/profile
// Headers: Cookie: JWT=<token>, Content-Type: application/json

const updateData = {
  course: "Computer Science",
  cvUrl: "https://storage.example.com/new-cv.pdf"
};

// Response: { message: "Profile updated successfully" }
```

### Get Profile Completion

```javascript
// GET /api/v1/students/profile-completion
// Headers: Cookie: JWT=<token>

// Response:
{
  completionPercentage: 75.5,
  completionStatus: {
    hasBasicInfo: true,
    hasCourse: true,
    hasCv: true,
    hasProfilePicture: false
  },
  message: "You can now apply to vacancies"
}
```

## Company Profile

### Get Company Profile

```javascript
// GET /api/v1/company/profile
// Headers: Cookie: JWT=<token>

// Response:
{
  id: "company-uuid",
  email: "company@company.com",
  companyName: "TechCorp Inc",
  registrationNumber: "REG123456",
  industry: "Technology",
  isVerified: false,
  status: "PENDING_VERIFICATION"
}
```

### Get Company's Vacancies

```javascript
// GET /api/v1/company/vacancies
// Headers: Cookie: JWT=<token>

// Response:
{
  vacancies: [...],
  count: 5
}
```

### Get Verification Status

```javascript
// GET /api/v1/company/verification-status
// Headers: Cookie: JWT=<token>

// Response:
{
  isVerified: false,
  status: "PENDING_VERIFICATION",
  message: "Your company is pending verification"
}
```

## Organizations

### Get All Universities

```javascript
// GET /api/v1/organizations/universities

// Response:
{
  universities: [
    {
      id: "uni-uuid",
      name: "Stanford University",
      location: "Palo Alto, CA",
      website: "https://www.stanford.edu"
    }
  ],
  count: 10
}
```

### Get All Departments

```javascript
// GET /api/v1/organizations/departments?page=0&size=20

// Response:
{
  departments: [
    {
      id: "dept-uuid",
      name: "Computer Science",
      code: "CS",
      description: "Computer Science Department",
      facultyName: "Engineering",
      universityName: "Stanford University"
    }
  ],
  totalPages: 5,
  totalElements: 100
}
```

## Notifications

### Get My Notifications

```javascript
// GET /api/v1/notifications?page=0&size=10
// Headers: Cookie: JWT=<token>

// Response:
{
  notifications: [
    {
      id: "notif-uuid",
      title: "Application Accepted",
      message: "Your application to TechCorp has been accepted!",
      isRead: false,
      createdAt: "2024-03-11T10:30:00"
    }
  ],
  totalPages: 2,
  totalElements: 15,
  unreadCount: 3
}
```

### Get Unread Count

```javascript
// GET /api/v1/notifications/unread/count
// Headers: Cookie: JWT=<token>

// Response: { unreadCount: 3 }
```

### Mark Notification as Read

```javascript
// PATCH /api/v1/notifications/{notificationId}/read
// Headers: Cookie: JWT=<token>

// Response: { message: "Notification marked as read" }
```

### Mark All as Read

```javascript
// PATCH /api/v1/notifications/read-all
// Headers: Cookie: JWT=<token>

// Response: { message: "All notifications marked as read" }
```

### Delete Notification

```javascript
// DELETE /api/v1/notifications/{notificationId}
// Headers: Cookie: JWT=<token>

// Response: { message: "Notification deleted" }
```

## Reports & Analytics

### Get Placement Statistics (Admin)

```javascript
// GET /api/v1/reports/placements/{departmentId}
// Headers: Cookie: JWT=<token>

// Response:
{
  totalStudents: 120,
  placedStudents: 95,
  profileCompletionRate: 78.5
}
```

### Get Logbook Compliance (Admin)

```javascript
// GET /api/v1/reports/compliance/{departmentId}
// Headers: Cookie: JWT=<token>

// Response:
{
  GREEN: 95,
  YELLOW: 15,
  RED: 10
}
```

### Get Dashboard Report (Admin)

```javascript
// GET /api/v1/reports/dashboard/{departmentId}
// Headers: Cookie: JWT=<token>

// Response:
{
  placements: {...},
  compliance: {...},
  vacancies: {...},
  timestamp: 1710159000000
}
```

## Frontend Implementation Examples

### Using Fetch API

```javascript
// Login
async function login(email, password) {
  const response = await fetch('/api/v1/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    credentials: 'include',  // Important: include cookies
    body: JSON.stringify({ email, password })
  });
  return response.json();
}

// Get vacancies
async function getVacancies(page = 0, size = 10) {
  const response = await fetch(`/api/v1/vacancies?page=${page}&size=${size}`, {
    credentials: 'include'
  });
  return response.json();
}

// Apply to vacancy
async function applyVacancy(vacancyId) {
  const response = await fetch('/api/v1/applications', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    credentials: 'include',
    body: JSON.stringify({ vacancyId })
  });
  return response.json();
}
```

### Using Axios

```javascript
import axios from 'axios';

const api = axios.create({
  baseURL: 'http://localhost:8080',  // or your backend URL
  withCredentials: true  // Important: include cookies
});

// Login
async function login(email, password) {
  return api.post('/api/v1/auth/login', { email, password });
}

// Get applications
async function getApplications() {
  return api.get('/api/v1/applications');
}

// Apply to vacancy
async function applyVacancy(vacancyId) {
  return api.post('/api/v1/applications', { vacancyId });
}
```

## Error Handling

All endpoints follow standard HTTP status codes:

```javascript
// Success
200 OK - Successful request
201 Created - Resource created

// Client Errors
400 Bad Request - Invalid input or business logic violation
403 Forbidden - Authorization denied (missing role)
404 Not Found - Resource not found

// Example error response:
{
  error: "Validation error",
  message: "Profile must be at least 60% complete to apply."
}
```

## CORS & Cookies

✅ CORS is properly configured for:
- `https://internova-frontend.netlify.app` (production)
- `http://localhost:3000`, `http://localhost:5173` (development)

✅ Credentials (cookies) are automatically handled when you use:
```javascript
credentials: 'include'  // Fetch
withCredentials: true    // Axios
```

## Environment Variables

For Netlify deployment, ensure your frontend environment is set to:

```
REACT_APP_API_URL=https://your-backend-domain.com
```

Or for development:

```
REACT_APP_API_URL=http://localhost:8080
```

---

**API Status:** ✅ Ready for Integration
**Last Updated:** 2024-03-11
