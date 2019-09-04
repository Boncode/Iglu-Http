package org.ijsberg.iglu.server.service;

import org.ijsberg.iglu.rest.RequestParameter;
import org.ijsberg.iglu.rest.RequestPath;
import org.ijsberg.iglu.server.service.model.FileList;
import org.ijsberg.iglu.server.service.model.PropertiesDto;

import static org.ijsberg.iglu.rest.RequestPath.ParameterType.*;
import static org.ijsberg.iglu.rest.RequestPath.RequestMethod.GET;
import static org.ijsberg.iglu.rest.RequestPath.RequestMethod.POST;

public interface MaintenanceAgent {
    @RequestPath(inputType = VOID, path = "test", method = GET, returnType = STRING)
    String test();

    @RequestPath(inputType = VOID, path = "listEditablePropertiesFiles", method = GET, returnType = JSON)
    FileList listEditablePropertiesFiles();

    @RequestPath(inputType = MAPPED, path = "getProperties", method = GET, returnType = JSON, parameters = {
            @RequestParameter(name = "fileName")})
    PropertiesDto getProperties(String fileName);

    @RequestPath(inputType = MAPPED, path = "getProperties", method = GET, returnType = STRING, parameters = {
            @RequestParameter(name = "fileName")})
    String getPropertiesAsText(String fileName);

    @RequestPath(inputType = JSON, path = "saveProperties", method = POST, returnType = VOID)
    void saveProperties(PropertiesDto propertiesDto);

    @RequestPath(inputType = MAPPED, path = "tryPost", method = POST, returnType = STRING, parameters = {
            @RequestParameter(name = "field_1"),@RequestParameter(name = "field_1")})
    String tryPost(String f1, String f2);

    @RequestPath(inputType = MAPPED, path = "tryPost", method = POST, returnType = STRING, parameters = {
            @RequestParameter(name = "fileName"),@RequestParameter(name = "propertiesAsText")})
    //can be used in HTML form with method:POST
    void saveProperties(String fileName, String propertiesAsText);

    @RequestPath(inputType = VOID, path = "listAvailablePatches", method = GET, returnType = JSON)
    FileList listAvailablePatches();

    @RequestPath(inputType = VOID, path = "executePatch", method = GET, returnType = VOID)
    void executePatch();
}
