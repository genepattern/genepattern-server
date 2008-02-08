/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2008) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.

 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

package org.genepattern.gpge.ui.maindisplay;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;

import org.genepattern.io.DatasetHandler;
import org.genepattern.io.DatasetParser;
import org.genepattern.io.ParseException;
import org.genepattern.io.cls.ClsReader;
import org.genepattern.io.gct.GctParser;
import org.genepattern.io.odf.OdfHandler;
import org.genepattern.io.odf.OdfParser;
import org.genepattern.io.res.ResParser;
import org.genepattern.matrix.DefaultClassVector;

/**
 * Displays information about a selected file
 * 
 * 
 */
public class FileInfoUtil {
    static NumberFormat numberFormat;

    static OdfSummaryHandler odfSummaryHandler = new OdfSummaryHandler();

    static MyDatasetHandler datasetHandler = new MyDatasetHandler();

    static {
	numberFormat = NumberFormat.getInstance();
	numberFormat.setMaximumFractionDigits(1);
    }

    static Map extensionToReaderMap;

    static {
	extensionToReaderMap = new HashMap();

	GctParser gctParser = new GctParser();
	gctParser.setHandler(datasetHandler);
	extensionToReaderMap.put("gct", gctParser);

	ResParser resParser = new ResParser();
	resParser.setHandler(datasetHandler);
	extensionToReaderMap.put("res", resParser);

	ClsReader clsReader = new ClsReader();
	extensionToReaderMap.put("cls", clsReader);

	OdfParser odfParser = new OdfParser();
	odfParser.setHandler(odfSummaryHandler);
	extensionToReaderMap.put("odf", odfParser);
    }

    private static FileInfo _getInfo(String pathname, InputStream is) {
	int dotIndex = pathname.lastIndexOf(".");
	Object parser = null;
	FileInfo fileInfo = new FileInfo();

	if (dotIndex != -1) {// see if file has an extension
	    String suffix = pathname.substring(dotIndex + 1, pathname.length());
	    suffix = suffix.toLowerCase();
	    parser = extensionToReaderMap.get(suffix);
	    fileInfo.setKind(suffix);
	}
	try {
	    if (parser instanceof DatasetParser) {
		((DatasetParser) parser).parse(is);

	    } else if (parser instanceof ClsReader) {

		ClsReader clsReader = (ClsReader) parser;
		DefaultClassVector cv = clsReader.read(is);

		// keyValuePairs.add(new KeyValuePair("Number of Classes",
		// String.valueOf(cv.getClassCount())));
		fileInfo.setAnnotation(new KeyValuePair("Data Points", String.valueOf(cv.size())));
	    } else if (parser instanceof OdfParser) {
		((OdfParser) parser).parse(is);

	    }
	} catch (EndParseException epe) {// ignore
	    if (parser instanceof DatasetParser) {
		fileInfo.setAnnotation(datasetHandler.getKeyValuePair());
	    } else if (parser instanceof OdfParser) {
		fileInfo.setKind(odfSummaryHandler.getModel());
		fileInfo.setAnnotation(odfSummaryHandler.getKeyValuePair());
	    }
	} catch (ParseException pe) {
	    pe.printStackTrace();
	    String message = pe.getMessage();
	    fileInfo.setAnnotation(new KeyValuePair("Error", message));
	} catch (IOException ioe) {
	    ioe.printStackTrace();
	    fileInfo.setAnnotation(new KeyValuePair("Error", "Unable to parse file"));
	} catch (Throwable t) {
	    t.printStackTrace();
	    fileInfo.setAnnotation(new KeyValuePair("Error", "Unable to parse file"));
	}
	return fileInfo;
    }

    public static FileInfo getInfo(File file) {
	if (file == null) {
	    return null;
	}
	if (!file.exists()) {
	    String pathname = file.getName();
	    int dotIndex = pathname.lastIndexOf(".");
	    FileInfo fileInfo = new FileInfo();

	    if (dotIndex != -1) {// see if file has an extension
		String suffix = pathname.substring(dotIndex + 1, pathname.length());
		suffix = suffix.toLowerCase();
		fileInfo.setKind(suffix);

	    } else {
		fileInfo.setKind(null);
	    }
	    fileInfo.setSize(null);
	    return fileInfo;
	}
	String size = getSize(file.length());
	FileInputStream fis = null;
	try {
	    fis = new FileInputStream(file);
	    FileInfo fileInfo = _getInfo(file.getName(), fis);
	    fileInfo.setSize(size);
	    return fileInfo;
	} catch (IOException ioe) {
	    ioe.printStackTrace();
	} finally {
	    if (fis != null) {
		try {
		    fis.close();
		} catch (IOException x) {
		}
	    }
	}
	return null;
    }

    /**
     * Create FileInfo when the content of the file is not available.
     * @param name - the name of the file
     * @param length - best guess of the size of the file in bytes (-1 if not known).
     * @return
     */
    public static FileInfo getInfo(String name, long length) {
        FileInfo fileInfo = new FileInfo();
        String size = getSize(length);
        fileInfo.setSize(size);

        // see if file has an extension
        int dotIndex = name.lastIndexOf(".");
        if (dotIndex != -1) {
            String suffix = name.substring(dotIndex + 1, name.length());
            suffix = suffix.toLowerCase();
            fileInfo.setKind(suffix);
        }
        return fileInfo;
    }
    

