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

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import org.ijsberg.iglu.logging.Level;
import org.ijsberg.iglu.logging.LogEntry;
import org.ijsberg.iglu.util.io.FileSupport;

import java.io.IOException;

/**
 */
public class ZipFileResourceServlet extends BinaryResourceServlet implements ZipFileResource {

	private String zipFileName = null;
	protected String documentRoot;

	public void init(ServletConfig conf) throws ServletException {
		super.init(conf);
		zipFileName = conf.getInitParameter("zip_file_name");

		documentRoot = conf.getInitParameter("document_root");
		if(documentRoot == null) {
			documentRoot = "";
		}
	}

	@Override
	public void setZipFileName(String zipFileName) {
		this.zipFileName = zipFileName;
	}


	@Override
	public byte[] getResource(String path) throws IOException {

		if(zipFileName == null) {
			System.out.println(new LogEntry(Level.CRITICAL, this.getClass().getSimpleName() + ": cannot resolve resource"));
			return "currently no resources available".getBytes();
		}
		byte[] resource = FileSupport.getBinaryFromJar(documentRoot + path, zipFileName);
        return resource;
	}

	@Override
	protected void refresh() {

	}
}
