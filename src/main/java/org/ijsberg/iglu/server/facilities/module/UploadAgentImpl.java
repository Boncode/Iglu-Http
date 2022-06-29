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

import org.ijsberg.iglu.access.*;
import org.ijsberg.iglu.access.component.RequestRegistry;
import org.ijsberg.iglu.configuration.Cluster;
import org.ijsberg.iglu.http.json.JsonData;
import org.ijsberg.iglu.http.json.JsonSupport;
import org.ijsberg.iglu.logging.Level;
import org.ijsberg.iglu.logging.LogEntry;
import org.ijsberg.iglu.rest.*;
import org.ijsberg.iglu.server.facilities.FileNameChecker;
import org.ijsberg.iglu.server.facilities.UploadAgent;
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
import org.ijsberg.iglu.util.io.model.UserUploadedFilesDto;
import org.ijsberg.iglu.util.properties.IgluProperties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.ijsberg.iglu.access.Permissions.FULL_CONTROL;
import static org.ijsberg.iglu.access.Permissions.X;
import static org.ijsberg.iglu.rest.Endpoint.ParameterType.REQUEST_RESPONSE;
import static org.ijsberg.iglu.rest.Endpoint.ParameterType.VOID;
import static org.ijsberg.iglu.rest.Endpoint.RequestMethod.GET;
import static org.ijsberg.iglu.rest.Endpoint.RequestMethod.POST;
import static org.ijsberg.iglu.util.mail.WebContentType.JSON;
import static org.ijsberg.iglu.util.mail.WebContentType.TXT;


/**
 */
public class UploadAgentImpl implements UploadAgent, FileNameChecker {

	private MultiPartReader reader;
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

	private String getUserDir() {
		return ServletSupport.getUserDir(requestRegistry);
	}

	private String getDownloadDir() {
		return uploadRootDir + "/" + ServletSupport.getUserDir(requestRegistry) + "/" + downloadSubDir;
	}

	@Override
	@AllowPublicAccess
	@Endpoint(inputType = VOID, path = "downloadable_files", method = GET, returnType = JSON,
		description = "Returns a list of downloadable files.")
	public FileCollectionDto getDownloadableFileNames() {
		FSFileCollection fileCollection = new FSFileCollection(getDownloadDir());
		System.out.println(new LogEntry("getDownloadDir(): " + getDownloadDir()));
		return new FileCollectionDto(fileCollection.getFileNames().stream()
				.map(FileDto::new)
				.collect(Collectors.toList()));
	}

	@Override
	@RequireOneOrMorePermissions(permission = {X, FULL_CONTROL})
	@Endpoint(inputType = VOID, path = "all_uploaded_files", method = GET, returnType = JSON,
			description = "Returns a list of files uploaded by all users.")
	public UserUploadedFilesDto getAllUploadedFileNames() {
		File uploadRootFile = new File(uploadRootDir);
		Map<String, FileCollectionDto> map = new HashMap<>();
		for (File userDir : uploadRootFile.listFiles()) {
			List<FileDto> fileDtos = new ArrayList<>();
			for (File uploadedFile : userDir.listFiles()) {
				if (uploadedFile.isDirectory()) {
					continue;
				}
				fileDtos.add(new FileDto(uploadedFile.getName()));
			}
			map.put(userDir.getName(), new FileCollectionDto(fileDtos));
		}
		return new UserUploadedFilesDto(map);
	}

	@Override
	@RequireOneOrMorePermissions(permission = {X, FULL_CONTROL})
	@Endpoint(inputType = REQUEST_RESPONSE, path = "download_uploaded_file", method = GET,
			description = "Downloads specified file uploaded by specified user.")
	public void downloadUploadedFile(HttpServletRequest req, HttpServletResponse response) {
		// /download_uploaded_file/customerName/fileName
		String[] path = req.getPathInfo().split("/");
		String customerName = path[2];
		String fileName = path[3];
		String resourcePath = uploadRootDir + "/" + customerName + "/" + fileName;

		try {
			DownloadSupport.downloadFile(response, resourcePath);
		} catch (IOException e) {
			System.out.println(new LogEntry(Level.CRITICAL, String.format("failed to download %s", resourcePath), e));
			requestRegistry.dropMessageToCurrentUser(new EventMessage("processFailed", "Download failed with message: " + e.getMessage()));
			response.setStatus(500);
		}
	}

	@Override
	@AllowPublicAccess
	@Endpoint(inputType = REQUEST_RESPONSE, path = "upload", method = POST,
		description = "Uploads and processes data.")
	public void readMultiPartUpload(HttpServletRequest req, HttpServletResponse res) {
		readMultiPartUpload(req, new String[]{"*"});
	}

