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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.ijsberg.iglu.server.facilities.model.MultipartUploadProgress;
import org.ijsberg.iglu.util.io.model.FileCollectionDto;
import org.ijsberg.iglu.util.io.model.UploadedFileCommentDto;

import java.io.IOException;

/**
 */
public interface FileManagerAgent {

	FileCollectionDto getDownloadableFileNames();

    FileCollectionDto getUploadedFileNames();

    void addCommentToUploadedFile(UploadedFileCommentDto uploadedFileCommentDto);

    FileCollectionDto getAllUploadedFileNames();

    FileCollectionDto getAllDownloadableClientFiles();

    void deleteFile(String path);

    void downloadUploadedFile(HttpServletRequest req, HttpServletResponse response);

    void downloadDownloadableFile(HttpServletRequest req, HttpServletResponse response);

    void downloadUserDownloadableFile(HttpServletRequest req, HttpServletResponse res);

    //uploads

    String initializePersonalUpload();

    void startPersonalUpload(HttpServletRequest req, HttpServletResponse res);

    MultipartUploadProgress getPersonalUploadProgress(String uploadId);

    void cancelPersonalUpload(String uploadId);

    void moveFileToCustomerDownloads(String fileName, String customerId) throws IOException;
}
