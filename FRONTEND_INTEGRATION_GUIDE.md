# 🚀 Internova Frontend Integration Guide

> Complete API documentation for integrating with the Internova backend. All endpoints, authentication flows, and examples included.

## 📍 Base URLs

```javascript
// Production (Railway)
const API_BASE_URL = 'https://internova-backend-production.up.railway.app';

// Development (Local)
const API_BASE_URL = 'http://localhost:8080';
```

## 🔐 Authentication Flow

### Cookie-Based JWT Authentication
- **JWT Token**: Stored in `HttpOnly` cookie named `JWT`
- **Automatic**: Login/refresh endpoints set the cookie automatically
- **Include in Requests**: Cookie is sent automatically by browser
- **Logout**: Clears the cookie

### 1. User Registration

```javascript
// POST /api/v1/auth/register
// Content-Type: application/json
// No authentication required

// Student Registration
const studentData = {
  email: "student@university.edu",
  password: "SecurePass123!",
  role: "STUDENT",
  studentIdNumber: "STU2024001",
  departmentId: "uuid-of-department"  // Required: Get from departments endpoint
};

// Company Registration
const companyData = {
  email: "hr@company.com",
  password: "SecurePass123!",
  role: "COMPANY_REP",
  companyName: "Tech Solutions Inc",
  registrationNumber: "REG123456",
  industry: "Technology"
};

// Response: { message: "Registration successful" }
// Status: 200 OK

// Error Responses:
// 400: { message: "Email already exists" }
// 400: { message: "Department not found" }
// 500: { message: "Internal server error" }
```

### 2. User Login

```javascript
// POST /api/v1/auth/login
// Content-Type: application/json
// No authentication required

const loginData = {
  email: "student@university.edu",
  password: "SecurePass123!"
};

// Response: { message: "Login successful" }
// Status: 200 OK
// Sets JWT cookie automatically

// Error Responses:
// 401: { message: "Invalid credentials" }
// 403: { message: "Account not active" }
```

### 3. Get Current User Profile

```javascript
// GET /api/v1/auth/me
// Requires: JWT cookie

// Response:
{
  email: "student@university.edu",
  role: "STUDENT",
  status: "ACTIVE"
}

// Status: 200 OK

// Error Responses:
// 401: { message: "Unauthorized" }
```

### 4. Refresh Token

```javascript
// POST /api/v1/auth/refresh
// Requires: JWT cookie

// Response: { message: "Token refreshed successfully" }
// Status: 200 OK
// Updates JWT cookie

// Error Responses:
// 401: { message: "Invalid token" }
```

### 5. Logout

```javascript
// POST /api/v1/auth/logout
// Requires: JWT cookie

// Response: { message: "Logged out successfully" }
// Status: 200 OK
// Clears JWT cookie
```

## 🎯 Public Endpoints (No Authentication Required)

### Get All Vacancies

```javascript
// GET /api/v1/vacancies?page=0&size=10

// Response:
{
  vacancies: [
    {
      id: "uuid",
      title: "Software Developer Intern",
      description: "Great opportunity for CS students...",
      requirements: "Java, Spring Boot knowledge preferred",
      location: "Remote",
      companyName: "TechCorp Inc",
      industry: "Technology",
      isActive: true,
      createdAt: "2024-03-19T10:30:00Z"
    }
  ],
  totalPages: 5,
  totalElements: 47
}

// Query Parameters:
// - page: Page number (default: 0)
// - size: Items per page (default: 10)
```

### Get Single Vacancy

```javascript
// GET /api/v1/vacancies/{vacancyId}

// Response: Single vacancy object (same format as above)
// Status: 200 OK

// Error: 404 { message: "Vacancy not found" }
```

### Search Vacancies

```javascript
// GET /api/v1/vacancies/search?keyword=developer&page=0&size=10

// Same response format as getAllVacancies
// Searches in title, description, and requirements
```

### Health Check

```javascript
// GET /actuator/health

// Response: { status: "UP" }
// Status: 200 OK
```

## 👨‍🎓 Student Endpoints

### Get Student Profile

```javascript
// GET /api/v1/students/profile
// Requires: JWT cookie (STUDENT role)

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
// Requires: JWT cookie (STUDENT role)
// Content-Type: application/json

const updateData = {
  course: "Computer Science",           // Optional
  cvUrl: "https://storage.example.com/new-cv.pdf"  // Optional
};

// Response: { message: "Profile updated successfully" }
// Status: 200 OK
```

### Get Profile Completion Status

```javascript
// GET /api/v1/students/profile-completion
// Requires: JWT cookie (STUDENT role)

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

### Get Vacancy Feed (Personalized)

```javascript
// GET /api/v1/vacancies/feed/{departmentId}
// Requires: JWT cookie (STUDENT role)

