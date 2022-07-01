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

import org.ijsberg.iglu.util.io.model.FileCollectionDto;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 */
public interface UploadAgent {

	FileCollectionDto getDownloadableFileNames();

    FileCollectionDto getAllUploadedFileNames();

    FileCollectionDto getAllDownloadableClientFiles();

    void deleteFile(String path);

    void downloadUploadedFile(HttpServletRequest req, HttpServletResponse response);

    void downloadDownloadableFile(HttpServletRequest req, HttpServletResponse response);

    void readMultiPartUpload(HttpServletRequest req, HttpServletResponse res);

    void readMultiPartUploadForClient(HttpServletRequest req, HttpServletResponse res);

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
