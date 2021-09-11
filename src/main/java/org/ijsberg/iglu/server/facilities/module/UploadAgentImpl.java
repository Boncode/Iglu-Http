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
import org.ijsberg.iglu.rest.AllowPublicAccess;
import org.ijsberg.iglu.rest.RequestParameter;
import org.ijsberg.iglu.rest.RequestPath;
import org.ijsberg.iglu.server.facilities.UploadAgent;
import org.ijsberg.iglu.util.execution.Executable;
import org.ijsberg.iglu.util.http.MultiPartReader;
import org.ijsberg.iglu.util.http.ServletSupport;
import org.ijsberg.iglu.util.io.FSFileCollection;
import org.ijsberg.iglu.util.io.FileData;
import org.ijsberg.iglu.util.io.FileSupport;
import org.ijsberg.iglu.util.mail.EMail;
import org.ijsberg.iglu.util.properties.IgluProperties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import static org.ijsberg.iglu.rest.RequestPath.ParameterType.*;
import static org.ijsberg.iglu.rest.RequestPath.RequestMethod.GET;
import static org.ijsberg.iglu.rest.RequestPath.RequestMethod.POST;
import static org.ijsberg.iglu.util.mail.WebContentType.JSON;
import static org.ijsberg.iglu.util.mail.WebContentType.TXT;


/**
 */
public class UploadAgentImpl implements UploadAgent {

	public static final String UPLOAD_AGENT_NAME = "UploadAgent";

	private MultiPartReader reader;
	private boolean readingUpload;
	private boolean isUploadCancelled = false;
	private RequestRegistry requestRegistry;
	private Properties properties;
	//directory to move file to after upload finished
	private String targetDir;
	private String uploadRootDir = "uploads";
	private String downloadSubDir = "downloads";

	private boolean sendEmail = false;

	public UploadAgentImpl(Properties agentProperties) {
		this.properties = agentProperties;
		targetDir = properties.getProperty("target_dir");
		downloadSubDir = properties.getProperty("download_sub_dir", downloadSubDir);
		uploadRootDir = properties.getProperty("upload_root_dir", uploadRootDir);
		sendEmail = Boolean.parseBoolean(properties.getProperty("send_email", "" + sendEmail));
	}


	public void setProperties(Properties properties) {
	}

	public void setRequestRegistry(RequestRegistry requestRegistry) {
		this.requestRegistry = requestRegistry;
	}


	public static AgentFactory<UploadAgent> getAgentFactory(Cluster cluster, Properties agentProperties) {
		return new BasicAgentFactory<UploadAgent>(cluster, UPLOAD_AGENT_NAME, agentProperties) {
			public UploadAgent createAgentImpl() {
				return new UploadAgentImpl(getAgentProperties());
			}
		};
	}


/*	@Override
	public String readMultiPartUpload(HttpServletRequest request, Properties properties) throws IOException {
		return readMultiPartUpload(request, properties, null);
	}
*/

	private String getUserDir() {
		return ServletSupport.getUserDir(requestRegistry);
	}

	private String getDownloadDir() {
		return uploadRootDir + "/" + ServletSupport.getUserDir(requestRegistry) + "/" + downloadSubDir;
	}

	@Override
	@AllowPublicAccess
	@RequestPath(inputType = VOID, path = "downloadable_files", method = GET, returnType = JSON)
	public List<String> getDownloadableFileNames() {
		FSFileCollection fileCollection = new FSFileCollection(getDownloadDir());
		System.out.println("getDownloadDir(): " + getDownloadDir());
		return fileCollection.getFileNames();
	}

/*	@RequestPath(inputType = STRING, path = "calculate_next_execution_time", method = GET, returnType = TXT, parameters = {
			@RequestParameter(name = "cronUnixDef")
	})
*/
	@Override
	@AllowPublicAccess
	@RequestPath(inputType = REQUEST_RESPONSE, path = "upload", method = POST)
	public void readMultiPartUpload(HttpServletRequest req, HttpServletResponse res) {
//		System.out.println("UPLOAD ==================================================");
//		readMultiPartUpload(req, new Properties(), "configurations/analysis-server/upload/uploads/");
		readMultiPartUpload(req, new Properties(), uploadRootDir);
	}


