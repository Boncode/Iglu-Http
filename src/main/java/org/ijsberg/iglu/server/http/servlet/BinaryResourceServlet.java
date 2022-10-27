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

package org.ijsberg.iglu.server.http.servlet;

import org.ijsberg.iglu.logging.Level;
import org.ijsberg.iglu.logging.LogEntry;
import org.ijsberg.iglu.util.io.FileSupport;
import org.ijsberg.iglu.util.mail.MimeTypeSupport;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 */
public abstract class BinaryResourceServlet extends HttpServlet {

	public void service(HttpServletRequest request, HttpServletResponse response) {

		String pathInfo = request.getPathInfo();
		if(pathInfo == null) {
			pathInfo = "";
		}
		try {
			String resourcePath = FileSupport.convertToUnixStylePath(pathInfo);
			if(resourcePath.endsWith("/")) {
				resourcePath += "index.html";
			}
			if(resourcePath.startsWith("/")) {
				resourcePath = resourcePath.substring(1);
			}
			if("refresh".equals(resourcePath)) {
				refresh();
				response.setStatus(200);
				return;
			}

			ServletOutputStream out = response.getOutputStream();
			response.setContentType(MimeTypeSupport.getMimeTypeForFileExtension(resourcePath.substring(resourcePath.lastIndexOf('.') + 1)));

			byte[] responseData =  getResource(resourcePath);
			if(responseData == null) {
				response.setStatus(404);
				return;
			}

			System.out.println(new LogEntry(Level.TRACE, "resource path: " + resourcePath));
			out.write(responseData);
		} catch (Exception e) {
			System.out.println(new LogEntry(Level.VERBOSE, "BinaryResourceServlet: unable to obtain resource " + pathInfo, e));
			response.setStatus(404);
		}
	}

	public abstract byte[] getResource(String path) throws IOException, ServletException;

	protected abstract void refresh();

}