{
  vacancies: [
    // Same format as public vacancies
    // But prioritized by department partnerships
  ]
}
```

### Apply to Vacancy

```javascript
// POST /api/v1/applications
// Requires: JWT cookie (STUDENT role)
// Content-Type: application/json

const applicationData = {
  vacancyId: "vacancy-uuid-here"
};

// Response:
{
  id: "application-uuid",
  vacancyTitle: "Software Developer Intern",
  companyName: "TechCorp Inc",
  status: "APPLIED",
  appliedAt: "2024-03-19T10:30:00Z",
  updatedAt: "2024-03-19T10:30:00Z"
}

// Error Responses:
// 400: { message: "Profile must be at least 60% complete to apply" }
// 400: { message: "You already applied to this vacancy" }
// 404: { message: "Vacancy not found" }
```

### Get My Applications

```javascript
// GET /api/v1/applications?page=0&size=10
// Requires: JWT cookie (STUDENT role)

{
  applications: [
    {
      id: "application-uuid",
      vacancyTitle: "Software Developer Intern",
      companyName: "TechCorp Inc",
      status: "APPLIED",  // APPLIED, PENDING, ACCEPTED, REJECTED, WITHDRAWN
      appliedAt: "2024-03-19T10:30:00Z",
      updatedAt: "2024-03-19T10:30:00Z"
    }
  ],
  totalPages: 2,
  totalElements: 15
}
```

### Get Single Application

```javascript
// GET /api/v1/applications/{applicationId}
// Requires: JWT cookie (STUDENT role)

{
  id: "application-uuid",
  vacancyTitle: "Software Developer Intern",
  companyName: "TechCorp Inc",
  status: "APPLIED",
  appliedAt: "2024-03-19T10:30:00Z",
  updatedAt: "2024-03-19T10:30:00Z"
}

// Error: 403 { message: "Unauthorized" } (if not your application)
// Error: 404 { message: "Application not found" }
```

### Withdraw Application

```javascript
// DELETE /api/v1/applications/{applicationId}
// Requires: JWT cookie (STUDENT role)

{
  message: "Application withdrawn successfully"
}

// Error: 400 { message: "Cannot withdraw accepted application" }
```

### Submit Logbook Entry

```javascript
// POST /api/v1/logbooks/submit?date=2024-03-19
// Requires: JWT cookie (STUDENT role)
// Content-Type: application/json

const logbookData = {
  content: "Today I worked on implementing the authentication module. Learned about JWT tokens and Spring Security configuration.",
  tags: "authentication,spring-boot,jwt,security"
};

// Response:
{
  message: "Log submitted successfully",
  id: "logbook-entry-uuid"
}

// Error Responses:
// 400: { message: "Log already exists for this date" }
// 400: { message: "Entry date cannot be in the future" }
// 400: { message: "Submission window closed (48-hour rule)" }
```

### Get My Logbook Entries

```javascript
// GET /api/v1/logbooks/my-logs
// Requires: JWT cookie (STUDENT role)

[
  {
    id: "entry-uuid",
    entryDate: "2024-03-19",
    content: "Worked on authentication module...",
    tags: "authentication,spring-boot,jwt",
    submittedAt: "2024-03-19T15:30:00Z",
    isStamped: false,
    supervisorRemarks: null,
    status: "ON_TIME"  // ON_TIME or LATE
  }
]
```

## 🏢 Company Endpoints

### Get Company Profile

```javascript
// GET /api/v1/company/profile
// Requires: JWT cookie (COMPANY_REP role)

{
  id: "company-uuid",
  email: "hr@company.com",
  companyName: "Tech Solutions Inc",
  registrationNumber: "REG123456",
  industry: "Technology",
  isVerified: false,
  status: "PENDING_VERIFICATION"
}
```

### Get Company Vacancies

```javascript
// GET /api/v1/company/vacancies
// Requires: JWT cookie (COMPANY_REP role)

{
  vacancies: [
    {
      id: "vacancy-uuid",
      title: "Software Developer Intern",
      description: "Great opportunity...",
      requirements: "Java, Spring Boot",
      location: "Remote",
      companyName: "Tech Solutions Inc",
      industry: "Technology",
      isActive: true,
      createdAt: "2024-03-19T10:30:00Z"
    }
  ],
  count: 5
}
```

### Post New Vacancy

```javascript
// POST /api/v1/vacancies
// Requires: JWT cookie (COMPANY_REP role)
// Content-Type: application/json

const vacancyData = {
  title: "Software Developer Intern",
  description: "We are looking for talented CS students to join our development team...",
  requirements: "Knowledge of Java, Spring Boot preferred. Database experience a plus.",
  location: "Remote"  // Optional
};

// Response: Full vacancy object (same as above)
// Status: 200 OK

