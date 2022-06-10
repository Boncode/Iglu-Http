package org.ijsberg.iglu.server.facilities;

import org.ijsberg.iglu.rest.RestException;

public interface FileNameChecker {
    /**
     *
     * @param fileName
     * @throws RestException if file name is not allowed
     */
    void assertFileNameAllowed(String fileName) throws RestException;
}
