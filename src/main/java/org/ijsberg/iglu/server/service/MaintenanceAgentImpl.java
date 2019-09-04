package org.ijsberg.iglu.server.service;

import org.ijsberg.iglu.access.AgentFactory;
import org.ijsberg.iglu.access.BasicAgentFactory;
import org.ijsberg.iglu.configuration.Cluster;
import org.ijsberg.iglu.rest.RequestParameter;
import org.ijsberg.iglu.rest.RequestPath;
import org.ijsberg.iglu.rest.RestException;
import org.ijsberg.iglu.server.service.model.FileList;
import org.ijsberg.iglu.server.service.model.PropertiesDto;

import java.util.Properties;

import static org.ijsberg.iglu.rest.RequestPath.ParameterType.*;
import static org.ijsberg.iglu.rest.RequestPath.RequestMethod.GET;
import static org.ijsberg.iglu.rest.RequestPath.RequestMethod.POST;

public class MaintenanceAgentImpl implements MaintenanceAgent {

    public static final String AGENT_NAME = "MaintenanceAgent";

    public static AgentFactory<MaintenanceAgent> getAgentFactory(Cluster cluster, Properties properties) {
        return new BasicAgentFactory<MaintenanceAgent>(cluster, AGENT_NAME, properties) {
            public MaintenanceAgent createAgentImpl() {
                return new MaintenanceAgentImpl();
            }
        };
    }

    private MaintenanceService maintenanceService;

    public void setMaintenanceService(MaintenanceService maintenanceService) {
        this.maintenanceService = maintenanceService;
    }

    @Override
    @RequestPath(inputType = VOID, path = "test", method = GET, returnType = STRING)
    public String test() {
        return maintenanceService.test() + " admin ? " + maintenanceService.hasAdminRights();
    }

    @Override
    @RequestPath(inputType = MAPPED, path = "tryPost", method = POST, returnType = STRING, parameters = {
            @RequestParameter(name = "field_1"),@RequestParameter(name = "field_2")})
    public String tryPost(String f1, String f2) {
        return "OK: " + f1 + ":" + f2;
    }

    //localhost:17691/service/listEditablePropertyFiles
    @Override
    @RequestPath(inputType = VOID, path = "listEditablePropertyFiles", method = GET, returnType = JSON)
    public FileList listEditablePropertiesFiles() {
        assertIsAdmin();
        return new FileList(maintenanceService.listEditablePropertyFiles());
    }

    @Override
    @RequestPath(inputType = MAPPED, path = "getProperties", method = GET, returnType = JSON, parameters = {
            @RequestParameter(name = "fileName")})
    public PropertiesDto getProperties(String fileName) {
        assertIsAdmin();
        return maintenanceService.getProperties(fileName);
    }

    @Override
    @RequestPath(inputType = MAPPED, path = "getPropertiesAsText", method = GET, returnType = STRING, parameters = {
            @RequestParameter(name = "fileName")})
    public String getPropertiesAsText(String fileName) {
        assertIsAdmin();
        return maintenanceService.getPropertiesAsText(fileName);
    }

    @Override
    @RequestPath(inputType = JSON, path = "saveProperties", method = POST, returnType = VOID)
    public void saveProperties(PropertiesDto propertiesDto) {
        assertIsAdmin();
        maintenanceService.saveProperties(propertiesDto.getFileName(), propertiesDto);
    }

    @Override
    @RequestPath(inputType = MAPPED, path = "tryPost", method = POST, returnType = STRING, parameters = {
            @RequestParameter(name = "fileName"),@RequestParameter(name = "propertiesAsText")})
    //can be used in HTML form with method:POST
    public void saveProperties(String fileName, String propertiesAsText) {
        assertIsAdmin();
        maintenanceService.saveProperties(fileName, propertiesAsText);
    }


    private void assertIsAdmin() {
        if(!maintenanceService.hasAdminRights()) {
            throw new RestException("operation not allowed", 403);
        }
    }

    @Override
    @RequestPath(inputType = VOID, path = "listAvailablePatches", method = GET, returnType = JSON)
    public FileList listAvailablePatches() {
        assertIsAdmin();
        return new FileList(maintenanceService.listAvailablePatches());
    }

    @Override
    @RequestPath(inputType = VOID, path = "executePatch", method = GET, returnType = VOID)
    public void executePatch() {
        assertIsAdmin();
        maintenanceService.executePatch();
    }
}