	public synchronized String readMultiPartUpload(HttpServletRequest req, String[] allowedFormatsWildcardExpressions) {

		this.allowedFormatsWildcardExpressions = allowedFormatsWildcardExpressions;
		isUploadCancelled = false;

		Exception uploadFailedExc = null;

		System.out.println(new LogEntry("about to read multipart upload, content-type: " + req.getContentType()));

		if (req.getContentType() != null && req.getContentType().startsWith("multipart/form-data"))
		{
			//TODO content type contains boundary
			//context is the container for all submitted data
			//read the multipart uploadstream
			//data and files are set as attributes
			//ServletSupport.readMultipartUpload(req);

			if(readingUpload) {
				System.out.println(new LogEntry(Level.VERBOSE, "can not process upload: UploadAgent still busy with " +
						(reader != null ? reader.getUploadFile() : "[ERROR:reader:null]" )));
				return "BUSY";
			}

			readingUpload = true;
			try {
				System.out.println(new LogEntry(Level.VERBOSE, "about to read upload in " + uploadRootDir + '/' + getUserDir()));
				reader = new MultiPartReader(req, uploadRootDir + '/' + getUserDir(), this);
				//TODO exception if file missing
				reader.readMultipartUpload();
				System.out.println(new LogEntry(Level.VERBOSE, "reading upload " + (reader != null ? reader.getUploadFile() : "[ERROR:reader:null]" ) + " done"));
			} catch (Exception e) {
				uploadFailedExc = e;
				isUploadCancelled = true;
				System.out.println(new LogEntry(Level.CRITICAL, "reading upload " + (reader != null ? reader.getUploadFile() : "[ERROR:reader:null]" ) + " failed or was interrupted", e));
				//TODO exception if file missing
				requestRegistry.dropMessageToCurrentUser(new EventMessage("processFailed", "Upload failed with message: " + e.getMessage()));
			}
		}
		System.out.println(new LogEntry(Level.VERBOSE, "reading upload " + (reader != null ? reader.getUploadFile() : "[ERROR:reader:null]" ) + " ended"));
		if(reader != null && reader.getUploadFile() != null && !isUploadCancelled) {
			postProcess();
		} else {
			isUploadCancelled = true; //TODO distinguish between failed and cancelled
			readingUpload = false;
			System.out.println(new LogEntry(Level.CRITICAL, "reading upload " + (reader != null ? "file: " + reader.getUploadFile() : "[ERROR:reader:null]" ) + " FAILED"));
			if(reader.getUploadFile() != null) {
				reader.getUploadFile().delete();
			}
			requestRegistry.dropMessageToCurrentUser(new EventMessage("processFailed", "Upload failed or cancelled."));
		}
		readingUpload = false;

		if(uploadFailedExc != null && uploadFailedExc instanceof RestException) {
			throw (RestException)uploadFailedExc;
		}
		return "DONE";
	}

	private void postProcess() {
		File uploadedFile = reader.getUploadFile();
		FileData fileData = new FileData(uploadedFile.getPath());
		if(targetDir != null) {
			String tmpFileName = targetDir + "/ignore.tmp";
			String permanentFileName = targetDir + "/" + fileData.getFileName();
			try {
				FileSupport.copyFile(reader.getUploadFile(), tmpFileName, true);
				File file = new File(tmpFileName);
				//make file appear as atomic as possible
				file.renameTo(new File(permanentFileName));
				User user = requestRegistry.getCurrentRequest().getUser();
				IgluProperties metadata = new IgluProperties();
				metadata.setProperty("userId", user.getId());
				if(!user.getGroupNames().isEmpty()) {
					metadata.setProperty("groups", CollectionSupport.format(user.getGroupNames(), ","));
				}
				metadata.setProperty("isAdmin", "" + user.hasRole(AccessConstants.ADMIN_ROLE_NAME));
				IgluProperties.saveProperties(metadata, permanentFileName + ".metadata.properties");
				uploadedFile.delete();
			} catch (IOException e) {
				System.out.println(new LogEntry(Level.CRITICAL, "cannot move file (or metadata) to target dir", e));
				requestRegistry.dropMessageToCurrentUser(new EventMessage("processFailed", "Upload failed. A problem occurred while moving the file."));
			}
		}
		if(sendEmail) {
			notifyAsync(fileData);
		} else {
			System.out.println(new LogEntry("notification disabled"));
		}
		requestRegistry.dropMessageToCurrentUser(new EventMessage("uploadDone", uploadSuccessMessage));
	}

	private void notifyAsync(FileData fileData) {
		//TODO make configurable
		String message = fileData.getFileName() + " has been uploaded to " + getUserDir();
		System.out.println(new LogEntry(Level.VERBOSE, "about to mail: " + message));

		requestRegistry.dropMessage("System", new MailMessage(getUserDir() + " : upload notification", message));
	}

	@Override
	public long getBytesRead() {
		if(reader != null) {
			return reader.getBytesRead();
		}
		return 0;
	}

	@Override
	public long getContentLength() {
		if(reader != null) {
			return reader.getContentLength();
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
		description = "Returns the amount of bytes read and the amount of bytes to read in total.")
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
	@Endpoint(inputType = VOID, path = "cancel", method = GET, returnType = TXT,
		description = "Cancels the upload.")
	public String cancelUpload() {
		System.out.println(new LogEntry(Level.VERBOSE, "cancelling upload " + (reader != null ? reader.getUploadFile() : "[ERROR:reader:null]" )));
		if(reader != null) {
			reader.cancel();
		}
		readingUpload = false;
		isUploadCancelled = true;
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
	@Endpoint(inputType = VOID, path = "reset", method = GET,
		description = "Resets the upload.")
	public void reset() {
		System.out.println(new LogEntry(Level.VERBOSE, "resetting upload " + (reader != null ? reader.getUploadFile() : "[ERROR:reader:null]" )));
		if(reader != null) {
			if(readingUpload) {
				cancelUpload();
			}
			reader = null;
		}
		readingUpload = false;
		isUploadCancelled = false;
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
