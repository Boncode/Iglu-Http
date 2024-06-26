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

package org.ijsberg.iglu.server.facilities.module;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.ijsberg.iglu.access.AccessConstants;
import org.ijsberg.iglu.access.AgentFactory;
import org.ijsberg.iglu.access.BasicAgentFactory;
import org.ijsberg.iglu.access.User;
import org.ijsberg.iglu.access.component.RequestRegistry;
import org.ijsberg.iglu.configuration.Cluster;
import org.ijsberg.iglu.event.messaging.MessageStatus;
import org.ijsberg.iglu.event.messaging.message.MailMessage;
import org.ijsberg.iglu.event.messaging.message.StatusMessage;
import org.ijsberg.iglu.http.json.JsonData;
import org.ijsberg.iglu.http.json.JsonSupport;
import org.ijsberg.iglu.logging.Level;
import org.ijsberg.iglu.logging.LogEntry;
import org.ijsberg.iglu.rest.*;
import org.ijsberg.iglu.server.facilities.FileNameChecker;
import org.ijsberg.iglu.server.facilities.UploadAgent;
import org.ijsberg.iglu.util.ResourceException;
import org.ijsberg.iglu.util.collection.CollectionSupport;
import org.ijsberg.iglu.util.formatting.PatternMatchingSupport;
import org.ijsberg.iglu.util.http.DownloadSupport;
import org.ijsberg.iglu.util.http.MultiPartReader;
import org.ijsberg.iglu.util.http.ServletSupport;
import org.ijsberg.iglu.util.io.FSFileCollection;
import org.ijsberg.iglu.util.io.FileData;
import org.ijsberg.iglu.util.io.FileSupport;
import org.ijsberg.iglu.util.io.model.FileCollectionDto;
import org.ijsberg.iglu.util.io.model.FileDto;
import org.ijsberg.iglu.util.properties.IgluProperties;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import static org.ijsberg.iglu.access.Permissions.FULL_CONTROL;
import static org.ijsberg.iglu.access.Permissions.UPLOAD;
import static org.ijsberg.iglu.rest.Endpoint.ParameterType.*;
import static org.ijsberg.iglu.rest.Endpoint.RequestMethod.GET;
import static org.ijsberg.iglu.rest.Endpoint.RequestMethod.POST;
import static org.ijsberg.iglu.util.mail.WebContentType.JSON;
import static org.ijsberg.iglu.util.mail.WebContentType.TXT;


/**
 */
public class UploadAgentImpl implements UploadAgent, FileNameChecker {

	MultiPartReader reader;
	private boolean readingUpload;
	private boolean isUploadCancelled = false;
	private RequestRegistry requestRegistry;
	private Properties properties;
	//directory to move file to after upload finished
	private String targetDir;
	private String uploadRootDir = "uploads";
	private String downloadSubDir = "downloads";

	private String uploadSuccessMessage = "Upload success! The file will be processed shortly.";

	private String[] allowedFormatsWildcardExpressions;

	private boolean sendEmail = false;

	public static final String UPLOAD_AGENT_NAME = "UploadAgent";

	public static AgentFactory<UploadAgent> getAgentFactory(Cluster cluster, Properties agentProperties) {
		return new BasicAgentFactory<>(cluster, UPLOAD_AGENT_NAME, agentProperties) {
			public UploadAgent createAgentImpl() {
				return new UploadAgentImpl(getAgentProperties());
			}
		};
	}

	public UploadAgentImpl(Properties agentProperties) {
		this.properties = agentProperties;
		targetDir = properties.getProperty("target_dir");
		downloadSubDir = properties.getProperty("download_sub_dir", downloadSubDir);
		uploadRootDir = properties.getProperty("upload_root_dir", uploadRootDir);
		uploadSuccessMessage = properties.getProperty("upload_success_message", uploadSuccessMessage);
		sendEmail = Boolean.parseBoolean(properties.getProperty("send_email", "" + sendEmail));
	}

	public void setProperties(Properties properties) {
	}

	public void setRequestRegistry(RequestRegistry requestRegistry) {
		this.requestRegistry = requestRegistry;
	}

	protected String getUserDir() {
		return ServletSupport.getUserDir(requestRegistry);
	}

	private String getDownloadDir() {
		return uploadRootDir + "/" + ServletSupport.getUserDir(requestRegistry) + "/" + downloadSubDir;
	}

