package org.ijsberg.iglu.util.http;

import com.sun.net.ssl.HttpsURLConnection;
import org.ijsberg.iglu.logging.Level;
import org.ijsberg.iglu.logging.LogEntry;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
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


	public static void main(String[] args) throws IOException {
		HttpResponse response = sendPost("http://localhost:17682/hub/post_metric_value", "{\"value\": 40}", "application/json");
		System.out.println(response.getResponse());
	}

	public static class HttpResponse {
		private int status;
		private String response;

		public HttpResponse(int status, String response) {
			this.status = status;
			this.response = response;
		}

		public int getStatus() {
			return status;
		}

		public String getResponse() {
			return response;
		}
	}


	public static HttpResponse sendPost(String url, String postData, String contentType) throws IOException {

		URL obj = new URL(url);
		HttpURLConnection con = null;
		String response;
		int status;
		try {
			con = (HttpURLConnection) obj.openConnection();
			con.setRequestProperty("Method", "POST");
			con.setRequestProperty("Content-Type", contentType);
			con.setConnectTimeout(5000);

			con.setDoOutput(true);
			DataOutputStream wr = new DataOutputStream(con.getOutputStream());
			wr.writeBytes(postData);
			wr.flush();
			wr.close();

			status = con.getResponseCode();
			response = con.getResponseMessage();
			System.out.println("[" + status + "]");
		} finally {
			if(con != null) {
				con.disconnect();
			}
		}
		return new HttpResponse(status, response);
	}
}

