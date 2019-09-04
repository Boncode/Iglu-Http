package org.ijsberg.iglu.server.service;

import org.ijsberg.iglu.access.AccessConstants;
import org.ijsberg.iglu.access.User;
import org.ijsberg.iglu.access.component.RequestRegistry;
import org.ijsberg.iglu.configuration.Startable;
import org.ijsberg.iglu.logging.LogEntry;
import org.ijsberg.iglu.server.service.model.PropertiesDto;
import org.ijsberg.iglu.util.ResourceException;
import org.ijsberg.iglu.util.io.FSFileCollection;
import org.ijsberg.iglu.util.io.FileCollection;
import org.ijsberg.iglu.util.io.FileFilterRuleSet;
import org.ijsberg.iglu.util.io.FileSupport;
import org.ijsberg.iglu.util.properties.IgluProperties;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.zip.ZipFile;

public class MaintenanceServiceImpl implements MaintenanceService {

    private Startable coreAssembly;
    private RequestRegistry requestRegistry;

    private String editablePropertyFilesRootDir;
    private FSFileCollection editablePropertiesFileCollection;

    public MaintenanceServiceImpl() {
    }

    public void setCoreAssembly(Startable coreAssembly) {
        this.coreAssembly = coreAssembly;
    }


    public void setProperties(Properties properties) {
        editablePropertyFilesRootDir = properties.getProperty("editablePropertyFilesRootDir");
        editablePropertiesFileCollection = new FSFileCollection(editablePropertyFilesRootDir, new FileFilterRuleSet().setIncludeFilesWithNameMask("*.properties"));
        for(String fileName : editablePropertiesFileCollection.getFileNames()) {
            System.out.println("----> " + fileName);
        }
    }

    public void setRequestRegistry(RequestRegistry registry) {
        this.requestRegistry = registry;
    }

    @Override
    public String test() {
        return "Ok\n" + "User:" + requestRegistry.getCurrentRequest().getUser();
    }

    @Override
    public boolean hasAdminRights() {
        User user = requestRegistry.getCurrentRequest().getUser();
        if(user != null) {
            System.out.println(new LogEntry("checking rights for " + user.getId() + (user.getGroup() != null ? " : " + user.getGroup().getName() : "")));
        }
        return user != null && user.getGroup() != null && AccessConstants.ADMIN_GROUP_NAME.equals(user.getGroup().getName());
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

    @Override
    public List<String> listAvailablePatches() {
        List<String> retval = new ArrayList<>();
        Map<Date,File> filesByDate = getPatchesByDate();
        for(Date date : filesByDate.keySet()) {
            File file = filesByDate.get(date);
            retval.add(file.getPath() + " " + date);
        }
        return retval;
    }

    public TreeMap<Date, File> getPatchesByDate() {
        TreeMap<Date,File> filesByDate = new TreeMap<>();
        FileCollection files = new FSFileCollection(".", new FileFilterRuleSet().setIncludeFilesWithNameMask("*.patch"));
        for(String fileName : files.getFileNames()) {
            try {
                File file = ((FSFileCollection) files).getActualFileByName(fileName);
                filesByDate.put(new Date(file.lastModified()), file);
            } catch (IOException e) {
                throw new ResourceException("file " + fileName + " not found", e);
            }
        }
        return filesByDate;
    }

    @Override
    public void executePatch() {
        TreeMap<Date,File> filesByDate = getPatchesByDate();
        File chosenPatch;
        if(!filesByDate.isEmpty()) {
            chosenPatch = filesByDate.lastEntry().getValue();
            try {
                FileSupport.moveFile(chosenPatch.getPath(), "./patch.zip", true);
            } catch (IOException e) {
                throw new ResourceException("cannot move " + chosenPatch.getPath(), e);
            }
            coreAssembly.stop();
        }
    }

}
