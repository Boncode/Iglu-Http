package org.ijsberg.iglu.server.facilities;

public interface FileNameChecker {
    /**
     *
     * @param fileName
     * @throws InvalidFilenameException if file name is not allowed
     */
    void assertFileNameAllowed(String fileName) throws InvalidFilenameException;
}
