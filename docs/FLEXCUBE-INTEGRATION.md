# Flexcube Integration Configuration Guide

## Overview

This document explains the Flexcube (FCUBS) payment integration configuration for the School Management System.

## Configuration Properties

Add these properties to your `application.properties` file:

```properties
# ==========================================
# FLEXCUBE INTEGRATION SETTINGS
# ==========================================

# Source System Identifier
# Values: MOBILEBANKING, COUNTER, INTERNETBANKING, USSD
flexcube.default.source=MOBILEBANKING

# User/System ID for Flexcube authentication
flexcube.default.userid=FLEXSWITCH

# Default branch code
flexcube.default.branch=120

# Product code
flexcube.default.product=ABDS

# Default transaction currency
# Values: USD, ZWL, etc.
flexcube.default.currency=USD

# School collection account (destination account for credits)
# This is the account that receives all school fee payments
flexcube.school.collection.account=58520134002019

# Flexcube API endpoint
flexcube.api.url=https://api.bank.example.com/flexcube/v1/payments

# API timeout (milliseconds)
flexcube.api.timeout=30000

# Enable/disable Flexcube integration
flexcube.enabled=true
```

## Account Structure Explanation

### Source Account (TXNACC) - Debit Side
- **Purpose**: Parent's bank account
- **Action**: Money is **DEBITED** from this account
- **Example**: `"TXNACC": "L13111000"`
- **Fields**:
  - `TXNCCY`: Transaction currency (e.g., "USD")
  - `TXNBRN`: Transaction branch code (e.g., "120")
  - `TXNACC`: Account number to debit

### Destination Account (OFFSETACC) - Credit Side
- **Purpose**: School's collection account
- **Action**: Money is **CREDITED** to this account
- **Example**: `"OFFSETACC": 58520134002019`
- **Fields**:
  - `OFFSETCCY`: Offset currency (e.g., "USD")
  - `OFFSETBRN`: Offset branch code (e.g., "120")
  - `OFFSETACC`: Account number to credit

## Request Structure

### Full Payment Request Example

```json
{
  "SOURCE": "MOBILEBANKING",
  "USERID": "FLEXSWITCH",
  "BRANCH": "120",
  "PROD": "ABDS",
  "BRN": "120",
  
  // SOURCE ACCOUNT (Debit - Parent's account)
  "TXNCCY": "USD",
  "TXNBRN": "120",
  "TXNACC": "L13111000",
  
  // DESTINATION ACCOUNT (Credit - School's account)
  "OFFSETCCY": "USD",
  "OFFSETBRN": "120",
  "OFFSETACC": 58520134002019,
  
  // Transaction details
  "TXNAMT": 100000,
  "NARRATION": "USD|120|52289804360572_10_840632018707E63"
}
```

## Field Descriptions

| Field | Description | Example | Required |
|-------|-------------|---------|----------|
| SOURCE | Source system identifier | "MOBILEBANKING" | Yes |
| USERID | User/system ID | "FLEXSWITCH" | Yes |
| BRANCH | Primary branch code | "120" | Yes |
| PROD | Product code | "ABDS" | Yes |
| BRN | Branch code | "120" | Yes |
| **TXNCCY** | **Transaction currency** | **"USD"** | **Yes** |
| **TXNBRN** | **Transaction branch** | **"120"** | **Yes** |
| **TXNACC** | **Source account (DEBIT)** | **"L13111000"** | **Yes** |
| **OFFSETCCY** | **Offset currency** | **"USD"** | **Yes** |
| **OFFSETBRN** | **Offset branch** | **"120"** | **Yes** |
| **OFFSETACC** | **Destination account (CREDIT)** | **58520134002019** | **Yes** |
| TXNAMT | Transaction amount | 100000 | Yes |
| NARRATION | Transaction description | "USD\|120\|..." | Yes |
| TXNDATE | Transaction date | "2026-01-07" | No |

## Narration Format

The narration field contains transaction metadata in pipe-delimited format:

```
CURRENCY|BRANCH|ACCOUNT_STUDENTID_TRANSACTIONID
```

**Example**:
```
USD|120|52289804360572_10_840632018707E63
```

