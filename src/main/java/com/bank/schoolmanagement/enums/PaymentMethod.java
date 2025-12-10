package com.bank.schoolmanagement.enums;

/**
 * Payment Method Enum - All supported payment channels
 * 
 * LEARNING: Payment channels in Zimbabwe's banking ecosystem
 * 
 * SCHOOL-BASED CHANNELS:
 * - Parents pay directly at school bursar's office
 * 
 * BANK-BASED CHANNELS (Option A & C):
 * - Parents pay at bank branches or via digital banking
 * - Bank system records payment and notifies school
 */
public enum PaymentMethod {
    
    // ========== SCHOOL-BASED PAYMENTS ==========
    
    /**
     * Cash payment at school bursar's office
     * Most common in Zimbabwe
     */
    CASH("Cash Payment", "School", false),
    
    /**
     * Mobile money payment at school
     * EcoCash, OneMoney, Telecash
     * Parent sends money to school's mobile wallet
     */
    MOBILE_MONEY("Mobile Money", "School", false),
    
    /**
     * Bank transfer initiated by parent
     * School receives notification from bank
     */
    BANK_TRANSFER("Bank Transfer", "School", false),
    
    /**
     * Cheque deposited at school
     * School deposits to bank later
     */
    CHEQUE("Cheque", "School", false),
    
    /**
     * Card payment at school
     * Point-of-sale terminal at bursar's office
     */
    CARD("Card Payment", "School", false),
    
    // ========== BANK-BASED PAYMENTS (Option A - Teller) ==========
    
    /**
     * Parent pays at bank teller counter
     * Teller processes payment in school management system
     * Real-time notification to school
     */
    BANK_COUNTER("Bank Counter Payment", "Bank", true),
    
    // ========== BANK-BASED PAYMENTS (Option C - Digital) ==========
    
    /**
     * Parent pays via bank's mobile app
     * Self-service payment using student reference number
     * Instant confirmation and receipt
     */
    MOBILE_BANKING("Mobile Banking", "Bank", true),
    
    /**
     * Parent pays via bank's internet banking website
     * Desktop/laptop browser payment
     */
    INTERNET_BANKING("Internet Banking", "Bank", true),
    
    /**
     * Parent pays via USSD (*123# codes)
     * Works on basic phones without smartphone
     * Common in Zimbabwe for accessibility
     */
    USSD("USSD Payment", "Bank", true),
    
    /**
     * Automated recurring payment
     * Bank deducts from parent's account monthly/termly
     * Set-it-and-forget-it convenience
     */
    STANDING_ORDER("Standing Order", "Bank", true);
    
    // ========== ENUM PROPERTIES ==========
    
    private final String displayName;
    private final String channel; // "School" or "Bank"
    private final boolean requiresBankTransaction; // true if needs bankTransactionId
    
    PaymentMethod(String displayName, String channel, boolean requiresBankTransaction) {
        this.displayName = displayName;
        this.channel = channel;
        this.requiresBankTransaction = requiresBankTransaction;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getChannel() {
        return channel;
    }
    
    public boolean isRequiresBankTransaction() {
        return requiresBankTransaction;
    }
    
    /**
     * Check if this is a bank channel payment
     */
    public boolean isBankChannel() {
        return "Bank".equals(channel);
    }
    
    /**
     * Check if this is a school channel payment
     */
    public boolean isSchoolChannel() {
        return "School".equals(channel);
    }
    
    /**
     * Get payment method from string (backward compatibility)
     */
    public static PaymentMethod fromString(String value) {
        if (value == null) return CASH; // Default
        
        try {
            return PaymentMethod.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return CASH; // Default fallback
        }
    }
    
    /**
     * Get all bank channel methods
     */
    public static PaymentMethod[] getBankChannelMethods() {
        return new PaymentMethod[]{
            BANK_COUNTER,
            MOBILE_BANKING,
            INTERNET_BANKING,
            USSD,
            STANDING_ORDER
        };
    }
    
    /**
     * Get all school channel methods
     */
    public static PaymentMethod[] getSchoolChannelMethods() {
        return new PaymentMethod[]{
            CASH,
            MOBILE_MONEY,
            BANK_TRANSFER,
            CHEQUE,
            CARD
        };
    }
}
