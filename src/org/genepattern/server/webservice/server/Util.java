package org.genepattern.server.webservice.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;

import org.genepattern.webservice.WebServiceException;

/**
 * Utilities methods
 * 
 * @author Joshua Gould
 * 
 */
public class Util {
	private Util() {
	}

	public static File downloadUrl(String url) throws WebServiceException {
		File file = null;
		java.io.OutputStream os = null;
		java.io.InputStream is = null;
		boolean deleteFile = false;
		try {

			if (url.startsWith("file://")) {
				String fileStr = url.substring(7, url.length());
				file = new File(fileStr);
			} else {
				deleteFile = true;
				file = File.createTempFile("gpz", ".zip");
				os = new java.io.FileOutputStream(file);
				is = new java.net.URL(url).openStream();
				byte[] buf = new byte[100000];
				int i;
				while ((i = is.read(buf, 0, buf.length)) > 0) {
					os.write(buf, 0, i);

				}
			}
		} catch (java.io.IOException ioe) {
			throw new WebServiceException(ioe);
		} finally {
			if (os != null) {
				try {
					os.close();
				} catch (java.io.IOException x) {
				}
			}
			if (is != null) {
				try {
					is.close();
				} catch (java.io.IOException x) {
				}
			}

		}
		return file;
	}

	public static void copyFile(File source, File dest) {
		byte[] buf = new byte[100000];
		int j;
		FileOutputStream os = null;
		FileInputStream is = null;
		try {
			os = new FileOutputStream(dest);
			is = new FileInputStream(source);
			while ((j = is.read(buf, 0, buf.length)) > 0) {
				os.write(buf, 0, j);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (is != null) {
					is.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				if (os != null) {
					os.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static File getAxisFile(DataHandler dh) {
		javax.activation.DataSource ds = dh.getDataSource();
		if (ds instanceof FileDataSource) { // if local
			return ((FileDataSource) ds).getFile();
		}
		// if through SOAP org.apache.axis.attachments.ManagedMemoryDataSource
		return new File(dh.getName());
	}
}
