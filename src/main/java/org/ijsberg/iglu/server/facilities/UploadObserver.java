package org.ijsberg.iglu.server.facilities;

import java.io.File;

public interface UploadObserver {
    void onUploadDone(File fileName);
}
