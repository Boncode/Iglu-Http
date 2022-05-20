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

	List<String> getDownloadableFileNames();

    void readMultiPartUpload(HttpServletRequest req, HttpServletResponse res);

    //String readMultiPartUpload(HttpServletRequest request, Properties properties, String fileName) throws IOException;

	long getBytesRead();

	long getContentLength();

    String getProgress();

    String cancelUpload();

	boolean isUploadCancelled();

    boolean isUploadInProgress();

    void reset();

	String getProgress(String jsFunction);
}
