package com.bank.schoolmanagement.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class FcubsRecordItem {
    private String TRN_DT;
    private String VALUE_DT;
    private String MAKER_ID;
    private String MAKER_DT_STAMP;
    private String TRN_CODE;
    private String AC_NO;
    private String DRCR_IND;
    private String TRN_DESC;
    private String AC_ENTRY_SR_NO;
    private String TRN_REF_NO;
    private Object FCY_AMOUNT;
    private String AC_CCY;
    private String CHECKER_ID;
    private String CHECKER_DT_STAMP;
    private String AC_BRANCH;
}
