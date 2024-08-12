package org.ijsberg.iglu.server.facilities.model;

public class MultipartUploadProgress {

    private final String uploadId;

    private final long bytesProcessed;
    private final long totalSizeInBytes;

    private final UploadStatus uploadStatus;


    public MultipartUploadProgress(String uploadId, long bytesProcessed, long totalSizeInBytes, UploadStatus uploadStatus) {
        this.uploadId = uploadId;
        this.bytesProcessed = bytesProcessed;
        this.totalSizeInBytes = totalSizeInBytes;
        this.uploadStatus = uploadStatus;
    }

    public long getBytesProcessed() {
        return bytesProcessed;
    }

    public long getTotalSizeInBytes() {
        return totalSizeInBytes;
    }

    public UploadStatus getUploadStatus() {
        return uploadStatus;
    }

    public String getUploadId() {
        return uploadId;
    }
}
