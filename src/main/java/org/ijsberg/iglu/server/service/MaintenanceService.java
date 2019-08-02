package org.ijsberg.iglu.server.service;

import org.ijsberg.iglu.server.service.model.PropertiesDto;

import java.util.List;

public interface MaintenanceService {

    String test();

    List<String> listEditablePropertyFiles();

    PropertiesDto getProperties(String fileName);

    String getPropertiesAsText(String fileName);

    void saveProperties(String fileName, String propertiesAsText);

    void saveProperties(String fileName, PropertiesDto propertiesDto);
}
