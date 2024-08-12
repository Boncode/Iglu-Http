package org.ijsberg.iglu.util.http;

import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import org.ijsberg.iglu.logging.Level;
import org.ijsberg.iglu.logging.LogEntry;
import org.ijsberg.iglu.server.facilities.FileNameChecker;
import org.ijsberg.iglu.util.io.FileData;
import org.ijsberg.iglu.util.io.FileSupport;

import java.io.*;

public class MultiPartReader2 {

    public static final int BUFFER_SIZE = 1024;

    private final String uploadDir;
    private final FileNameChecker fileNameChecker;

//    private HttpServletRequest request;
    private ServletInputStream input;

    private byte[] line = new byte[BUFFER_SIZE];
    private int bytesReadAsLine;

//    private String propertyName = null;
    private String fullFileName = null;
//    private String contentType = null;

    private long contentLength = 0;
    private long bytesRead = 0;

    private OutputStream partialCopyOutputStream;
    private File uploadFile;
    private String boundary;

    public MultiPartReader2(String uploadDir, FileNameChecker fileNameChecker) {
        this.uploadDir = uploadDir;
        this.fileNameChecker = fileNameChecker;
    }

    public long getContentLength() {
        return contentLength;
    }

    public long getBytesRead() {
        return bytesRead;
    }

    public File getUploadedFile() {
        return uploadFile;
    }

    /**
     * Reads uploaded files and form variables from POSTed data.
     * Files are stored on disk in case uploadDir is not null.
     * Otherwise files are stored as attributes on the http-request in the form of FileObjects, using the name of
     * the html-form-variable as key.
     *
     * The file can be retrieved by the getUploadFile method after calling this method successfully.
     *
     * Shortcomings:
     *  - todo this reader cannot deal with multiple boundary specifications. It reads a boundary once, then uses that
     *     for the upload.
     *
     * @throws IOException
     * @see org.ijsberg.iglu.util.io.FileData
     */
    public void readMultipartUpload(HttpServletRequest request) throws IOException {
//        this.request = request;
        contentLength = request.getContentLength();
        input = request.getInputStream();

        getMultiPartBoundary();

        while ((bytesReadAsLine = input.readLine(line, 0, BUFFER_SIZE)) != -1) {

            //TODO fullFileName may be empty in which case storage output stream cannot be opened
            readPropertiesUntilEmptyLine();

            fileNameChecker.assertFileNameAllowed(fullFileName);

            getStorageOutputStream();
            readUploadedFile();

//            if (fullFileName != null) {
//                if (uploadDir == null) {
//                    setFileAsObjectOnRequest();
//                }
//            } else {
//                setValueAsPropertyOnRequest();
//            }

            partialCopyOutputStream.close();
//            fullFileName = null;
        }
        System.out.println(new LogEntry("post data retrieved from multi-part data: " + bytesRead + " bytes"));
    }

