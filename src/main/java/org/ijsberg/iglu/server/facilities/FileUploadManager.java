package org.ijsberg.iglu.server.facilities;

import jakarta.servlet.http.HttpServletRequest;
import org.ijsberg.iglu.access.User;
import org.ijsberg.iglu.access.component.RequestRegistry;
import org.ijsberg.iglu.logging.Level;
import org.ijsberg.iglu.logging.LogEntry;
import org.ijsberg.iglu.server.facilities.model.MultipartUploadProgress;
import org.ijsberg.iglu.server.facilities.model.UploadStatus;
import org.ijsberg.iglu.util.formatting.PatternMatchingSupport;
import org.ijsberg.iglu.util.http.MultiPartReader;
import org.ijsberg.iglu.util.io.FileData;
import org.ijsberg.iglu.util.io.FileSupport;
import org.ijsberg.iglu.util.properties.IgluProperties;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.UUID;

public class FileUploadManager implements FileNameChecker {

    private final MultiPartReader uploadReader;
    private UploadStatus uploadStatus = UploadStatus.DONE;
    private UUID currentUploadId = null;

    private final String[] allowedFormatsWildcardExpressions;
    private final String[] disallowedFormatsWildcardExpressions;
    private final String uploadDir;
    private final String targetDir;

    private final RequestRegistry requestRegistry;
    private final UploadObserver uploadObserver;

    public FileUploadManager(IgluProperties properties, RequestRegistry requestRegistry, UploadObserver observer) {
        this.uploadDir = properties.getProperty("upload_dir");
        this.targetDir = properties.getProperty("target_dir");
        this.allowedFormatsWildcardExpressions = properties.getPropertyAsArray("allowed_files_wildcard");
        this.disallowedFormatsWildcardExpressions = properties.getPropertyAsArray("disallowed_files_wildcard");

        this.uploadReader = new MultiPartReader(uploadDir, this);
        this.requestRegistry = requestRegistry;
        this.uploadObserver = observer;
    }

    @Override
    public void assertFileNameAllowed(String fileName) throws InvalidFilenameException {
        for(String wildCardExp : disallowedFormatsWildcardExpressions) {
            if(PatternMatchingSupport.valueMatchesWildcardExpression(fileName, wildCardExp)) {
                throw new InvalidFilenameException("file " + fileName + " is not allowed, " + wildCardExp + " is not allowed.");
            }
        }

        for(String wildcardExp : allowedFormatsWildcardExpressions) {
            if(PatternMatchingSupport.valueMatchesWildcardExpression(fileName, wildcardExp)) {
                return;
            }
        }
        throw new InvalidFilenameException("file " + fileName + " does not match naming convention");
    }

    private boolean isBusy() {
        return uploadStatus == UploadStatus.PENDING || uploadStatus == UploadStatus.IN_PROGRESS;
    }

    public String initialize() throws IllegalStateException {
        if(isBusy()) {
            throw new IllegalStateException("An upload is already in progress at this time.");
        }

        uploadReader.reset();

        uploadStatus = UploadStatus.PENDING;
        currentUploadId = UUID.randomUUID();
        return currentUploadId.toString();
    }


    public void upload(HttpServletRequest req) throws IOException, InvalidFilenameException {
        String uploadIdString = getUploadIdStringFromRequest(req.getPathInfo());
        assertUploadIdValid(uploadIdString);

        uploadStatus = UploadStatus.IN_PROGRESS;

        try {
            uploadReader.readMultipartUpload(req);
        } catch (IOException | InvalidFilenameException e) {
            uploadReader.cancel();
            uploadStatus = UploadStatus.CANCELLED;
            throw e;
        }

        uploadReader.close();

        try {
            postProcess();
            uploadStatus = UploadStatus.DONE;
            uploadObserver.onUploadDone();
        } catch (IOException e) {
            System.out.println(new LogEntry(Level.CRITICAL, "cannot move file (or metadata) to target dir", e));
            uploadReader.cancel();
            uploadStatus = UploadStatus.CANCELLED;
            throw e;
        }
    }

    private String getUploadIdStringFromRequest(String pathInfo) {
        if(!pathInfo.contains("/")) {
            System.out.println(new LogEntry(Level.CRITICAL, "Endpoint contains no upload id in path: " + pathInfo));
            throw new IllegalArgumentException("No upload id provided.");
        }
        return pathInfo.substring(pathInfo.lastIndexOf("/") + 1);
    }

    private void postProcess() throws IOException {
        File uploadedFile = getUploadedFile();

        FileData fileData = new FileData(uploadedFile.getPath());
        String permanentFileName = fileData.getFullFileName();
        if(targetDir != null) {
            String tmpFileName = targetDir + "/ignore.tmp";
            permanentFileName = targetDir + "/" + fileData.getFileName();
            FileSupport.copyFile(uploadedFile, tmpFileName, true);
            File file = new File(tmpFileName);
            //make file appear as atomic as possible
            file.renameTo(new File(permanentFileName));
            uploadedFile.delete();
        }
        IgluProperties metadata = createMetadataPropertiesForUploadedFile();
        IgluProperties.saveProperties(metadata, permanentFileName + ".metadata.properties");
    }

    private IgluProperties createMetadataPropertiesForUploadedFile() {
        User user = requestRegistry.getCurrentRequest().getUser();
        IgluProperties metadata = new IgluProperties();
        metadata.setProperty("userId", user.getId());
        metadata.setProperty("uploadedTimestamp", "" + new Date().getTime());
        return metadata;
    }

    private void assertUploadIdValid(String uploadIdString) {
        try {
            UUID uploadId = UUID.fromString(uploadIdString);
            if (!uploadId.equals(currentUploadId)) {
                System.out.println(new LogEntry(Level.CRITICAL, "Provided upload id (" + uploadId + ") doesn't match current id (" + currentUploadId + ")."));
                throw new IllegalArgumentException("Provided upload id doesn't match current id.");
            }
        } catch (IllegalArgumentException e) {
            System.out.println(new LogEntry(Level.CRITICAL, "Invalid upload id: " + uploadIdString));
            throw new IllegalArgumentException("Invalid upload id.");
        }
    }

    public MultipartUploadProgress getProgress(String uploadIdString) {
        assertUploadIdValid(uploadIdString);

        return new MultipartUploadProgress(
                currentUploadId.toString(),
                uploadReader.getBytesRead(),
                uploadReader.getContentLength(),
                uploadStatus
        );
    }

    public void cancel(String uploadIdString) {
        assertUploadIdValid(uploadIdString);

        uploadReader.cancel();
        uploadStatus = UploadStatus.CANCELLED;
    }

    public File getUploadedFile() {
        return uploadReader.getUploadedFile();
    }
}
