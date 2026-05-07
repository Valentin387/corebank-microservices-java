package com.corebank.commons.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HeaderConstantsTest {

    @Test
    @DisplayName("Header constants should have correct values")
    void headerConstantsShouldHaveCorrectValues() {
        assertEquals("X-RqUid", HeaderConstants.X_RQ_UID);
        assertEquals("X-SesID", HeaderConstants.X_SES_ID);
        assertEquals("X-CustIdentNum", HeaderConstants.X_CUST_IDENT_NUM);
        assertEquals("X-CustIdentType", HeaderConstants.X_CUST_IDENT_TYPE);
        assertEquals("Authorization", HeaderConstants.AUTHORIZATION);
        assertEquals("Bearer ", HeaderConstants.BEARER_PREFIX);
    }

    @Test
    @DisplayName("Propagated headers should contain all banking headers")
    void propagatedHeadersShouldContainAllBankingHeaders() {
        String[] headers = HeaderConstants.PROPAGATED_HEADERS;

        assertEquals(4, headers.length);
        assertArrayEquals(new String[]{
                "X-RqUid", "X-SesID", "X-CustIdentNum", "X-CustIdentType"
        }, headers);
    }
}
