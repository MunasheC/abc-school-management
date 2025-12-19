package com.bank.schoolmanagement.dto;

import com.bank.schoolmanagement.entity.School;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class SchoolDetailsResponse {
    private SchoolResponse school;
    private long totalStudents;
    private long activeStudents;
    private Map<String, Long> studentsByGrade;
    private BigDecimal totalFeesAssigned;
    private BigDecimal totalOutstanding;
    private BigDecimal totalPaymentsReceived;
    private long totalPayments;
    private Map<String, Long> paymentsByMethod;
    private long bankChannelPayments;
    private long schoolChannelPayments;
    private List<PaymentResponse> recentPayments;

    public static SchoolDetailsResponse fromSchool(School school) {
        SchoolDetailsResponse d = new SchoolDetailsResponse();
        d.setSchool(SchoolResponse.fromEntity(school));
        return d;
    }
}
