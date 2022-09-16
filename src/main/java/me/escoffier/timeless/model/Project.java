package me.escoffier.timeless.model;


import com.fasterxml.jackson.annotation.JsonProperty;

public record Project(String id, String name, @JsonProperty("is_archived") boolean archived, @JsonProperty("parent_id") String parent) {

}
