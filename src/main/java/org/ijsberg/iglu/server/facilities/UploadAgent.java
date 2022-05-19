/*
 * Copyright 2011-2014 Jeroen Meetsma - IJsberg Automatisering BV
 *
 * This file is part of Iglu.
 *
 * Iglu is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Iglu is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Iglu.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.ijsberg.iglu.server.facilities;

import org.ijsberg.iglu.rest.Endpoint;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import static org.ijsberg.iglu.rest.Endpoint.ParameterType.REQUEST_RESPONSE;
import static org.ijsberg.iglu.rest.Endpoint.ParameterType.VOID;
import static org.ijsberg.iglu.rest.Endpoint.RequestMethod.GET;
import static org.ijsberg.iglu.rest.Endpoint.RequestMethod.POST;
import static org.ijsberg.iglu.util.mail.WebContentType.JSON;
import static org.ijsberg.iglu.util.mail.WebContentType.TXT;

/**
 */
public interface UploadAgent {

    @Endpoint(inputType = VOID, path = "downloadable_files", method = GET, returnType = JSON)
	List<String> getDownloadableFileNames();

    @Endpoint(inputType = REQUEST_RESPONSE, path = "upload", method = POST)
    void readMultiPartUpload(HttpServletRequest req, HttpServletResponse res);

    String readMultiPartUpload(HttpServletRequest request, Properties properties, String fileName) throws IOException;

	//String readMultiPartUpload(HttpServletRequest request, Properties properties) throws IOException;

	long getBytesRead();

	long getContentLength();

    @Endpoint(inputType = VOID, path = "progress", method = GET, returnType = JSON)
    String getProgress();

    @Endpoint(inputType = VOID, path = "cancel", method = GET, returnType = TXT)
    String cancelUpload();

	boolean isUploadCancelled();

    boolean isUploadInProgress();

    @Endpoint(inputType = VOID, path = "reset", method = GET)
    void reset();

	String getProgress(String jsFunction);
}
