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

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.TreeSet;

/**
 */
public abstract class BinaryResourceServlet extends HttpServlet {

	protected String documentRoot;
	private static TreeSet<String> requestedResources = new TreeSet<String>();

	public void init(ServletConfig conf) throws ServletException {
		super.init(conf);

		documentRoot = conf.getInitParameter("document_root");
		if(documentRoot == null) {
			documentRoot = "";
		}
	}

	public void service(HttpServletRequest request, HttpServletResponse response) {
		//Creates the output stream.
		String pathInfo = request.getPathInfo();
		if(pathInfo == null) {
			pathInfo = "";
		}
		try {
			String resourcePath = FileSupport.convertToUnixStylePath(documentRoot + '/' + pathInfo);
			if(resourcePath.startsWith("/")) {
				resourcePath = resourcePath.substring(1);
			}
			if(resourcePath.endsWith("/")) {
				resourcePath += "index.html";
			}
			if(resourcePath.endsWith("STATS")) {
				writeStats(response);
				return;
			}
			requestedResources.add(resourcePath);
			ServletOutputStream out = response.getOutputStream();
            response.setContentType(MimeTypeSupport.getMimeTypeForFileExtension(resourcePath.substring(resourcePath.lastIndexOf('.') + 1)));
			out.write(getResource(resourcePath));
		} catch (Exception e) {
			System.out.println(new LogEntry(Level.CRITICAL, "unable to obtain resource", e));
			response.setStatus(500);
		}
	}

	private void writeStats(HttpServletResponse response) throws IOException {
		response.setContentType("text/plain");
		PrintWriter out = response.getWriter();
		for(String requestedResource : requestedResources) {
			out.println(requestedResource);
		}
	}


	public abstract byte[] getResource(String path) throws IOException, ServletException;

}
