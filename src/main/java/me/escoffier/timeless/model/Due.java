package me.escoffier.timeless.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.Locale;

import static java.time.format.DateTimeFormatter.*;

public class Due {

    private boolean recurring;
    private String string;
    private String date;

    public Due setRecurring(boolean recurring) {
        this.recurring = recurring;
        return this;
    }

    public Due setString(String string) {
        this.string = string;
        return this;
    }

    public Due setDate(String date) {
        this.date = date;
        return this;
    }

    public LocalDate getDeadline() {
        if (date.contains("T")) {
            return LocalDate.from(ISO_LOCAL_DATE_TIME.parse(date));
        } else {
            return LocalDate.from(ISO_LOCAL_DATE.parse(date));
        }
    }

    public boolean isRecurring() {
        return recurring;
    }

    public String getString() {
        return string;
    }

    public String getDate() {
        return date;
    }
}
