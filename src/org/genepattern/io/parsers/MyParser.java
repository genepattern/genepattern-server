package org.genepattern.io.parsers;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import org.genepattern.data.DataModel;
import org.genepattern.data.DataObjector;
import org.genepattern.io.DefaultSummaryInfo;
import org.genepattern.io.SummaryInfo;
import org.genepattern.io.TextData;

public abstract class MyParser implements DataParser {
	String[] ext;
	String name;


	public MyParser(String[] ext, String name) {
		this.ext = ext;
		this.name = name;
	}


	public boolean canDecode(final InputStream in) throws IOException {
		return false;
	}


	public DataObjector parse(final InputStream in, final String name) throws IOException, ParseException {
		throw new UnsupportedOperationException("This method shouldn't be getting called");
	}


	public final String toString() {
		return name;
	}


	public String[] getFileExtensions() {
		return ext;
	}


	public String getFullHeader(final InputStream in) throws IOException, ParseException {
		return "";
	}


	public static class HtmlParser extends MyParser {
		DataModel htmlModel = new DataModel("HTML");


		public HtmlParser() {
			super(new String[]{"html", "htm"}, "html");
		}


		public SummaryInfo createSummary(final InputStream in) throws IOException, ParseException {
			final Map primary = new HashMap(3);
			primary.put("Size=", (long) (Math.ceil(in.available() / 1024.0)) + " KB");
			return new DefaultSummaryInfo(primary, null, htmlModel);
		}
	}


	public static class ImageParser extends MyParser {

		DataModel imageModel = new DataModel("Image");


		public ImageParser() {
			super(new String[]{"jpeg", "jpg", "png", "gif", "bmp", "tiff"}, "Image");
		}


		public SummaryInfo createSummary(final InputStream in) throws IOException, ParseException {
			final Map primary = new HashMap(1);
			primary.put("Size=", (long) (Math.ceil(in.available() / 1024.0)) + " KB");
			return new DefaultSummaryInfo(primary, null, imageModel);
		}
	}


	public static class TextParser extends MyParser {

		DataModel model = TextData.DATA_MODEL;


		public TextParser() {
			super(new String[]{"txt", "stdout", "sterr"}, "Text");
		}


		public SummaryInfo createSummary(final InputStream in) throws IOException, ParseException {
			final Map primary = new HashMap(1);
			primary.put("Size=", (long) (Math.ceil(in.available() / 1024.0)) + " KB");
			return new DefaultSummaryInfo(primary, null, model);
		}
	}


	public static class ExcelParser extends MyParser {

		DataModel model = new DataModel("Spreadsheet");


		public ExcelParser() {
			super(new String[]{"xls"}, "Spreadsheet");
		}


		public SummaryInfo createSummary(final InputStream in) throws IOException, ParseException {
			final Map primary = new HashMap(1);
			primary.put("Size=", (long) (Math.ceil(in.available() / 1024.0)) + " KB");
			return new DefaultSummaryInfo(primary, null, model);
		}
	}

}

