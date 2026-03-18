package com.internova.modules.company.model;

import com.internova.core.model.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "companies")
@PrimaryKeyJoinColumn(name = "user_id")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Company extends User {

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(name = "registration_number", unique = true, nullable = false)
    private String registrationNumber;

    private String industry;

    @Column(name = "is_verified")
    private Boolean isVerified = false;
}
