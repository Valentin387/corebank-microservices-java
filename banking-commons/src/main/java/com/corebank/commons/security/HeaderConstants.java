package com.corebank.commons.security;

/**
 * Constants for custom banking headers used across all CoreBank services.
 * These headers simulate the production banking header propagation pattern
 * used in real financial institutions (Davivienda, RBC, Scotiabank, etc.).
 */
public final class HeaderConstants {

    private HeaderConstants() {
        // Utility class — prevent instantiation
    }

    /** Unique request identifier for traceability */
    public static final String X_RQ_UID = "X-RqUid";

    /** Session identifier */
    public static final String X_SES_ID = "X-SesID";

    /** Customer identification number (document number) */
    public static final String X_CUST_IDENT_NUM = "X-CustIdentNum";

    /** Customer identification type (CC, NIT, CE, etc.) */
    public static final String X_CUST_IDENT_TYPE = "X-CustIdentType";

    /** Standard Authorization header */
    public static final String AUTHORIZATION = "Authorization";

    /** Bearer token prefix */
    public static final String BEARER_PREFIX = "Bearer ";

    /** All banking headers that should be propagated between services */
    public static final String[] PROPAGATED_HEADERS = {
            X_RQ_UID, X_SES_ID, X_CUST_IDENT_NUM, X_CUST_IDENT_TYPE
    };
}