    private void getMultiPartBoundary() throws IOException {
        bytesReadAsLine = input.readLine(line, 0, BUFFER_SIZE);

        if(bytesReadAsLine == -1) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignore) {
            }
            bytesReadAsLine = input.readLine(line, 0, BUFFER_SIZE);
        }

        bytesRead = bytesReadAsLine;

        boundary = new String(line, 0, bytesReadAsLine).stripTrailing(); //remove newline chars
    }

    /**
     * Read the multipart data.
     * This works by keeping track of 3 lines at the same time basically. We need this because when we find the
     * boundary, the formatting of the lines before the boundary can vary (newlines carriage returns).
     * We don't want to write those to output stream.
     * @throws IOException
     */
    private void readUploadedFile() throws IOException {
        byte[] previousLine = new byte[BUFFER_SIZE];
        int previousLength = 0;

        byte[] previousPreviousLine = new byte[BUFFER_SIZE];
        int previousPreviousLength = 0;

        bytesReadAsLine = input.readLine(line, 0, BUFFER_SIZE);

        LOOP:
        for (; bytesReadAsLine != -1; bytesReadAsLine = input.readLine(line, 0, BUFFER_SIZE)) {
            String newLine = new String(line, 0, bytesReadAsLine);

            if (newLine.startsWith(boundary)) {
                //must be only a LF char, this situation doesn't occur at all anymore with modern browsers.
                //we still want to write the remainder of the multipart data, while getting rid of the CR char on the line before (hence the - 1)
                //we can just skip the second to last line, as it contains only the LF
                //we skip the current line, as it only contains the boundary info
                if ((previousLength == 1) && (previousPreviousLength > 0)) {
                    partialCopyOutputStream.write(previousPreviousLine, 0, previousPreviousLength - 1);//get rid of the CR (\r)
                    bytesRead += previousPreviousLength;

                // the normal situation for modern browsers, we still have some data to write on the previous line,
                // because length >=2 (consisting of CRLF + some other data), we just write the previous-previous line,
                // and then get rid of the CRLF on the second to last line
                // we skip current line altogether as it just contains the boundary
                } else if (previousLength >= 2) {
                    partialCopyOutputStream.write(previousPreviousLine, 0, previousPreviousLength);
                    partialCopyOutputStream.write(previousLine, 0, previousLength - 2);//get rid of the CRLF (\r\n)
                    bytesRead += previousPreviousLength + previousLength;
                }
                bytesRead += bytesReadAsLine;
                //lastly we break, we are done.
                break LOOP;
            }
            partialCopyOutputStream.write(previousPreviousLine, 0, previousPreviousLength);

            bytesRead += previousPreviousLength;
            previousPreviousLength = previousLength;
            previousLength = bytesReadAsLine;

            previousPreviousLine = previousLine;
            previousLine = line;
            line = new byte[BUFFER_SIZE];
        }
    }

    private void getStorageOutputStream() throws IOException {
        if (fullFileName != null && uploadDir != null) {
            //check if file exists
            FileData file = new FileData(fullFileName/*, contentType*/);
            uploadFile = new File(uploadDir + '/' + file.getFileName());
            if(uploadFile.exists()) {
                uploadFile = getFileWithUniqueName(file, uploadFile);
            }
            uploadFile = FileSupport.createFile(uploadFile.getPath());
            partialCopyOutputStream = new FileOutputStream(uploadFile);
        } else {
            partialCopyOutputStream = new ByteArrayOutputStream();
        }
    }

    /**
     * In case the file exists, we suffix its name with '_' followed by an integer number so that it is unique.
     * @param file
     * @param uploadFile
     * @return
     */
    private File getFileWithUniqueName(FileData file, File uploadFile) {
        int i = 0;
        File existingUploadFile = uploadFile;
        while (uploadFile.exists()) {
            uploadFile = new File(uploadDir + '/' + file.getFileNameWithoutExtension() + '_' + i++ + '.' + file.getExtension());
        }
        existingUploadFile.renameTo(uploadFile);
        uploadFile = new File(uploadDir + '/' + file.getFileName());
        return uploadFile;
    }

    /**
     * Reads properties related to the multi-part request. The only properties that can be provided are
     *  - name
     *  - filename
     * Content-Type is something related to the request, not the multi-part, but it's collected here anyway.
     * There is some index magic, like the -2, which corresponds to getting rid of CRLF chars.
     * It's fast, but might be error-prone for weird uploads.
     * @throws IOException
     */
    private void readPropertiesUntilEmptyLine() throws IOException {
        for (; bytesReadAsLine > 2; bytesReadAsLine = input.readLine(line, 0, BUFFER_SIZE)) {

            bytesRead += bytesReadAsLine;
            String newLine = new String(line, 0, bytesReadAsLine);

            if (newLine.startsWith("Content-Disposition: form-data;")) {
//                propertyName = newLine.substring(newLine.indexOf("name=") + 6, newLine.indexOf('\"', newLine.indexOf("name=") + 6));
                if (newLine.contains("filename")) {
                    fullFileName = newLine.substring(newLine.indexOf("filename=") + 10, newLine.indexOf('\"', newLine.indexOf("filename=") + 10));
                }
            }
//            if (newLine.startsWith("Content-Type: ")) {
//                contentType = newLine.substring(newLine.indexOf("Content-Type: ") + 14, newLine.length() - 2);
//            }
        }
        bytesRead += bytesReadAsLine;
    }

//    /**
//     * The file object is set as a property on the request through this method. This way one can retrieve the file at
//     * a later stage from the request.
//     */
//    private void setFileAsObjectOnRequest() {
//        FileData file = new FileData(fullFileName, contentType);
//        file.setDescription("Obtained from: " + fullFileName);
//        file.setRawData(((ByteArrayOutputStream) partialCopyOutputStream).toByteArray());
//        //propertyName is specified in HTML form like <INPUT TYPE="FILE" NAME="myPropertyName">
//        request.setAttribute(propertyName, file);
//        System.out.println(new LogEntry("file found in multi-part data: " + file));
//    }

//    /**
//     * Similar to setFileAsObjectOnRequest, but this just sets the value as a key value pair property on the request.
//     */
//    private void setValueAsPropertyOnRequest() {
//        String propertyValue = partialCopyOutputStream.toString();
//        request.setAttribute(propertyName, propertyValue);
//        System.out.println(new LogEntry("property found in multi-part data:"  + propertyName + '=' + propertyValue));
//    }

    public void close() {
        if(input != null) {
            try {
                input.close();
            } catch (IOException e) {
                System.out.println(new LogEntry(Level.VERBOSE, "error when closing upload stream", e));
            }
        }
        if(partialCopyOutputStream != null) {
            try {
                partialCopyOutputStream.close();
            } catch (IOException e) {
                System.out.println(new LogEntry(Level.VERBOSE, "error when closing upload stream", e));
            }
        }
    }

    public void cancel() {
        close();
        if(uploadFile != null) {
            uploadFile.delete();
        }
        bytesRead = 0;
        contentLength = 0;
    }

    public void reset() {
//        request = null;
        input = null;
        partialCopyOutputStream = null;
//        propertyName = null;
        fullFileName = null;
//        contentType = null;
        line = new byte[BUFFER_SIZE];
        bytesReadAsLine = 0;
        contentLength = 0;
        bytesRead = 0;
        uploadFile = null;
        boundary = null;
    }
}
