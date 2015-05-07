/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

package org.genepattern.webservice;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.httpclient.methods.GetMethod;
import org.genepattern.io.IOUtil;
import org.genepattern.util.GPConstants;
import org.genepattern.util.JobDownloader;

/**
 * Encapsulates information about a job run on a GenePattern server.
 * 
 * @author Joshua Gould
 */
public class JobResult {
    private URL server;

    private int jobNumber;

    private String[] fileNames;

    private boolean stdout;

    private boolean stderr;

    private Parameter[] parameters;

    private String lsid;

    private String username;

    private String password;

    /**
     * Creates a new JobResult instance.
     * 
     * @param server
     *                The server on which this job was run.
     * @param jobNumber
     *                The job number of this job.
     * @param fileNames
     *                The array of file names that this job created.
     * @param stdout
     *                Whether a standard out file was created.
     * @param stderr
     *                Whether a standard err file was created.
     * @param parameters
     *                The parameters that this job was run with.
     * @param lsid
     *                The LSID of the module that produced this job result.
     */
    public JobResult(URL server, int jobNumber, String[] fileNames, boolean stdout, boolean stderr,
	    Parameter[] parameters, String lsid, String username, String password) {
	this.server = server;
	this.jobNumber = jobNumber;
	this.fileNames = fileNames;
	this.stdout = stdout;
	this.stderr = stderr;
	this.parameters = parameters;
	this.lsid = lsid;
	this.username = username;
	this.password = password;

    }

    /**
     * Gets the LSID of the module that produced this job result.
     * 
     * @return The module LSID
     */
    public String getLSID() {
	return lsid;
    }

    /**
     * Allocates a new array containing the file names of the output files that this job created excluding the standard
     * output file and the standard error file.
     * 
     * @return The output file names
     */
    public String[] getOutputFileNames() {
	return fileNames.clone();
    }

    /**
     * Allocates a new array containing the input parameters that this job was invoked with
     * 
     * @return The parameters
     */
    public Parameter[] getParameters() {
	return parameters.clone();
    }

    /**
     * Returns the url to download the file that was created in the given creation order
     * 
     * @param creationOrder
     *                The file creation order, starting at 0.
     * @return The url to retrieve the file from.
     * @throws IllegalArgumentException
     *                 if <code>creationOrder < 0</code> or <code>creatorOrder >= getOutputFileNames().length</code>
     */
    public URL getURL(int creationOrder) {
	try {
	    if (creationOrder < 0 || creationOrder >= fileNames.length) {
		throw new IllegalArgumentException("Creation order out of range.");
	    }
	    return new URL(getServerURL() + "/gp/jobResults/" + getJobNumber() + "/"
		    + URLEncoder.encode(fileNames[creationOrder], "UTF-8"));
	} catch (MalformedURLException x) {
	    throw new Error(x);
	} catch (java.io.UnsupportedEncodingException uee) {
	    throw new Error("Unable to encode " + fileNames[creationOrder]);
	}
    }

    /**
     * Returns the url to download the given file name.
     * 
     * @param fileName
     *                The file name.
     * @return The url to retrieve the file from.
     */
    public URL getURLForFileName(String fileName) {
	try {
	    return new URL(getServerURL() + "/gp/jobResults/" + getJobNumber() + "/"
		    + URLEncoder.encode(fileName, "UTF-8"));
	} catch (MalformedURLException x) {
	    throw new Error(x);
	} catch (java.io.UnsupportedEncodingException uee) {
	    throw new Error("Unable to encode " + fileName);
	}
    }

    /**
     * Returns the url to download the given file output file type from.
     * 
     * @param fileType
     *                The file type (e.g. gct)
     * @return The url to retrieve the file from or <tt>null</tt> if a file with the given type could not be found.
     */
    public URL getURL(String fileType) {
	for (int i = 0; i < fileNames.length; i++) {
	    String fileName = fileNames[i];
	    if (fileName.toLowerCase().endsWith(".odf")) {
		GetMethod get = null;
		try {
		    JobDownloader d = new JobDownloader(server.toString(), username, password);
		    get = d.getGetMethod(jobNumber, fileName);
		    String modelType = IOUtil.getOdfModelType(get.getResponseBodyAsStream());
		    if (fileType.equalsIgnoreCase(modelType)) {
			return getURLForFileName(fileName);
		    }
		} catch (IOException e) {
		    // ignore
		} finally {
		    if (get != null) {
			get.releaseConnection();
		    }
		}
	    }
	    String endsWithString = fileType;
	    if (endsWithString.indexOf('.') == -1) {
		endsWithString = "." + endsWithString;
	    }
	    if (fileName.toLowerCase().endsWith(endsWithString.toLowerCase())) {
		return getURLForFileName(fileName);
	    }

	}
	return null;

    }

    /**
     * Tests whether this job wrote to the standard error stream.
     * 
     * @return <tt>true</> if this job wrote to the standard error stream; <tt>
     *      false</tt> otherwise.
     */
    public boolean hasStandardError() {
	return stderr;
    }

    /**
     * Tests whether this job wrote to the standard output stream.
     * 
     * @return <tt>true</> if this job wrote to the standard output stream; <tt>
     *      false</tt> otherwise.
     */
    public boolean hasStandardOut() {
	return stdout;
    }

