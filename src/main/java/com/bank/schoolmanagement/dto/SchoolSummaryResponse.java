package com.bank.schoolmanagement.dto;

import com.bank.schoolmanagement.entity.School;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SchoolSummaryResponse {
    private SchoolResponse school;
    private long totalStudents;
    private BigDecimal totalOutstanding;
    private BigDecimal totalPaymentsReceived;
    private LocalDateTime lastPaymentDate;

    public static SchoolSummaryResponse fromEntity(School school) {
        SchoolSummaryResponse r = new SchoolSummaryResponse();
        r.setSchool(SchoolResponse.fromEntity(school));
        return r;
    }
}