	@Override
	@AllowPublicAccess
	@Endpoint(inputType = VOID, path = "downloadable_files", method = GET, returnType = JSON,
		description = "Retrieve a list of downloadable files for the current user.")
	public FileCollectionDto getDownloadableFileNames() {
		FSFileCollection fileCollection = new FSFileCollection(getDownloadDir());
		System.out.println(new LogEntry("getDownloadDir(): " + getDownloadDir()));
		return new FileCollectionDto(fileCollection.getFileNames().stream()
				.map(fileName -> new FileDto(fileName, getUserDir()))
				.collect(Collectors.toList()));
	}

	@Override
	@SystemEndpoint
	@RequireOneOrMorePermissions(permission = {FULL_CONTROL})
	@Endpoint(inputType = MAPPED, path = "delete_file", method = POST,
			description = "Delete the given file.",
			parameters = {
				@RequestParameter(name = "path")
			})
	public void deleteFile(String path) {
		File toDelete;
		try {
			toDelete = DownloadSupport.getDownloadableFile(uploadRootDir + "/" + path);
		} catch (ResourceException e) {
			throw new RestException("Cannot delete file: " + e.getMessage(), 403);
		}
		System.out.println(new LogEntry(Level.VERBOSE, "deleting uploaded client file " + toDelete));
		try {
			FileSupport.deleteFile(toDelete);
		} catch (IOException e) {
			String msg = "unable to delete " + toDelete;
			System.out.println(new LogEntry(Level.CRITICAL, msg));
			throw new RestException(msg, 500);
		}
	}

	@Override
	@SystemEndpoint
	@RequireOneOrMorePermissions(permission = {FULL_CONTROL})
	@Endpoint(inputType = VOID, path = "all_uploaded_files", method = GET, returnType = JSON,
			description = "Retrieve a list of files uploaded by all users.")
	public FileCollectionDto getAllUploadedFileNames() {
		File uploadRootFile = new File(uploadRootDir);
		List<FileDto> fileDtos = new ArrayList<>();
		if (uploadRootFile.listFiles() != null) {
			for (File userDir : uploadRootFile.listFiles()) {
				if (userDir == null || userDir.listFiles() == null) {
					continue;
				}
				for (File uploadedFile : userDir.listFiles()) {
					if (uploadedFile == null || uploadedFile.isDirectory()) {
						continue;
					}
					fileDtos.add(new FileDto(uploadedFile.getName(), userDir.getName()));
				}
			}
		}
		return new FileCollectionDto(fileDtos);
	}

	@Override
	@SystemEndpoint
	@RequireOneOrMorePermissions(permission = {FULL_CONTROL})
	@Endpoint(inputType = VOID, path = "all_downloadable_client_files", method = GET, returnType = JSON,
			description = "Retrieve a list of files retrievable by all users.")
	public FileCollectionDto getAllDownloadableClientFiles() {
		File uploadRootFile = new File(uploadRootDir);
		List<FileDto> downloadableFiles = new ArrayList<>();
		if (uploadRootFile.listFiles() != null) {
			for (File userDir : uploadRootFile.listFiles()) {
				if (userDir == null || !userDir.isDirectory()) {
					continue;
				}
				File userDownloadDirectory = new File(userDir.getPath() + "/downloads");
				String customerName = userDir.getName();
				if (userDownloadDirectory.listFiles() != null) {
					for (File downloadableFile : userDownloadDirectory.listFiles()) {
						if (!downloadableFile.isDirectory()) {
							downloadableFiles.add(new FileDto(downloadableFile.getName(), customerName));
						}
					}
				}
			}
		}
		return new FileCollectionDto(downloadableFiles);
	}

	@Override
	@SystemEndpoint
	@RequireOneOrMorePermissions(permission = {FULL_CONTROL})
	@Endpoint(inputType = REQUEST_RESPONSE, path = "uploaded_file", method = GET,
			description = "Retrieve specified file uploaded by specified user.")
	public void downloadUploadedFile(HttpServletRequest req, HttpServletResponse response) {
		String[] path = req.getPathInfo().split("/");
		String resourcePath = uploadRootDir + "/" + path[2] + "/" + path[3];
		downloadFile(response, resourcePath);
	}

	private void downloadFile(HttpServletResponse response, String path) {
		try {
			DownloadSupport.downloadFile(response, path);
		} catch (IOException e) {
			System.out.println(new LogEntry(Level.CRITICAL, String.format("failed to download %s", path), e));
			requestRegistry.dropMessageToCurrentUser(new StatusMessage("processFailed", "Download failed with message: " + e.getMessage(), MessageStatus.FAILURE));
			response.setStatus(500);
		}
	}

