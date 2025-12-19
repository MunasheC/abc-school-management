package com.bank.schoolmanagement.dto;

import com.bank.schoolmanagement.entity.School;
import lombok.Data;

import java.time.LocalDate;

@Data
public class SchoolResponse {
    private Long id;
    private String schoolCode;
    private String schoolName;
    private String schoolType;
    private String primaryPhone;
    private String secondaryPhone;
    private String email;
    private String address;
    private String city;
    private String province;
    private String postalCode;
    private String country;
    private String subscriptionTier;
    private Boolean isActive;
    private LocalDate onboardingDate;
    private String relationshipManager;
    private String relationshipManagerPhone;

    public static SchoolResponse fromEntity(School s) {
        if (s == null) return null;
        SchoolResponse dto = new SchoolResponse();
        dto.setId(s.getId());
        dto.setSchoolCode(s.getSchoolCode());
        dto.setSchoolName(s.getSchoolName());
        dto.setSchoolType(s.getSchoolType());
        dto.setPrimaryPhone(s.getPrimaryPhone());
        dto.setSecondaryPhone(s.getSecondaryPhone());
        dto.setEmail(s.getEmail());
        dto.setAddress(s.getAddress());
        dto.setCity(s.getCity());
        dto.setProvince(s.getProvince());
        dto.setPostalCode(s.getPostalCode());
        dto.setCountry(s.getCountry());
        dto.setSubscriptionTier(s.getSubscriptionTier());
        dto.setIsActive(s.getIsActive());
        dto.setOnboardingDate(s.getOnboardingDate());
        dto.setRelationshipManager(s.getRelationshipManager());
        dto.setRelationshipManagerPhone(s.getRelationshipManagerPhone());
        return dto;
    }
}
