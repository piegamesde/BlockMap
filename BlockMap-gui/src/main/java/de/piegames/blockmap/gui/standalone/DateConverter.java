package de.piegames.blockmap.gui.standalone;

import java.time.Instant;

/**
 * A simple class to convert dates, for example to relative strings.
 *
 * @author saibotk
 */
class DateConverter {

    public static final long SECOND_MILLI = 1000;
    public static final long MINUTE_MILLI = 60 * SECOND_MILLI;
    public static final long HOUR_MILLI = 60 * MINUTE_MILLI;
    public static final long DAY_MILLI = 24 * HOUR_MILLI;
    public static final long MONTH_MILLI = 30 * DAY_MILLI;

    /**
     * Converts an epoch millisecond timestamp to an human readable relative string.
     *
     * @param timestamp the epoch timestamp in milliseconds
     * @return the relative time eg. "2 hours" or "just now"
     */
    public static String toRelative(long timestamp) {
        long passedTime = Instant.now().toEpochMilli() - timestamp;

        if (passedTime < MINUTE_MILLI) {
            return "just now";
        } else if (passedTime < HOUR_MILLI) {
            int count = (int) (passedTime / MINUTE_MILLI);
            return count + " " + selectLangUnitAmount(count, "minute", "minutes") + " ago";
        } else if (passedTime < DAY_MILLI) {
            int count = (int) (passedTime / HOUR_MILLI);
            return count + " " + selectLangUnitAmount(count, "hour", "hours") + " ago";
        } else if (passedTime < MONTH_MILLI) {
            int count = (int) (passedTime / DAY_MILLI);
            return count + " " + selectLangUnitAmount(count, "day", "days") + " ago";
        } else {
            int count = (int) (passedTime / MONTH_MILLI);
            return count + " " + selectLangUnitAmount(count, "month", "months") + " ago";
        }
    }

    private static String selectLangUnitAmount(int amount, String single, String multiple) {
        return amount > 1 ? multiple : single;
    }
}
