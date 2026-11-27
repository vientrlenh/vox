package com.sep.vox.domain.common;

/** How a school divides personal quota among teachers/students -- see DistributeQuotaToUsersService. */
public enum DistributionMode {
    /** Splits the school balance equally among every eligible person, spreading the remainder over the first people in the list. */
    AUTO,
    /** The school enters the amount for each person by hand. */
    MANUAL
}