	@Override
	@SystemEndpoint
	@RequireOneOrMorePermissions(permission = {FULL_CONTROL})
	@Endpoint(inputType = REQUEST_RESPONSE, path = "downloadable_file", method = GET,
			description = "Retrieve specified file downloadable by specified user.")
	public void downloadDownloadableFile(HttpServletRequest req, HttpServletResponse response) {
		String[] path = req.getPathInfo().split("/");
		String resourcePath = uploadRootDir + "/" + path[2] + "/downloads/" + path[3];
		downloadFile(response, resourcePath);
	}


	@Override
	@RequireOneOrMorePermissions(permission = {UPLOAD})
	@BypassCsrfCheck
	@Endpoint(inputType = REQUEST_RESPONSE, path = "upload", method = POST,
		description = "Upload and process data.")
	public void readMultiPartUpload(HttpServletRequest req, HttpServletResponse res) {
		readMultiPartUpload(req, new String[]{"*"}, getUserDir());
	}

	@Override
	@SystemEndpoint
	@BypassCsrfCheck
	@RequireOneOrMorePermissions(permission = {FULL_CONTROL})
	@Endpoint(inputType = REQUEST_RESPONSE, path = "upload_for_client", method = POST,
		description = "Upload and process file to /downloads folder for the given user for them to retrieve.")
	public void readMultiPartUploadForClient(HttpServletRequest req, HttpServletResponse res) {
		// /upload_for_client/customerName
		String customerName = req.getPathInfo().split("/")[2];
		System.out.println(new LogEntry(Level.DEBUG, String.format("uploading file for client %s", customerName)));
		String destination = customerName + "/downloads";
		readMultiPartUpload(req, new String[]{"*"}, destination);
	}

	private static final Object lock = new Object();

	public void readMultiPartUpload(HttpServletRequest req, String[] allowedFormatsWildcardExpressions, String destinationPath) {
		System.out.println(new LogEntry("about to read multipart upload, content-type: " + req.getContentType()));
		if (req.getContentType() != null && req.getContentType().startsWith("multipart/form-data")) {
			synchronized (lock) {
				if (readingUpload) {
					System.out.println(new LogEntry(Level.VERBOSE, "can not process upload: UploadAgent still busy"));
					return;
				}
				readingUpload = true;
				isUploadCancelled = false;
			}

			File uploadedFile = null;

			this.allowedFormatsWildcardExpressions = allowedFormatsWildcardExpressions;

			try {
				System.out.println(new LogEntry(Level.VERBOSE, "about to read upload in " + uploadRootDir + '/' + destinationPath));
				reader = new MultiPartReader(req, uploadRootDir + '/' + destinationPath, this);
				//TODO exception if file missing
				uploadedFile = reader.readMultipartUpload();
				System.out.println(new LogEntry(Level.VERBOSE, "reading upload " + uploadedFile + " done"));
			} catch (Exception e) { //TODO try IOException
				//TODO exception if file missing
				if(!isUploadCancelled) {
					System.out.println(new LogEntry(Level.CRITICAL, "reading upload " + uploadedFile + " failed or was interrupted", e));
					requestRegistry.dropMessageToCurrentUser(new StatusMessage("processFailed", "Upload failed with message: " + e.getMessage(), MessageStatus.FAILURE));
				}
				cancelUpload();
				reader = null;
				return;
			}
			if (!isUploadCancelled) {
				postProcess(uploadedFile);
			}
			synchronized (lock) {
				reader = null;
				readingUpload = false;
			}
		} else {
			System.out.println(new LogEntry(Level.CRITICAL, "cannot process upload for content type " + req.getContentType()));
		}
	}

