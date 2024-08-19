package org.ijsberg.iglu.server.facilities.module;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.ijsberg.iglu.access.*;
import org.ijsberg.iglu.access.component.RequestRegistry;
import org.ijsberg.iglu.configuration.Cluster;
import org.ijsberg.iglu.event.messaging.MessageStatus;
import org.ijsberg.iglu.event.messaging.message.MailMessage;
import org.ijsberg.iglu.event.messaging.message.StatusMessage;
import org.ijsberg.iglu.logging.Level;
import org.ijsberg.iglu.logging.LogEntry;
import org.ijsberg.iglu.rest.*;
import org.ijsberg.iglu.server.facilities.FileManagerAgent;
import org.ijsberg.iglu.server.facilities.FileUploadManager;
import org.ijsberg.iglu.server.facilities.model.MultipartUploadProgress;
import org.ijsberg.iglu.util.ResourceException;
import org.ijsberg.iglu.util.http.DownloadSupport;
import org.ijsberg.iglu.util.http.ServletSupport;
import org.ijsberg.iglu.util.io.FSFileCollection;
import org.ijsberg.iglu.util.io.FileData;
import org.ijsberg.iglu.util.io.FileFilterRuleSet;
import org.ijsberg.iglu.util.io.FileSupport;
import org.ijsberg.iglu.util.io.model.FileCollectionDto;
import org.ijsberg.iglu.util.io.model.FileDto;
import org.ijsberg.iglu.util.io.model.UploadedFileCommentDto;
import org.ijsberg.iglu.util.io.model.UploadedFileDto;
import org.ijsberg.iglu.util.mail.WebContentType;
import org.ijsberg.iglu.util.properties.IgluProperties;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.ijsberg.iglu.access.Permissions.FULL_CONTROL;
import static org.ijsberg.iglu.access.Permissions.UPLOAD;
import static org.ijsberg.iglu.rest.Endpoint.ParameterType.*;
import static org.ijsberg.iglu.rest.Endpoint.RequestMethod.GET;
import static org.ijsberg.iglu.rest.Endpoint.RequestMethod.POST;
import static org.ijsberg.iglu.util.mail.WebContentType.JSON;

public class FileManagerAgentImpl implements FileManagerAgent {

	public static final String FILE_MANAGER_AGENT_NAME = "FileManagerAgent";

	private RequestRegistry requestRegistry;

	private Properties properties;
	private String uploadDir = "uploads/";
	private boolean sendEmail;

	private FileUploadManager personalFileUploadManager;


	public static AgentFactory<FileManagerAgent> getAgentFactory(Cluster cluster, Properties agentProperties) {
		return new BasicAgentFactory<>(cluster, FILE_MANAGER_AGENT_NAME, agentProperties) {
			public FileManagerAgent createAgentImpl() {
				return new FileManagerAgentImpl(getAgentProperties());
			}
		};
	}

	public FileManagerAgentImpl(Properties agentProperties) {
		this.properties = agentProperties;
		uploadDir = properties.getProperty("upload_dir", uploadDir);
		sendEmail = Boolean.parseBoolean(properties.getProperty("send_email", "false"));
	}

	public void setProperties(Properties properties) {

	}

	public void setRequestRegistry(RequestRegistry requestRegistry) {
		this.requestRegistry = requestRegistry;
	}

	@Override
	@AllowPublicAccess
	@Endpoint(inputType = VOID, path = "downloadable_files", method = GET, returnType = JSON,
		description = "Retrieve a list of downloadable files for the current user.")
	public FileCollectionDto getDownloadableFileNames() {
		FSFileCollection fileCollection = getUserDownloadsFileCollection();
		return new FileCollectionDto(fileCollection.getFileNames().stream()
				.map(fileName -> new FileDto(fileName, getUserDir()))
				.collect(Collectors.toList()));
	}

	@Override
	@RequireOneOrMorePermissions(permission = {UPLOAD})
	@Endpoint(inputType = VOID, path = "uploaded_files", method = GET, returnType = JSON,
			description = "Retrieve a list of uploaded files of the current user.")
	public FileCollectionDto getUploadedFileNames() {
		FSFileCollection fileCollection = getUserUploadsFileCollection();
		return enrichWithMetadata(fileCollection);
	}

	private FSFileCollection getUserUploadsFileCollection() {
		return new FSFileCollection(uploadDir + "/" + getUserDir() + "/",
				new FileFilterRuleSet()
						.setIncludeFilesWithNameMask("*")
						.excludeFilesWithNameMask("*/downloads/*", "*.metadata.properties"));
	}

	private FSFileCollection getUserDownloadsFileCollection() {
		return new FSFileCollection(getUserDownloadDir(),
				new FileFilterRuleSet()
						.setIncludeFilesWithNameMask("*")
						.excludeFilesWithNameMask("*.metadata.properties"));
	}

