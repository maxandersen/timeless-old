package me.escoffier.timeless.helpers;

import java.util.Calendar;

public class DueDates {

    private DueDates() {
        // Avoid direct instantiation
    }

    public static String todayOrTomorrow() {
        Calendar instance = Calendar.getInstance();
        int day = instance.get(Calendar.DAY_OF_WEEK);
        int hour = instance.get(Calendar.HOUR_OF_DAY);
        if (day == Calendar.SATURDAY || day == Calendar.SUNDAY) {
            return "monday";
        }
        return hour >= 18 ? "tomorrow" : "today";
    }

    public static String inThreeDays() {
        Calendar instance = Calendar.getInstance();
        int day = instance.get(Calendar.DAY_OF_WEEK);
        switch (day) {
            case Calendar.MONDAY:
                return "thursday";
            case Calendar.TUESDAY:
                return "friday";

            case Calendar.WEDNESDAY:
            case Calendar.THURSDAY:
                return "monday";

            case Calendar.FRIDAY:
                return "tuesday";
            case Calendar.SATURDAY:
            case Calendar.SUNDAY:
                return "wednesday";
            default:
                return "in 3 days";
        }
    }
}
