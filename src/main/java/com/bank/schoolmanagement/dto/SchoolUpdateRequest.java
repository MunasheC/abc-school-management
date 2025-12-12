package com.bank.schoolmanagement.dto;

import lombok.Data;

/**
 * DTO for updating school information
 * 
 * This DTO is used for partial updates and doesn't require all fields.
 * Only the fields you want to update need to be included in the request.
 */
@Data
public class SchoolUpdateRequest {
    
    private String schoolName;
    private String schoolType;
    private String address;
    private String city;
    private String province;
    private String postalCode;
    private String country;
    private String primaryPhone;
    private String secondaryPhone;
    private String email;
    private String website;
    private String headTeacherName;
    private String bursarName;
    private String bursarPhone;
    private String bursarEmail;
    private String ministryRegistrationNumber;
    private String zimsecCenterNumber;
    private String bankAccountNumber;
    private String bankBranch;
    private String bankAccountName;
    private String relationshipManager;
    private String relationshipManagerPhone;
    private String subscriptionTier;
    private Boolean isActive;
    private Integer maxStudents;
    private String logoUrl;
    private String primaryColor;
    private String notes;
}
