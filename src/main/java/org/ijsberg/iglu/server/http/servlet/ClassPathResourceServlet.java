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
import org.ijsberg.iglu.util.formatting.PatternMatchingSupport;
import org.ijsberg.iglu.util.io.FileSupport;

import java.io.IOException;

/**
 */
public class ClassPathResourceServlet extends BinaryResourceServlet {

	protected String classPathRoot;
	protected String allowedContentRegExp;

	@Override
	public void init(ServletConfig conf) throws ServletException {
		super.init(conf);

		classPathRoot = conf.getInitParameter("classpath_root");
		if(classPathRoot == null) {
			classPathRoot = "";
		}

		allowedContentRegExp = conf.getInitParameter("allowed_content_reg_exp");
		if(allowedContentRegExp == null) {
			allowedContentRegExp = "(.*\\.html|.*\\.ico|.*\\.js|.*\\.css|.*\\.json|.*\\.svg|.*\\.png|.*\\.woff2|.*\\.ttf)";
		}
	}

	@Override
	public byte[] getResource(String path) throws IOException, ServletException {
		//prevent accessing directories
		//prevent upDirs
		if(path.contains(".") && !path.contains("..") && isAllowedContent(path)) {
			return FileSupport.getBinaryFromClassLoader(classPathRoot + path);
		}
		return null;
	}

	@Override
	protected void refresh() {

	}

	private boolean isAllowedContent(String servletPath) {
		return allowedContentRegExp != null && PatternMatchingSupport.valueMatchesRegularExpression(servletPath, allowedContentRegExp);
	}
}