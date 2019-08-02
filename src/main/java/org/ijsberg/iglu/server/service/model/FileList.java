package org.ijsberg.iglu.server.service.model;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.List;

public class FileList {

    private List<String> fileList;

    @JsonCreator
    public FileList(List<String> fileList) {
        this.fileList = fileList;
    }

    public List<String> getFileList() {
        return fileList;
    }
}
