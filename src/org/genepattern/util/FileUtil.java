/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

package org.genepattern.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;


/**
 * A static class of convenience functions for Files
 **/
//TODO Fill in javadoc
/**
 * @author qgao: modified from Matthew Wrobel's CMAP classes
 *
 */
public class FileUtil {	
	/**
	 * @return
	 * @throws IOException
	 */
	public static File createTempDirectory() throws IOException
	{
	    File tempFile = File.createTempFile("TEMP", null);
	    tempFile.delete();
	    tempFile.mkdirs();
	    return tempFile;
	}
	
	  
	/**
	 * @param directory
	 * @throws IOException
	 */
	public static void deleteDirectory(File directory) throws IOException
	{
		if (!directory.isDirectory()) {
		   	directory.delete();   	
		}else {
		   	File directories[] = directory.listFiles();
		   	for (int i = 0; i < directories.length; i++)
		    {
		    	deleteDirectory(directories[i]);
		    }
		}
	    // delete self.
	    directory.delete();
	}
	
	/**
	 * @param file
	 * @param tempDir
	 * @throws Exception
	 */
	public static void copyToDirectory(String file, File tempDir) throws Exception
	{
		File in = new File(file);
	    File out = new File(tempDir, in.getName());
	    copyFile(in, out);
	}
	
	/**
	 * @param in
	 * @param out
	 */
	private static void copyFile(File in, File out)
	{
		try {
			FileChannel inChannel = null, outChannel = null;
			try	{
				out.getParentFile().mkdirs();
				inChannel = new FileInputStream(in).getChannel();
				outChannel = new FileOutputStream(out).getChannel();
				outChannel.transferFrom(inChannel, 0, inChannel.size());
			} finally {
				if (inChannel != null)
				{
					inChannel.close();
				}
				if (outChannel != null)
				{
					outChannel.close();
				}
			}
	   }catch (Exception e)  {
		   	throw new Error(e);
	   }
	}
}
