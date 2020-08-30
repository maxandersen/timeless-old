package me.escoffier.timeless.model;

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
