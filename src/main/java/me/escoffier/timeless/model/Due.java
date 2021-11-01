package me.escoffier.timeless.model;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;

public class Due {

    public boolean recurring;
    public String string;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd['T'HH:mm:ss['Z']]")
    public LocalDate date;

    public LocalDate getDeadline() {
        return date;
    }

}
