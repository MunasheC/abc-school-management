package com.bank.schoolmanagement.dto;

import com.bank.schoolmanagement.entity.Guardian;
import lombok.Data;

@Data
public class GuardianResponse {
    private Long id;
    private String fullName;
    private String primaryPhone;
    private String secondaryPhone;
    private String email;
    private String address;
    private String occupation;
    private String employer;
    private Boolean active;
    private String bankAccountNumber;
    private String bankName;
    private String bankBranch;

    public static GuardianResponse fromEntity(Guardian g) {
        GuardianResponse dto = new GuardianResponse();
        dto.setId(g.getId());
        dto.setFullName(g.getFullName());
        dto.setPrimaryPhone(g.getPrimaryPhone());
        dto.setSecondaryPhone(g.getSecondaryPhone());
        dto.setEmail(g.getEmail());
        dto.setAddress(g.getAddress());
        dto.setOccupation(g.getOccupation());
        dto.setEmployer(g.getEmployer());
        dto.setActive(g.getIsActive());
        dto.setBankAccountNumber(g.getBankAccountNumber());
        dto.setBankName(g.getBankName());
        dto.setBankBranch(g.getBankBranch());
        return dto;
    }
}
