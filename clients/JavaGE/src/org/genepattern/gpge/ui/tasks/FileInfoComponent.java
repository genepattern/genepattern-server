package org.genepattern.gpge.ui.tasks;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import org.genepattern.io.*;
import java.text.NumberFormat;

/**
 * Displays information about a selected file
 * 
 * @author Joshua Gould
 */
public class FileInfoComponent extends JLabel {
	static String NAME = "Name";

	static String KIND = "Kind";

	static String SIZE = "Size";

   private String semanticType;
   private String kind;
   
   public String getSemanticType() {
      return semanticType;   
   }
   
   public String getKind() {
      return kind;   
   }
   
	public FileInfoComponent() {
      setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		setVisible(false);
	}

	public void select(File file) throws IOException {
		if (file == null) {
			setText("");
			setVisible(false);
			return;
		}
		setVisible(true);
		kind = null;
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(file);
			String[] info = FileSummaryReader.getKindAndType(fis, file.getName());
         kind = info[0];
         semanticType = info[1];

		} finally {
			if (fis != null) {
				try {
					fis.close();
				} catch (IOException x) {
				}
			}
		}
		StringBuffer text = new StringBuffer();
		text.append("<html>");
		text.append("<p>Name: ");
		text.append(file.getName());
		text.append("<p>Kind: ");
		text.append(kind);
		text.append("<p>Size: ");
		text.append(FileSummaryReader.getSize(file.length()));
		setText(text.toString());
	}

	public void select(java.net.URLConnection conn, String name)
			throws IOException {
		if (conn == null) {
			setVisible(false);
			setText("");
			return;
		}
		setVisible(true);
		kind = null;
		InputStream is = null;
		try {
			is = conn.getInputStream();
			String[] info = FileSummaryReader.getKindAndType(is, name);
         kind = info[0];
         semanticType = info[1];
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException x) {
				}
			}
		}

		StringBuffer text = new StringBuffer();
		text.append("<html>");
		text.append("<p>Name: ");
		text.append(name);

		text.append("<p>Kind: ");
		text.append(kind);
		text.append("<p>Size: ");
		text.append(FileSummaryReader.getSize(conn.getContentLength()));
		setText(text.toString());
	}

	static class OdfSummaryHandler implements IOdfHandler {
		String model;

		public void endHeader() throws ParseException {
			throw new ParseException("");
		}

		public void header(String key, String[] values) throws ParseException {
		}

		public void header(String key, String value) throws ParseException {
			if (key.equalsIgnoreCase("Model")) {
				model = value;
				throw new ParseException("");
			}
		}

		public void data(int row, int column, String s) throws ParseException {
			throw new ParseException("");
		}

	}

	static class FileSummaryReader {
		static Map extension2KindMap = new HashMap();
		static NumberFormat numberFormat;

		public static String[] getKindAndType(InputStream is, String name)
				throws IOException {
         OdfParser odfParser = new OdfParser();
         OdfSummaryHandler odfHandler = new OdfSummaryHandler();
         
			int dotIndex = name.lastIndexOf(".");
			String kind = "";
         String type = null;
			if (dotIndex != -1 && dotIndex != (name.length() - 1)) {
				String extension = name.substring(dotIndex + 1, name.length());
				extension = extension.toLowerCase();
            type = extension;
				kind = (String) extension2KindMap.get(extension);
				if (kind == null) {
					kind = extension;
				}
			}
			if (kind.equals("odf")) {
				try {
					odfParser.parse(is);
				} catch (ParseException e) {
				}
				kind = odfHandler.model;
            type = kind;
				if (kind == null) {
					kind = "Unknown";
				}
			}
			return new String[]{kind, type};
		}

		public static String getSize(long lengthInBytes) {
			String size = null;
			if (lengthInBytes >= 1073741824) {
				double gigabytes = lengthInBytes / 1073741824.0;
				size = numberFormat.format(gigabytes) + " GB";
			} else if (lengthInBytes >= 1048576) {
				double megabytes = lengthInBytes / 1048576.0;
				size = numberFormat.format(megabytes) + " MB";
			} else {
				size = Math.ceil(lengthInBytes / 1024.0) + " KB";
			}

			return size;
		}

		static {
			extension2KindMap.put("jpeg", "Image");
			extension2KindMap.put("jpg", "Image");
			extension2KindMap.put("gif", "Image");
			extension2KindMap.put("png", "Image");
			extension2KindMap.put("bmp", "Image");
			extension2KindMap.put("tiff", "Image");
			extension2KindMap.put("cls", "Class Vector");
			extension2KindMap.put("res", "Dataset");
			extension2KindMap.put("gct", "Dataset");
			extension2KindMap.put("zip", "Zip Archive");
			extension2KindMap.put("txt", "Text Document");
			extension2KindMap.put("doc", "Word Document");
			extension2KindMap.put("xls", "Excel Document");

			// FIXME, pol, gmt, xxx

			numberFormat = NumberFormat.getInstance();
			numberFormat.setMaximumFractionDigits(1);

		}
	}
}