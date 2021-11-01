package me.escoffier.timeless.helpers;

import java.util.Calendar;

public class TodayOrTomorrow {

    private TodayOrTomorrow() {
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
}
