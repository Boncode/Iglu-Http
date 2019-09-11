package org.ijsberg.iglu.server.service.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class PropertiesDto {

    private String fileName;
    private Map<String, String> properties;


    @JsonCreator
    public PropertiesDto(@JsonProperty("fileName") String fileName, @JsonProperty("properties") Map<String, String> properties) {
        this.fileName = fileName;
        this.properties = properties;
    }

    public String getFileName() {
        return fileName;
    }

    public Map<String, String> getProperties() {
        return properties;
    }
}