**Breakdown**:
- `USD`: Currency code
- `120`: Branch code
- `52289804360572`: Parent's account number
- `10`: Student ID
- `840632018707E63`: Transaction ID

## Usage in Code

### Building a Payment Request

```java
@Autowired
private FlexcubeIntegrationService flexcubeService;

public void processPayment(Payment payment, String parentAccount) {
    // Build Flexcube request
    FCUBSPayment fcubsRequest = flexcubeService.buildSchoolFeePaymentRequest(
        payment,
        parentAccount,          // Parent's account (source - debit)
        payment.getAmount()
    );
    
    // Validate request
    flexcubeService.validatePaymentRequest(fcubsRequest);
    
    // Send to Flexcube API
    // FCUBSPaymentResponse response = flexcubeClient.sendPayment(fcubsRequest);
}
```

### Custom Payment Request

```java
FCUBSPayment customRequest = flexcubeService.buildCustomPaymentRequest(
    "L13111000",              // Source account (debit)
    58520134002019L,          // Destination account (credit)
    new BigDecimal("500.00"), // Amount
    "School fee payment",     // Narration
    "120",                    // Source branch
    "120",                    // Destination branch
    "USD"                     // Currency
);
```

## Payment Flow

```
┌─────────────┐
│   Parent    │
│   Account   │
│  L13111000  │
└──────┬──────┘
       │ DEBIT $500
       │ (TXNACC)
       │
       ▼
┌─────────────┐
│  FLEXCUBE   │
│   SYSTEM    │
└──────┬──────┘
       │ CREDIT $500
       │ (OFFSETACC)
       │
       ▼
┌─────────────┐
│   School    │
│ Collection  │
│  58520...   │
└─────────────┘
```

## Environment-Specific Configuration

### Development
```properties
flexcube.enabled=false
flexcube.api.url=https://api-sandbox.bank.example.com/flexcube
flexcube.school.collection.account=TEST_ACCOUNT
```

### Production
```properties
flexcube.enabled=true
flexcube.api.url=https://api.bank.example.com/flexcube/v1/payments
flexcube.school.collection.account=58520134002019
```

## Error Handling

Common errors and solutions:

| Error | Cause | Solution |
|-------|-------|----------|
| "Insufficient funds" | Parent account balance too low | Request parent to fund account |
| "Invalid account" | TXNACC or OFFSETACC incorrect | Verify account numbers |
| "Currency mismatch" | TXNCCY ≠ OFFSETCCY | Use same currency for both |
| "Branch not found" | Invalid TXNBRN or OFFSETBRN | Verify branch codes |

## Testing

### Test Request Example

```java
@Test
public void testFlexcubePaymentRequest() {
    FCUBSPayment testRequest = FCUBSPayment.builder()
        .SOURCE("MOBILEBANKING")
        .USERID("FLEXSWITCH")
        .BRANCH("120")
        .PROD("ABDS")
        .BRN("120")
        .TXNCCY("USD")
        .TXNBRN("120")
        .TXNACC("TEST_PARENT_ACCOUNT")
        .OFFSETCCY("USD")
        .OFFSETBRN("120")
        .OFFSETACC("TEST_SCHOOL_ACCOUNT")
        .TXNAMT(new BigDecimal("100.00"))
        .NARRATION("USD|120|TEST_TRANSACTION")
        .build();
    
    flexcubeService.validatePaymentRequest(testRequest);
    
    // Assert validations pass
    assertNotNull(testRequest.getTXNACC());
    assertNotNull(testRequest.getOFFSETACC());
    assertTrue(testRequest.getTXNAMT().compareTo(BigDecimal.ZERO) > 0);
}
```

## Security Considerations

1. **Never log full account numbers** - Use masking (e.g., `****1234`)
2. **Encrypt sensitive data** - Use HTTPS for API calls
3. **Validate all inputs** - Check account numbers before sending
4. **Audit all transactions** - Log all Flexcube requests/responses
5. **Implement timeouts** - Prevent hanging requests
6. **Rate limiting** - Avoid overwhelming Flexcube API

## Support

For Flexcube integration issues:
- Technical Support: flexcube-support@bank.example.com
- Documentation: https://bank.example.com/flexcube/docs
- API Status: https://status.bank.example.com