    public static FileInfo getInfo(InputStream is, String name, long length) {
	try {
	    if (length == -1) {
		length = is.available();
	    }
	    String size = getSize(length);
	    FileInfo fileInfo = _getInfo(name, is);
	    fileInfo.setSize(size);
	    return fileInfo;
	} catch (IOException ioe) {
	    ioe.printStackTrace();
	} finally {
	    if (is != null) {
		try {
		    is.close();
		} catch (IOException x) {
		}
	    }
	}
	return null;

    }

    public static FileInfo getInfo(URL url, String name) {
	if (url == null) {
	    return null;
	}

	try {
	    URLConnection conn = url.openConnection();
	    long length = Long.parseLong(conn.getHeaderField("content-length"));
	    return getInfo(conn.getInputStream(), name, length);
	} catch (IOException e) {
	    e.printStackTrace();
	}
	return null;

    }

    private static String getSize(long lengthInBytes) {
	if (lengthInBytes <= 0) {
	    return "";
	}
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

    static class KeyValuePair {
	Object key;

	Object value;

	public KeyValuePair(String key, String value) {
	    this.key = key;
	    this.value = value;
	}

	public String toString() {
	    return key + ": " + value;
	}

    }

    /**
     * Description of the Class
     * 
     * 
     */
    public static class FileInfo {
	private String kind = "";

	private String size = "";

	private KeyValuePair annotation = null;

	public String getSize() {
	    return size;
	}

	public String getKind() {
	    return kind;
	}

	public void setSize(String _size) {
	    this.size = _size;
	    if (this.size == null) {
		this.size = "";
	    }
	}

	public void setKind(String _kind) {
	    this.kind = _kind;
	    if (this.kind == null) {
		this.kind = "";
	    }
	}

	public KeyValuePair getAnnotation() {
	    return annotation;
	}

	public void setAnnotation(KeyValuePair s) {
	    annotation = s;
	}

	public String toString() {
	    return "kind " + kind + " size " + size;
	}
    }

    private static class MyDatasetHandler implements DatasetHandler {
	int rows, columns;

	public KeyValuePair getKeyValuePair() {
	    return new KeyValuePair("Dimensions", String.valueOf(rows) + " rows x " + String.valueOf(columns)
		    + " columns");

	}

	public void data(int i, int j, double d) throws ParseException {
	    throw new EndParseException();
	}

	public void columnName(int j, String name) throws ParseException {
	    throw new EndParseException();
	}

	public void rowName(int i, String name) throws ParseException {
	    throw new EndParseException();
	}

	public void init(int rows, int columns, String[] rowMetaDataNames, String[] columnMetaDataNames,
		String[] matrices) throws ParseException {
	    this.rows = rows;
	    this.columns = columns;
	    throw new EndParseException();
	}

	public void data(int row, int column, int depth, double s) throws ParseException {
	    throw new EndParseException();
	}

	public void rowMetaData(int row, int depth, String s) throws ParseException {
	    throw new EndParseException();
	}

	public void columnMetaData(int column, int depth, String s) throws ParseException {
	    throw new EndParseException();
	}

	public void beginRow(int rowIndex) throws ParseException {
	}

    }

    private static class EndParseException extends ParseException {
	public EndParseException() {
	    super("");
	}
    }

    private static class OdfSummaryHandler implements OdfHandler {
	String model;

	String rows, columns;

	String numFeatures, numErrors, numCorrect;

	public OdfSummaryHandler() {
	}

	public KeyValuePair getKeyValuePair() {
	    if ("Dataset".equals(model)) {
		return new KeyValuePair("Dimensions", rows + " rows x " + columns + " columns");
	    } else if ("Prediction Results".equals(model)) {
		int total = Integer.parseInt(numCorrect) + Integer.parseInt(numErrors);
		return new KeyValuePair("Accuracy", numCorrect + "/" + total + " correct");
	    } else if ("Prediction Features".equals(model)) {
		return new KeyValuePair("Features", numFeatures);
	    }
	    return null;
	}

	public void endHeader() throws ParseException {
	    throw new EndParseException();
	}

	public void header(String key, String[] values) throws ParseException {
	    if (key.equals("COLUMN_NAMES")) {
		columns = String.valueOf(values.length);
	    }
	}

	public String getModel() {
	    return model;
	}

	public void header(String key, String value) throws ParseException {
	    if (key.equalsIgnoreCase("Model")) {
		model = value;
	    } else if (key.equalsIgnoreCase("DataLines")) {
		rows = value;
	    } else if (key.equalsIgnoreCase("NumFeatures")) {
		numFeatures = value;
	    } else if (key.equalsIgnoreCase("NumErrors")) {
		numErrors = value;
	    } else if (key.equalsIgnoreCase("NumCorrect")) {
		numCorrect = value;
	    }

	}

	public void data(int row, int column, String s) throws ParseException {
	    throw new EndParseException();
	}

    }

}
