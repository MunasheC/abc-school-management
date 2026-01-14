package com.bank.schoolmanagement.utils;

import lombok.*;

import java.math.BigDecimal;

/**
 * FCUBS (Flexcube) Payment Request DTO
 * 
 * Structure for processing payments through Oracle FLEXCUBE
 * 
 * SOURCE ACCOUNT (Debit):
 * - TXNCCY: Transaction currency (e.g., "USD")
 * - TXNBRN: Transaction branch code (e.g., "120")
 * - TXNACC: Transaction account number - the account to debit (e.g., "L13111000")
 * 
 * DESTINATION ACCOUNT (Credit):
 * - OFFSETCCY: Offset currency (e.g., "USD")
 * - OFFSETBRN: Offset branch code (e.g., "120")
 * - OFFSETACC: Offset account number - the account to credit (e.g., 58520134002019)
 * 
 * Example Request:
 * {
 *   "SOURCE": "MOBILEBANKING",
 *   "USERID": "FLEXSWITCH",
 *   "BRANCH": "120",
 *   "PROD": "ABDS",
 *   "BRN": "120",
 *   "TXNCCY": "USD",
 *   "TXNBRN": "120",
 *   "TXNACC": "L13111000",           // Source account (debit)
 *   "OFFSETCCY": "USD",
 *   "OFFSETBRN": "120",
 *   "OFFSETACC": 58520134002019,     // Destination account (credit)
 *   "TXNAMT": 100000,
 *   "NARRATION": "USD|120|52289804360572_10_840632018707E63"
 * }
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class FCUBSPayment {
    // System identifiers
    private String SOURCE;           // Source system (e.g., "MOBILEBANKING", "COUNTER")
    private String USERID;           // User/system ID (e.g., "FLEXSWITCH")
    private String BRANCH;           // Primary branch code
    private String PROD;             // Product code (e.g., "ABDS")
    private String BRN;              // Branch code
    
    // Source account fields (Debit side - where funds come from)
    private String TXNCCY;           // Transaction currency (e.g., "USD", "ZWL")
    private String TXNBRN;           // Transaction branch code
    private String TXNACC;           // Transaction account - SOURCE ACCOUNT TO DEBIT
    
    // Destination account fields (Credit side - where funds go to)
    private String OFFSETCCY;        // Offset currency
    private String OFFSETBRN;        // Offset branch code
    private Object OFFSETACC;        // Offset account - DESTINATION ACCOUNT TO CREDIT (can be String or Long)
    
    // Transaction details
    private BigDecimal TXNAMT;       // Transaction amount
    private String NARRATION;        // Transaction narration/description
    private String TXNDATE;          // Transaction date
    
    // Additional fields
    private String ACCOUNT_NUMBER;
    private String BENEF_NAME;
    private String BRANCH_CODE;
    private String CUSTOMERS_BANK_NAME;
    private String BRANCH1;
    private String TXNMIS1;
    private String MSGID;
    private String CORRELID;
    private String FCCREF;
}
