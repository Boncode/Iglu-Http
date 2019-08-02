package org.ijsberg.iglu.server.service;

import org.ijsberg.iglu.server.service.model.PropertiesDto;
import org.ijsberg.iglu.util.ResourceException;
import org.ijsberg.iglu.util.io.FSFileCollection;
import org.ijsberg.iglu.util.io.FileFilterRuleSet;
import org.ijsberg.iglu.util.properties.IgluProperties;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

public class MaintenanceServiceImpl implements MaintenanceService {

    private String editablePropertyFilesRootDir;
    private FSFileCollection editablePropertiesFileCollection;

    public void setProperties(Properties properties) {
        editablePropertyFilesRootDir = properties.getProperty("editablePropertyFilesRootDir");
        editablePropertiesFileCollection = new FSFileCollection(editablePropertyFilesRootDir, new FileFilterRuleSet().setIncludeFilesWithNameMask("*.properties"));
        for(String fileName : editablePropertiesFileCollection.getFileNames()) {
            System.out.println("----> " + fileName);
        }
    }

    @Override
    public String test() {
        return "Ok";
    }

    @Override
    public List<String> listEditablePropertyFiles() {
        return editablePropertiesFileCollection.getFileNames();
    }

    @Override
    public PropertiesDto getProperties(String fileName) {
        File propertiesFile = getPropertiesFile(fileName);
        IgluProperties properties = IgluProperties.loadProperties(propertiesFile.getAbsolutePath());
        return new PropertiesDto(fileName, properties.toOrderedMap());
    }

    @Override
    public String getPropertiesAsText(String fileName) {
        try {
            return editablePropertiesFileCollection.getFileContentsAsString(fileName);
        } catch (IOException e) {
            throw new ResourceException("unable to get properties file for name: " + fileName, e);
        }
    }

    @Override
    public void saveProperties(String fileName, String propertiesAsText) {
        Properties inComingProperties = IgluProperties.loadPropertiesFromText(propertiesAsText);
        save(fileName, inComingProperties);
    }

    @Override
    public void saveProperties(String fileName, PropertiesDto propertiesDto) {
        Properties inComingProperties = IgluProperties.loadPropertiesFromMap(propertiesDto.getProperties());
        save(fileName, inComingProperties);
    }

    private void save(String fileName, Properties inComingProperties) {
        File propertiesFile = getPropertiesFile(fileName);
        IgluProperties originalProperties = IgluProperties.loadProperties(propertiesFile.getAbsolutePath());
        originalProperties.merge(inComingProperties);
        try {
            IgluProperties.saveProperties(originalProperties, propertiesFile.getAbsolutePath());
        } catch (IOException e) {
            throw new ResourceException("unable to save properties file for name: " + fileName, e);
        }
    }

    private File getPropertiesFile(String fileName) {
        File propertiesFile;
        try {
            propertiesFile = editablePropertiesFileCollection.getActualFileByName(fileName);
        } catch (IOException e) {
            throw new ResourceException("unable to get properties file for name: " + fileName, e);
        }
        return propertiesFile;
    }
}
