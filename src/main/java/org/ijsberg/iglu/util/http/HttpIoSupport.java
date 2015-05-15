package org.ijsberg.iglu.util.http;

import org.ijsberg.iglu.logging.Level;
import org.ijsberg.iglu.logging.LogEntry;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

/**
 */
public class HttpIoSupport {

	public static String getDataByHttp(String urlStr, int nrofTries) {

		for(int i = 0; i < nrofTries; i++) {
			String result = getDataByHttp(urlStr);
			if(result != null) {
				System.out.println(new LogEntry("data obtained in trial " + (i + 1)));
				return result;
			}
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				//ignore
			}
		}
		return null;

	}


	public static String getDataByHttp(String urlStr) {

		StringBuffer result = new StringBuffer();
		URL url = null;
		URLConnection yc = null;
		try {
			url = new URL(urlStr);
			System.out.println(new LogEntry("opening connection to " + urlStr));
			yc = url.openConnection();
			yc.setConnectTimeout(60 * 1000);
			yc.setReadTimeout(60 * 1000);
			yc.connect();
			BufferedReader in = new BufferedReader(
					new InputStreamReader(
							yc.getInputStream()));
			String inputLine;

			while ((inputLine = in.readLine()) != null)  {
				result.append(inputLine + "\n");
			}
			in.close();
			System.out.println(new LogEntry("connection to " + urlStr + " closed"));

		} catch (IOException e) {
			System.out.println(new LogEntry(Level.CRITICAL, "unable to obtain data from " + urlStr, e));
			return null;
		}
		return result.toString();
	}
}