// Error: 403 { message: "Only companies can post vacancies" }
```

### Update Vacancy

```javascript
// PUT /api/v1/vacancies/{vacancyId}
// Requires: JWT cookie (COMPANY_REP role)
// Content-Type: application/json

const updateData = {
  title: "Updated Title",
  description: "Updated description",
  requirements: "Updated requirements",
  location: "Updated location"
};

// Response: Updated vacancy object
// Error: 403 { message: "Unauthorized" } (if not your vacancy)
```

### Update Application Status

```javascript
// PATCH /api/v1/applications/{applicationId}/status?status=ACCEPTED
// Requires: JWT cookie (COMPANY_REP or ADMIN role)

{
  message: "Application status updated"
}

// Valid statuses: APPLIED, PENDING, ACCEPTED, REJECTED
// Error: 404 { message: "Application not found" }
```

### Get Verification Status

```javascript
// GET /api/v1/company/verification-status
// Requires: JWT cookie (COMPANY_REP role)

{
  isVerified: false,
  status: "PENDING_VERIFICATION",
  message: "Your company is pending verification"
}
```

## 🔔 Notification Endpoints

### Get My Notifications

```javascript
// GET /api/v1/notifications?page=0&size=10
// Requires: JWT cookie (any authenticated user)

{
  notifications: [
    {
      id: "notification-uuid",
      title: "Application Update",
      message: "Your application for Software Developer Intern has been viewed",
      isRead: false,
      createdAt: "2024-03-19T10:30:00Z"
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
// Requires: JWT cookie

{
  unreadCount: 3
}
```

### Mark Notification as Read

```javascript
// PATCH /api/v1/notifications/{notificationId}/read
// Requires: JWT cookie

{
  message: "Notification marked as read"
}
```

### Mark All Notifications as Read

```javascript
// PATCH /api/v1/notifications/read-all
// Requires: JWT cookie

{
  message: "All notifications marked as read"
}
```

## 📊 Error Handling

### Common HTTP Status Codes

- **200**: Success
- **400**: Bad Request (validation error)
- **401**: Unauthorized (invalid/missing JWT)
- **403**: Forbidden (insufficient permissions)
- **404**: Not Found
- **500**: Internal Server Error

### Error Response Format

```javascript
{
  message: "Error description",
  // Additional fields may be present
}
```

## 🔧 JavaScript Integration Examples

### Axios Setup with Cookie Support

```javascript
import axios from 'axios';

const api = axios.create({
  baseURL: 'https://internova-backend-production.up.railway.app',
  withCredentials: true,  // Important: Sends cookies automatically
  headers: {
    'Content-Type': 'application/json'
  }
});

// Login function
export const login = async (email, password) => {
  try {
    const response = await api.post('/api/v1/auth/login', {
      email,
      password
    });
    return response.data;
  } catch (error) {
    throw error.response?.data || error;
  }
};

// Get current user
export const getCurrentUser = async () => {
  try {
    const response = await api.get('/api/v1/auth/me');
    return response.data;
  } catch (error) {
    throw error.response?.data || error;
  }
};

// Get vacancies
export const getVacancies = async (page = 0, size = 10) => {
  try {
    const response = await api.get(`/api/v1/vacancies?page=${page}&size=${size}`);
    return response.data;
  } catch (error) {
    throw error.response?.data || error;
  }
};
```

### React Hook Example

```javascript
import { useState, useEffect } from 'react';
import { api } from './api';  // Your axios instance

export const useAuth = () => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    checkAuthStatus();
  }, []);

  const checkAuthStatus = async () => {
    try {
      const userData = await api.get('/api/v1/auth/me');
      setUser(userData);
    } catch (error) {
      setUser(null);
    } finally {
      setLoading(false);
    }
  };

  const login = async (email, password) => {
    const response = await api.post('/api/v1/auth/login', { email, password });
    await checkAuthStatus();  // Refresh user data
    return response.data;
  };

  const logout = async () => {
    await api.post('/api/v1/auth/logout');
    setUser(null);
  };

  return { user, loading, login, logout };
};
```

## 🚀 Quick Start Checklist

1. ✅ **Set Base URL**: Use `https://internova-backend-production.up.railway.app`
2. ✅ **Configure Axios**: Enable `withCredentials: true`
3. ✅ **Implement Auth**: Login → Get user → Protect routes
4. ✅ **Test Public APIs**: Vacancies, health check
5. ✅ **Add Role-Based UI**: Different features for students vs companies
6. ✅ **Handle Errors**: Show user-friendly error messages
7. ✅ **Implement Loading States**: For all API calls

## 📞 Support

- **API Base URL**: `https://internova-backend-production.up.railway.app`
- **Health Check**: `GET /actuator/health`
- **PgWeb Database**: `https://pgweb-production-ad27.up.railway.app`

---

**Last Updated**: March 19, 2026
**API Version**: v1
**Status**: ✅ Production Ready
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
