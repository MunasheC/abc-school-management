package com.bank.schoolmanagement.dto;

import com.bank.schoolmanagement.entity.Student;
import lombok.Data;

@Data
public class StudentResponse {
    private Long id;
    private String studentId;
    private String firstName;
    private String lastName;
    private String gender;
    private String nationalId;
    private String grade;
    private String className;
    private String guardianNationalId;
    private String address;
    private Boolean active;
    private String completionStatus;

    public static StudentResponse fromEntity(Student student) {
        StudentResponse dto = new StudentResponse();
        dto.setId(student.getId());
        dto.setStudentId(student.getStudentId());
        dto.setFirstName(student.getFirstName());
        dto.setLastName(student.getLastName());
        dto.setGender(student.getGender());
        dto.setNationalId(student.getNationalId());
        dto.setGrade(student.getGrade());
        dto.setClassName(student.getClassName());
        // dto.setGuardianNationalId(student.getGuardian().getNationalId());
        if (student.getGuardian() != null) {
            dto.setGuardianNationalId(student.getGuardian().getNationalId());
            dto.setAddress(student.getGuardian().getAddress());
        }
        dto.setActive(student.getIsActive());
        dto.setCompletionStatus(student.getCompletionStatus());

        return dto;
    }
}
