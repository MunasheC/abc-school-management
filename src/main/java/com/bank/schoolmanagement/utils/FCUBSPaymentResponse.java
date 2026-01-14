package com.bank.schoolmanagement.utils;

import lombok.*;

import java.util.ArrayList;

@Getter
@Setter
@NoArgsConstructor
@ToString
@AllArgsConstructor
public class FCUBSPaymentResponse {
    private String response;
    private String message;
    private ArrayList<Value> value;
}