	private void postProcess(File uploadedFile) {
		FileData fileData = new FileData(uploadedFile.getPath());
		if(targetDir != null) {
			String tmpFileName = targetDir + "/ignore.tmp";
			String permanentFileName = targetDir + "/" + fileData.getFileName();
			try {
				FileSupport.copyFile(uploadedFile, tmpFileName, true);
				File file = new File(tmpFileName);
				//make file appear as atomic as possible
				file.renameTo(new File(permanentFileName));
				User user = requestRegistry.getCurrentRequest().getUser();
				IgluProperties metadata = new IgluProperties();
				metadata.setProperty("userId", user.getId());
//				if(!user.getGroupNames().isEmpty()) {
//					metadata.setProperty("groups", CollectionSupport.format(user.getGroupNames(), ","));
//				}
//				metadata.setProperty("isAdmin", "" + user.hasRole(AccessConstants.ADMIN_ROLE_NAME));
				IgluProperties.saveProperties(metadata, permanentFileName + ".metadata.properties");
				uploadedFile.delete();
			} catch (IOException e) {
				System.out.println(new LogEntry(Level.CRITICAL, "cannot move file (or metadata) to target dir", e));
				requestRegistry.dropMessageToCurrentUser(new StatusMessage("processFailed", "Upload failed. A problem occurred while moving the file.", MessageStatus.FAILURE));
			}
		}
		if(sendEmail) {
			notifyAsync(fileData);
		} else {
			System.out.println(new LogEntry("notification disabled"));
		}
		requestRegistry.dropMessageToCurrentUser(new StatusMessage("uploadDone", uploadSuccessMessage, MessageStatus.SUCCESS));
	}

	private void notifyAsync(FileData fileData) {
		//TODO make configurable
		String message = fileData.getFileName() + " has been uploaded to " + getUserDir();
		System.out.println(new LogEntry(Level.VERBOSE, "about to mail: " + message));

		requestRegistry.dropMessage("System", new MailMessage(getUserDir() + " : upload notification", message));
	}

	@Override
	public long getBytesRead() {
		synchronized (lock) {
			if (reader != null) {
				return reader.getBytesRead();
			}
		}
		return 0;
	}

	@Override
	public long getContentLength() {
		synchronized (lock) {
			if (reader != null) {
				return reader.getContentLength();
			}
		}
		return 0;
	}

	@Override
	public String getProgress(String jsFunction) {
		JsonData retval = new JsonData();
		JsonData jsonData = new JsonData();
		retval.addAttribute("progress", jsonData);
		jsonData.addHtmlEscapedStringAttribute("bytesRead", "" + getBytesRead());
		jsonData.addHtmlEscapedStringAttribute("contentLength", "" + getContentLength());
		jsonData.addAttribute("isUploadCancelled", isUploadCancelled);

		return JsonSupport.toMessage(jsFunction, "progress", retval);
	}

	@Override
	@SystemEndpoint
	@AllowPublicAccess
	@Endpoint(inputType = VOID, path = "progress", method = GET, returnType = JSON,
		description = "Retrieve the amount of bytes read and the amount of bytes to read in total.")
	public String getProgress() {
		JsonData retval = new JsonData();
		JsonData jsonData = new JsonData();
		retval.addAttribute("progress", jsonData);
		jsonData.addHtmlEscapedStringAttribute("bytesRead", "" + getBytesRead());
		jsonData.addHtmlEscapedStringAttribute("contentLength", "" + getContentLength());
		jsonData.addAttribute("isUploadCancelled", isUploadCancelled);
		return jsonData.toString();
	}

	@Override
	@SystemEndpoint
	@AllowPublicAccess
	@Endpoint(inputType = VOID, path = "cancel", method = POST, returnType = TXT,
		description = "Cancel the upload.")
	public String cancelUpload() {
		System.out.println(new LogEntry(Level.VERBOSE, "cancelling upload"));
		synchronized (lock) {
			if (reader != null) {
				reader.cancel();
			}
			readingUpload = false;
			isUploadCancelled = true;
		}
		return "";
	}

	@Override
	public boolean isUploadCancelled() {
		return isUploadCancelled;

	}

	@Override
	public boolean isUploadInProgress() {
		return readingUpload;

	}

	@Override
	@SystemEndpoint
	@AllowPublicAccess
	@Endpoint(inputType = VOID, path = "reset", method = POST,
		description = "Reset the upload.")
	public void reset() {
		System.out.println(new LogEntry(Level.VERBOSE, "resetting upload"));
		synchronized (lock) {
			if (readingUpload) {
				cancelUpload();
			}
//			readingUpload = false;
			isUploadCancelled = false;
		}
	}

	@Override
	public void assertFileNameAllowed(String fullFileName) {
		for(String wildcardExp : allowedFormatsWildcardExpressions) {
			if(PatternMatchingSupport.valueMatchesWildcardExpression(fullFileName, wildcardExp)) {
				return;
			}
		}
		throw new RestException("file " + fullFileName + " not allowed to be uploaded", 403);
	}
}
