package com.example.demo_dl.dto;

import lombok.Data;
import java.util.List;

@Data
public class FunctionDefinition {
    private String name;
    private String version;
    private String packageName;
    private List<FieldDefinition> inputFields;
    private List<FieldDefinition> outputFields;
    private String description;
    private String logic;  // Custom logic description or template
}
