-- ========================================
-- Bank Payment Enhancement Migration
-- ========================================
-- Purpose: Add bank payment fields to support:
--   Option A: Bank teller counter payments
--   Option C: Mobile/Internet banking payments
-- 
-- Date: December 10, 2025
-- ========================================

-- Add bank payment fields to payments table
ALTER TABLE payments
ADD COLUMN bank_branch VARCHAR(100) COMMENT 'Bank branch where payment was processed',
ADD COLUMN teller_name VARCHAR(100) COMMENT 'Bank teller who processed payment',
ADD COLUMN parent_account_number VARCHAR(50) COMMENT 'Parent bank account number for reconciliation',
ADD COLUMN bank_transaction_id VARCHAR(100) COMMENT 'Bank internal transaction ID',
ADD COLUMN bank_processed_time DATETIME COMMENT 'Timestamp when bank processed payment';

-- Add index on bank_transaction_id for fast lookup
CREATE INDEX idx_payments_bank_transaction_id ON payments(bank_transaction_id);

-- Add index on bank_processed_time for date range queries
CREATE INDEX idx_payments_bank_processed_time ON payments(bank_processed_time);

-- Add index on parent_account_number for reconciliation
CREATE INDEX idx_payments_parent_account ON payments(parent_account_number);

-- Update payment_method column to accommodate new enum values
ALTER TABLE payments 
MODIFY COLUMN payment_method VARCHAR(50) COMMENT 'Payment method: CASH, MOBILE_MONEY, BANK_TRANSFER, CHEQUE, CARD, BANK_COUNTER, MOBILE_BANKING, INTERNET_BANKING, USSD, STANDING_ORDER';

-- ========================================
-- Verification Queries
-- ========================================

-- Verify new columns exist
SELECT 
    COLUMN_NAME, 
    DATA_TYPE, 
    CHARACTER_MAXIMUM_LENGTH,
    COLUMN_COMMENT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'school_management_db'
  AND TABLE_NAME = 'payments'
  AND COLUMN_NAME IN (
      'bank_branch', 
      'teller_name', 
      'parent_account_number', 
      'bank_transaction_id', 
      'bank_processed_time'
  )
ORDER BY ORDINAL_POSITION;

-- Verify indexes created
SHOW INDEX FROM payments 
WHERE Key_name IN (
    'idx_payments_bank_transaction_id',
    'idx_payments_bank_processed_time',
    'idx_payments_parent_account'
);

-- Test data validation (no bank payments should exist yet)
SELECT 
    COUNT(*) as total_payments,
    SUM(CASE WHEN bank_transaction_id IS NOT NULL THEN 1 ELSE 0 END) as bank_payments,
    SUM(CASE WHEN bank_transaction_id IS NULL THEN 1 ELSE 0 END) as school_payments
FROM payments;

-- ========================================
-- Sample Test Data (Optional - for testing)
-- ========================================

-- Uncomment to create sample bank payment
/*
-- Find a student for testing
SET @test_student_id = (SELECT id FROM students LIMIT 1);
SET @test_school_id = (SELECT school_id FROM students WHERE id = @test_student_id);

-- Insert sample bank counter payment
INSERT INTO payments (
    payment_reference,
    school_id,
    student_id,
    amount,
    payment_method,
    status,
    bank_branch,
    teller_name,
    parent_account_number,
    bank_transaction_id,
    bank_processed_time,
    received_by,
    payment_notes,
    is_reversed,
    payment_date
) VALUES (
    CONCAT('PAY-TEST-', UNIX_TIMESTAMP()),
    @test_school_id,
    @test_student_id,
    100.00,
    'BANK_COUNTER',
    'COMPLETED',
    'Branch 005 - Harare Central',
    'John Ncube',
    '1234567890',
    CONCAT('BNK-TXN-', UNIX_TIMESTAMP()),
    NOW(),
    'Bank: John Ncube',
    'Test bank counter payment',
    FALSE,
    NOW()
);

-- Verify test payment created
SELECT 
    payment_reference,
    amount,
    payment_method,
    bank_branch,
    teller_name,
    bank_transaction_id,
    payment_date
FROM payments
WHERE payment_reference LIKE 'PAY-TEST-%'
ORDER BY payment_date DESC
LIMIT 1;
*/

-- ========================================
-- Rollback Script (if needed)
-- ========================================
/*
-- WARNING: This will delete the new columns and data!
-- Only run this if you need to undo the migration

-- Drop indexes first
DROP INDEX idx_payments_bank_transaction_id ON payments;
DROP INDEX idx_payments_bank_processed_time ON payments;
DROP INDEX idx_payments_parent_account ON payments;

-- Drop columns
ALTER TABLE payments
DROP COLUMN bank_branch,
DROP COLUMN teller_name,
DROP COLUMN parent_account_number,
DROP COLUMN bank_transaction_id,
DROP COLUMN bank_processed_time;

-- Restore payment_method column length
ALTER TABLE payments 
MODIFY COLUMN payment_method VARCHAR(20);
*/
