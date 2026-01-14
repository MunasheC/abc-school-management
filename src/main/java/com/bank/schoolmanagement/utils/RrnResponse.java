package com.bank.schoolmanagement.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RrnResponse {
    private String response;
    private String message;
    private Value value;
}