	private FileCollectionDto enrichWithMetadata(FSFileCollection fileCollection) {
		List<FileDto> fileDtos = new ArrayList<>();
		for(String fileName : fileCollection.getFileNames()) {
			String metadataPropertiesFileName = uploadDir + "/" + getUserDir() + "/" + fileName + ".metadata.properties";
			if(IgluProperties.propertiesExist(metadataPropertiesFileName)) {
				IgluProperties metadataProperties = IgluProperties.loadProperties(metadataPropertiesFileName);
				fileDtos.add(createFileDtoWithMetadata(fileName, metadataProperties.getProperty("userId"), Long.parseLong(metadataProperties.getProperty("uploadedTimestamp")), metadataProperties.getProperty("comment", "No comments")));
			} else {
				fileDtos.add(createFileDtoWithMetadata(fileName, "N/a", 0L, "No comments"));
			}
		}
		return new FileCollectionDto(fileDtos);
	}

	private UploadedFileDto createFileDtoWithMetadata(String fileName, String userId, long uploadedTimestamp, String comment) {
		return new UploadedFileDto(
				fileName,
				getUserDir(),
				userId,
				uploadedTimestamp,
				comment
		);
	}

	@Override
	@RequireOneOrMorePermissions(permission = {UPLOAD})
		@Endpoint(inputType = JSON_POST, path = "add_comment", method = POST,
			description = "Add a comment to a previously uploaded file.")
	public void addCommentToUploadedFile(UploadedFileCommentDto uploadedFileCommentDto) {
		if(getUserUploadsFileCollection().containsFile(uploadedFileCommentDto.getFileName())) {
			String metadataPropertiesFileName = uploadDir + "/" + getUserDir() + "/" + uploadedFileCommentDto.getFileName() + ".metadata.properties";
			if(IgluProperties.propertiesExist(metadataPropertiesFileName)) {
				IgluProperties metadataProperties = IgluProperties.loadProperties(metadataPropertiesFileName);
				metadataProperties.setProperty("comment", uploadedFileCommentDto.getComment());
				try {
					IgluProperties.saveProperties(metadataProperties, metadataPropertiesFileName);
				} catch (IOException e) {
					System.out.println(new LogEntry(Level.DEBUG, "Saving metadata properties for file " + uploadedFileCommentDto.getFileName() + " has failed.", e));
					throw new RestException("Saving comment failed", 500);
				}
			} else {
				// create new metadataproperties
				IgluProperties metadataProperties = createMetadataPropertiesForUploadedFile();
				metadataProperties.setProperty("comment", uploadedFileCommentDto.getComment());
				try {
					IgluProperties.saveProperties(metadataProperties, metadataPropertiesFileName);
				} catch (IOException e) {
					System.out.println(new LogEntry(Level.DEBUG, "Saving metadata properties for file " + uploadedFileCommentDto.getFileName() + " has failed.", e));
					throw new RestException("Saving comment failed", 500);
				}
			}
		}
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
			toDelete = DownloadSupport.getDownloadableFile(uploadDir + "/" + path);
		} catch (FileNotFoundException e) {
			throw new RestException("File not found", 404);
		}
		System.out.println(new LogEntry(Level.VERBOSE, "deleting uploaded client file " + toDelete));
		try {
			FileSupport.deleteFile(toDelete);
			FileSupport.deleteFile(toDelete + ".metadata.properties"); //as well as potential metadata properties
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
		File uploadRootFile = new File(uploadDir);
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
					if(uploadedFile.getName().endsWith(".metadata.properties")) {
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
		File uploadRootFile = new File(uploadDir);
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
						if (downloadableFile.isDirectory() || downloadableFile.getName().endsWith(".metadata.properties")) {
							continue;
						}
						downloadableFiles.add(new FileDto(downloadableFile.getName(), customerName));
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
		String resourcePath = uploadDir + "/" + path[2] + "/" + path[3];
		downloadFile(response, resourcePath);
	}

	private void downloadFile(HttpServletResponse response, String path) {
		try {
			DownloadSupport.downloadFile(response, path);
		} catch (IOException e) {
			System.out.println(new LogEntry(Level.CRITICAL, String.format("failed to download %s", path), e));
			requestRegistry.dropMessageToCurrentUser(new StatusMessage("processFailed", "Download failed with message: " + e.getMessage(), MessageStatus.FAILURE));
			response.setStatus(500);
		} catch (ResourceException re) {
			System.out.println(new LogEntry(Level.DEBUG, String.format("Resource exception for %s", path), re));
			response.setStatus(404);
		}
	}

	@Override
	@SystemEndpoint
	@RequireOneOrMorePermissions(permission = {FULL_CONTROL})
	@Endpoint(inputType = REQUEST_RESPONSE, path = "downloadable_file", method = GET,
			description = "Retrieve specified file downloadable by specified user.")
	public void downloadDownloadableFile(HttpServletRequest req, HttpServletResponse response) {
		String[] path = req.getPathInfo().split("/");
		String resourcePath = uploadDir + "/" + path[2] + "/downloads/" + path[3];
		downloadFile(response, resourcePath);
	}


	@Override
	@BypassCsrfCheck
	@RequireOneOrMorePermissions(permission = {UPLOAD})
	@Endpoint(inputType = REQUEST_RESPONSE, path = "download", method = POST,
		description = "Download a downloadable file.")
	public void downloadUserDownloadableFile(HttpServletRequest req, HttpServletResponse res) {
		String[] path = req.getPathInfo().split("/");
		String downloadFileName = path[2]; // upload/download/filename.txt
		String userDir = getUserDir();
		if(userDir != null) {
			String resourcePath = uploadDir + "/" + userDir + "/downloads/" + downloadFileName;
			downloadFile(res, resourcePath);
		}
	}

	@Override
	@RequireOneOrMorePermissions(permission = {UPLOAD})
	@Endpoint(inputType = VOID, path = "personal/initialize", method = GET, returnType = WebContentType.TXT,
			description = "Initialize personal upload.")
	public String initializePersonalUpload() {
		try {
			return getPersonalFileUploadManager().initialize();
		} catch (IllegalStateException ise) {
			throw new RestException(ise.getMessage(), 400);
		}
	}

	@Override
	@BypassCsrfCheck
	@RequireOneOrMorePermissions(permission = {UPLOAD})
	@Endpoint(inputType = REQUEST_RESPONSE, path = "personal/upload", method = POST,
			description = "Start the upload for uploadId")
	public void startPersonalUpload(HttpServletRequest req, HttpServletResponse res) {
		try {
			getPersonalFileUploadManager().upload(req);
			if(sendEmail) {
				notifyAsync(new FileData(getPersonalFileUploadManager().getUploadedFile()));
			}
		} catch (IllegalArgumentException iae) { //iae message is safe for users
			throw new RestException(iae.getMessage(), 400);
		} catch (IOException e) { // in case of failure in file write in postProcess
			throw new RestException("An error occurred while post processing upload.", 500);
		}
	}

	@Override
	@RequireOneOrMorePermissions(permission = {UPLOAD})
	@Endpoint(inputType = FROM_PATH, path = "personal/progress", method = GET, returnType = WebContentType.JSON,
			description = "Get upload progress for uploadId.")
	public MultipartUploadProgress getPersonalUploadProgress(String uploadIdString) {
		try {
			return getPersonalFileUploadManager().getProgress(uploadIdString);
		} catch (IllegalArgumentException iae) {
			throw new RestException(iae.getMessage(), 400);
		}
	}

	@Override
	@RequireOneOrMorePermissions(permission = {UPLOAD})
	@Endpoint(inputType = FROM_PATH, path = "personal/cancel", method = POST,
			description = "Cancel upload by uploadId.")
	public void cancelPersonalUpload(String uploadIdString) {
		getPersonalFileUploadManager().cancel(uploadIdString);
	}

	private FileUploadManager getPersonalFileUploadManager() {
		if(personalFileUploadManager == null) {
			IgluProperties generalFileUploadProperties = new IgluProperties();
			generalFileUploadProperties.merge(properties);
			generalFileUploadProperties.setProperty("upload_dir", generalFileUploadProperties.getProperty("upload_dir") + getUserDir());
			personalFileUploadManager = new FileUploadManager(generalFileUploadProperties, requestRegistry);
		}
		return personalFileUploadManager;
	}

	@Override
	@RequireOneOrMorePermissions(permission = {FULL_CONTROL})
	@Endpoint(inputType = MAPPED, path = "personal/move", method = POST,
			description = "Move uploaded file to downloads folder for client.", parameters = {
			@RequestParameter(name = "fileName"),
			@RequestParameter(name = "customerName")
	})
	public void moveFileToCustomerDownloads(String fileName, String customerName) throws IOException {
		String fullFileName = uploadDir + getUserDir() + fileName;
		File fileToMove = new File(fullFileName);
		if(fileToMove.exists()) {
			String newFileName = uploadDir + customerName + "/" + "downloads/" + fileName;

			//todo check if customerName is legit, but only administrator can do this currently
			FileSupport.moveFile(fullFileName, newFileName, true);
			File metadata = new File(fullFileName + ".metadata.properties");
			if(metadata.exists()) {
				FileSupport.moveFile(fullFileName + ".metadata.properties", newFileName + ".metadata.properties", true);
			}
		} else {
			throw new RestException("File not found: " + fileName, 404);
		}
	}

	private IgluProperties createMetadataPropertiesForUploadedFile() {
		User user = requestRegistry.getCurrentRequest().getUser();
		IgluProperties metadata = new IgluProperties();
		metadata.setProperty("userId", user.getId());
		metadata.setProperty("uploadedTimestamp", "" + new Date().getTime());
		return metadata;
	}

	private void notifyAsync(FileData fileData) {
		String message = fileData.getFileName() + " has been uploaded to " + getUserDir();
		System.out.println(new LogEntry(Level.VERBOSE, "about to mail: " + message));
		requestRegistry.dropMessage("System", new MailMessage(getUserDir() + " : upload notification", message));
	}

	private String getUserDir() {
		return ServletSupport.getUserDir(requestRegistry) + "/";
	}

	private String getUserDownloadDir() {
		return uploadDir + getUserDir() + "downloads/";
	}
}
