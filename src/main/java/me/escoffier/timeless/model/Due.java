package me.escoffier.timeless.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

public record Due(
        @JsonProperty("data")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd['T'HH:mm:ss['Z']]")
        LocalDate deadline,
        String string,
        boolean recurring) {

}
