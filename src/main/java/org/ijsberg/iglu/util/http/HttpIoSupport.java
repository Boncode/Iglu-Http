package org.ijsberg.iglu.util.http;

import java.io.IOException;
import java.net.Socket;

public class HttpIoSupport {

    public static boolean isPortAvailable(int port) {
        try (Socket ignored = new Socket("localhost", port)) {
            return false;
        } catch (IOException ignored) {
            return true;
        }
    }
}