	//TODO reconsider "synchronized" : should probably be confined to code starting with: if(readingUpload) {
	public synchronized String readMultiPartUpload(HttpServletRequest req, Properties properties, String directoryName) {

		isUploadCancelled = false;

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
				System.out.println(new LogEntry(Level.VERBOSE, "about to read upload in " + directoryName + '/' + getUserDir()));
				reader = new MultiPartReader(req, directoryName + '/' + getUserDir());
				//TODO exception if file missing
				reader.readMultipartUpload();
				System.out.println(new LogEntry(Level.VERBOSE, "reading upload " + (reader != null ? reader.getUploadFile() : "[ERROR:reader:null]" ) + " done"));
			} catch (Exception e) {
				isUploadCancelled = true;
				System.out.println(new LogEntry(Level.CRITICAL, "reading upload " + (reader != null ? reader.getUploadFile() : "[ERROR:reader:null]" ) + " failed or was interrupted", e));

				requestRegistry.dropMessageToCurrentUser(new EventMessage("processFailed", "Upload failed"));
				//TODO exception if file missing
			}


/*			Enumeration e = req.getAttributeNames();
			while (e.hasMoreElements())
			{
				String name = (String) e.nextElement();
				Object attr = req.getAttribute(name);
				properties.put(name, attr);
			}*/
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
			requestRegistry.dropMessageToCurrentUser(new EventMessage("processFailed", "Upload failed"));
		}
		readingUpload = false;
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
				if(user.getGroup() != null) {
					metadata.setProperty("group", user.getGroup().getName());
				}
				metadata.setProperty("isAdmin", "" + user.hasRole(AccessConstants.ADMIN_ROLE_NAME));
				IgluProperties.saveProperties(metadata, permanentFileName + ".metadata.properties");
				uploadedFile.delete();
			} catch (IOException e) {
				System.out.println(new LogEntry(Level.CRITICAL, "cannot move file (or metadata) to target dir", e));
				requestRegistry.dropMessageToCurrentUser(new EventMessage("processFailed", "Upload failed"));
			}
		}
		if(sendEmail) {
			notifyAsync(fileData);
		} else {
			System.out.println(new LogEntry("notification disabled"));
		}
		requestRegistry.dropMessageToCurrentUser(new EventMessage("processSuccess", "Upload success!"));
	}

	private void notifyAsync(FileData fileData) {
		//TODO make configurable
		String message = fileData.getFileName() + " has been uploaded to " + getUserDir();
		System.out.println(new LogEntry(Level.VERBOSE, "about to mail: " + message));

		requestRegistry.dropMessage("System", new MailMessage("upload notification", message));

/*		new Executable() {
			@Override
			protected Object execute() throws Throwable {
				try {
					new EMail("BONcat Upload Server", "bls@service.bon-code.nl", "bls@bon-code.nl", "file upload notification",
							message).mail();
				} catch (Exception e) {
					System.out.println(new LogEntry(Level.CRITICAL,"sending notification failed", e));
				}
				return null;
			}
		}.executeAsync();*/
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
		return JsonSupport.toMessage(jsFunction, "progress", retval);
	}



	@Override
	@AllowPublicAccess
	@RequestPath(inputType = VOID, path = "progress", method = GET, returnType = JSON)
	public String getProgress() {
		JsonData retval = new JsonData();
		JsonData jsonData = new JsonData();
		retval.addAttribute("progress", jsonData);
		jsonData.addHtmlEscapedStringAttribute("bytesRead", "" + getBytesRead());
		jsonData.addHtmlEscapedStringAttribute("contentLength", "" + getContentLength());
		return jsonData.toString();
	}

	@Override
	@AllowPublicAccess
	@RequestPath(inputType = VOID, path = "cancel", method = GET, returnType = TXT)
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
	@RequestPath(inputType = VOID, path = "reset", method = GET)
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

}