    /**
     * Downloads all the available result files from the server to the given download directory. If the directory
     * already exists and contains files of the same name, it will overwrite the files.
     * 
     * @param downloadDirectory
     *                The pathname of a directory to create the files. The directory will be created if it does not
     *                exist.
     * @return The array of files.
     * @throws IOException
     *                 If an error occurs while downloading the files.
     * @throws NullPointerException
     *                 If the <code>downloadDirectory</code> argument is <code>null</code>
     */

    public File[] downloadFiles(String downloadDirectory) throws IOException {
	return downloadFiles(downloadDirectory, true);
    }

    /**
     * Downloads all the available result files from the server to the given download directory. If the directory
     * already exists and contains files of the same name, it will overwrite the files if <code>overwrite</code> is
     * <code>true</code>, and if <true>false</code> it will prepend job_#_ to the file name of any preexisting file.
     * 
     * 
     * @param downloadDirectory
     *                The pathname of a directory to create the files. The directory will be created if it does not
     *                exist.
     * @param overwrite
     *                overwrite existing files if true.
     * @return The array of files.
     * @throws IOException
     *                 If an error occurs while downloading the files.
     * @throws NullPointerException
     *                 If the <code>downloadDirectory</code> argument is <code> null</code>
     */

    public File[] downloadFiles(String downloadDirectory, boolean overwrite) throws IOException {

	if (downloadDirectory == null) {
	    throw new NullPointerException();
	}

	List<File> files = new ArrayList<File>();
	for (int i = 0, length = fileNames.length; i < length; i++) {
	    File f = downloadFile(fileNames[i], downloadDirectory, overwrite);
	    if (f != null) {
		files.add(f);
	    }
	}
	if (stdout) {
	    File f = downloadFile(GPConstants.STDOUT, downloadDirectory, overwrite);
	    if (f != null) {
		files.add(f);
	    }
	}
	if (stderr) {
	    File f = downloadFile(GPConstants.STDERR, downloadDirectory, overwrite);
	    if (f != null) {
		files.add(f);
	    }
	}
	return (File[]) files.toArray(new File[0]);
    }

    /**
     * Downloads the result file from the server to the given download directory. The name of the downloaded file will
     * be equal to the given file name. If a file of the same name already exists in the given directory it will be
     * overwritten.
     * 
     * @param fileName
     *                The file name.
     * @param downloadDirectory
     *                The pathname of a directory to create the file. The directory will be created if it does not
     *                exist.
     * @return The file or <code>null</code> if the file does not exist on the server.
     * @throws IOException
     *                 If an error occurs while downloading the file.
     * @throws NullPointerException
     *                 If the <code>fileName</code> or <code>downloadDirectory</code> arguments are
     *                 <code>null</code>
     */
    public File downloadFile(String fileName, String downloadDirectory) throws IOException {
	return downloadFile(fileName, downloadDirectory, true);
    }

    /**
     * Downloads the result file from the server to the given download directory. The name of the downloaded file will
     * be equal to the given file name. If overwrite is fale, if a file already existis of the same name in the given
     * directory then the file will be written with job_#_ prefixing the filename. If a file of this name already exists
     * it will be overwritten.
     * 
     * @param filename
     *                The file name.
     * @param downloadDirectory
     *                The pathname of a directory to create the file. The directory will be created if it does not
     *                exist.
     * @param overwrite
     *                overwrite existing files of the same name
     * @return The file or <code>null</code> if the file does not exist on the server.
     * @throws IOException
     *                 If an error occurs while downloading the file.
     * @throws NullPointerException
     *                 If the <code>fileName</code> or <code>downloadDirectory</code> arguments are
     *                 <code>null</code>
     */

    public File downloadFile(String filename, String downloadDirectory, boolean overwrite) throws IOException {
	if (filename == null || downloadDirectory == null) {
	    throw new NullPointerException();
	}
	File dir = new File(downloadDirectory);
	if (!dir.exists()) {
	    dir.mkdirs();
	}

	try {
	    JobDownloader d = new JobDownloader(server.toString(), username, password);
	    File outputFile = new File(dir, filename);
	    if (outputFile.exists() && !overwrite) {
		String name = "job_" + getJobNumber() + "_" + filename;
		outputFile = new File(downloadDirectory, name);
		if (outputFile.exists()) {
		    String suffix = null;
		    String prefix = name;
		    int dotIndex = name.lastIndexOf(".");
		    if (dotIndex != -1) {
			prefix = name.substring(0, dotIndex);
			suffix = name.substring(dotIndex, name.length());

		    }
		    if (prefix.length() < 3) {
			prefix += "tmp";
		    }
		    try {
			outputFile = File.createTempFile(prefix, suffix, dir);
		    } catch (IOException e) {
			throw new IOException("Unable to create temp file.");
		    }
		}
	    } else if (outputFile.exists() && overwrite) {
		outputFile.delete();
	    }

	    d.download(jobNumber, filename, outputFile);
	    return outputFile;
	} catch (IOException e) {
	    throw new IOException("Error downloading " + filename + " for job " + getJobNumber() + ".");
	}

    }

    /**
     * Gets the url of the server that this job was run on.
     * 
     * @return The server url.
     */

    public URL getServerURL() {
	return server;
    }

    /**
     * Gets the job number for this job.
     * 
     * @return The job number
     */
    public int getJobNumber() {
	return jobNumber;
    }
}
