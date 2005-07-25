package org.genepattern.gpge.util;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
/**
 * Sends a post request
 * @author Joshua Gould
 *
 */
public class PostData {
	StringBuffer sb;
	int i;
	String urlString;

	public PostData(String url) {
		sb = new StringBuffer();
		this.urlString = url;
	}

	public void addPostData(String key, String value) throws IOException {
		if (i > 0) {
			sb.append("&");
		}
		sb.append(URLEncoder.encode(key, "UTF-8") + "="
				+ URLEncoder.encode(value, "UTF-8"));
		i++;
	}
	
	public String toString() {
		return urlString + "?" + sb.toString();
	}

	public void send() throws IOException {
		// Send data
		URL url = new URL(urlString);
		URLConnection conn = url.openConnection();
		conn.setDoOutput(true);
		OutputStreamWriter wr = null;
		try {
			wr = new OutputStreamWriter(conn.getOutputStream());
			wr.write(sb.toString());
			wr.flush();
		} finally {
			if (wr != null) {
				wr.close();
			}
		}

	}

}
