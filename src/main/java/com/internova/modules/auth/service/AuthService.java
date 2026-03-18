package com.internova.modules.auth.service;

import com.internova.core.enums.AccountStatus;
import com.internova.core.model.User;
import com.internova.modules.auth.dto.LoginRequest;
import com.internova.modules.auth.dto.RegisterRequest;
import com.internova.modules.auth.repository.UserRepository;
import com.internova.modules.company.model.Company;
import com.internova.modules.organization.model.Department;
import com.internova.modules.organization.repository.DepartmentRepository;
import com.internova.modules.student.model.Student;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public String register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        User user;
        switch (request.getRole()) {
            case STUDENT -> {
                Student student = new Student();
                student.setStudentIdNumber(request.getStudentIdNumber());
                Department dept = departmentRepository.findById(request.getDepartmentId())
                        .orElseThrow(() -> new RuntimeException("Department not found"));
                student.setDepartment(dept);
                user = student;
            }
            case COMPANY_REP -> {
                Company company = new Company();
                company.setCompanyName(request.getCompanyName());
                company.setRegistrationNumber(request.getRegistrationNumber());
                company.setIndustry(request.getIndustry());
                company.setStatus(AccountStatus.PENDING_VERIFICATION);
                user = company;
            }
            default -> throw new IllegalArgumentException("Invalid role for public registration");
        }

        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());

        userRepository.save(user);
        return "Registration successful. Please check your status.";
    }

    public void login(LoginRequest request, HttpServletResponse response) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        User user = userRepository.findByEmail(request.getEmail()).orElseThrow();
        String jwt = jwtService.generateToken(user);

        setJwtCookie(response, jwt);
    }

    public void refreshToken(User user, HttpServletResponse response) {
        String newJwt = jwtService.generateToken(user);
        setJwtCookie(response, newJwt);
    }

    private void setJwtCookie(HttpServletResponse response, String jwt) {
        Cookie cookie = new Cookie("JWT", jwt);
        cookie.setHttpOnly(true);
        cookie.setSecure(true); // Should be true in production (HTTPS)
        cookie.setPath("/");
        cookie.setMaxAge(86400); // 24 hours
        cookie.setAttribute("SameSite", "None");

        response.addCookie(cookie);
    }
}
