package com.example.demo_dl.dto;

import lombok.Data;

@Data
public class FieldDefinition {
    private String name;
    private FieldType type;
    private boolean required;
    private String description;

    public enum FieldType {
        STRING,
        INT,
        LONG,
        BOOLEAN,
        DOUBLE
    }
}
