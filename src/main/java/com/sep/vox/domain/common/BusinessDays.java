package com.sep.vox.domain.common;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * "How many days has it been waiting" for queue cards.
 *
 * <p>Counts BUSINESS-TIME CALENDAR DAYS, not elapsed time divided by 24 hours: for an application submitted at
 * 23:00 yesterday, the person on duty reading it at 01:00 sees "second day", while dividing by 24 hours still
 * returns 0 and makes the card look as if the queue were clean.
 *
 * <p>Every card that counts waiting days must go through here. Three places use it today — school registrations
 * awaiting review, appeals awaiting handling, and sessions without a score — and three copies of the same
 * calculation need to differ in only one detail (time zone, or calendar days versus 24 hours) for three screens
 * to count days in three different ways.
 */
public final class BusinessDays {

    private BusinessDays() {
    }

    /**
     * Number of calendar days from {@code from} to {@code now}, in {@link ZoneConstant#BUSINESS_ZONE}.
     *
     * @param from start moment; null means NOTHING IS WAITING and null is returned.
     * @return null when {@code from} is null — null is NOT 0, and on these cards 0 is the best state
     *         ("just submitted today"), so merging the two would draw an empty queue exactly like a queue that
     *         just received work.
     */
    public static Integer waitedDaysSince(Instant from, Instant now) {
        if (from == null) {
            return null;
        }
        return (int) ChronoUnit.DAYS.between(
            from.atZone(ZoneConstant.BUSINESS_ZONE).toLocalDate(),
            now.atZone(ZoneConstant.BUSINESS_ZONE).toLocalDate());
    }
}
