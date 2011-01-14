package org.genepattern.server.webapp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadBase;
import org.apache.commons.fileupload.RequestContext;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.servlet.ServletRequestContext;
import org.apache.log4j.Logger;
import org.genepattern.util.GPConstants;

public class MultiFileUploadReceiver extends HttpServlet {
	private static final long serialVersionUID = -6720003935924717973L;

	private static Logger log =  Logger.getLogger(MultiFileUploadReceiver.class);
	
	private int partitionCount;
	private int partitionNumber;
	private long fileLength;
	private String partitionedFileName;
	private String userName;
	private PrintWriter responseWriter;
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		userName =  (String) request.getSession().getAttribute(GPConstants.USERID);
		
		responseWriter = response.getWriter();
		
		RequestContext reqContext = new ServletRequestContext(request);
		if (FileUploadBase.isMultipartContent(reqContext)){
			FileItemFactory factory = new DiskFileItemFactory();
			ServletFileUpload upload = new ServletFileUpload(factory);			
			try{
				List<FileItem> postParametrs = upload.parseRequest(reqContext);
				readPartitionInfo(postParametrs);
				if (partitionCount == 1){
					loadFile (postParametrs);
				}else{
					loadPartition(request, postParametrs);
				}		
			}
			catch (Exception e) {
				e.printStackTrace();
			} 	
			responseWriter.close();
		}else{
			//This servlet wasn't called by a multi-file uploader.  Return an error page.
			response.sendRedirect(request.getContextPath() + "/pages/internalError.jsf");
		}
	}
	private void loadPartition(HttpServletRequest request, List<FileItem> postParameters) throws Exception {
		String directory = "";
		if (partitionNumber == 0){
			//Create a temporary directory for the uploaded files.
			String prefix = userName + "_run";
			//use createTempFile to guarantee a unique name, but then change it to a directory
			File tempDir = File.createTempFile(prefix, null);
			tempDir.delete();
			tempDir.mkdir();
			request.getSession().setAttribute("StagingDir", tempDir.getCanonicalPath());
			directory = tempDir.getCanonicalPath();
		}else{
			directory = (String) request.getSession().getAttribute("StagingDir");
		}
		
		Iterator<FileItem> it = postParameters.iterator();
		while (it.hasNext()){
			FileItem postParameter = it.next();
			if (!postParameter.isFormField()){
				File file = new File(directory, postParameter.getName()+"."+partitionNumber);
				postParameter.write(file);
				break;											
			}
		}	
		
		if  (partitionNumber == (partitionCount -1)){
			File wholeFile = new File(directory, partitionedFileName);
			OutputStream out = new FileOutputStream(wholeFile);
			byte[] buffer = new byte[65536];
			for (int i=0; i <partitionCount; i++){
				File portion = new File(directory, partitionedFileName + "."+ i);
				FileInputStream in = new FileInputStream(portion);
				int len;
				while ( (len=in.read(buffer)) > 0){
					out.write(buffer,0,len);
				}
				in.close();				
				portion.delete();
			}
			out.close();	
			if (wholeFile.length() == fileLength){
				responseWriter.println(wholeFile.getCanonicalPath());
			}else{
				responseWriter.println("Error: Upload validation error");
			}
		}
				
	}
	private void loadFile(List<FileItem> postParameters) throws Exception {
		
		//Create a temporary directory for the uploaded files.
		String prefix = userName + "_run";
		//use createTempFile to guarantee a unique name, but then change it to a directory
		File tempDir = File.createTempFile(prefix, null);
		tempDir.delete();
		tempDir.mkdir();
		
		Iterator<FileItem> it = postParameters.iterator();
		while (it.hasNext()){
			FileItem postParameter = it.next();
			if (!postParameter.isFormField()){
				File file = new File(tempDir, postParameter.getName());
				postParameter.write(file);
				responseWriter.println(file.getCanonicalPath());							
			}
		}		
	}
	private void readPartitionInfo(List<FileItem> uploadedFiles) {
		Iterator<FileItem> it = uploadedFiles.iterator();
		while (it.hasNext()){
			FileItem postParameter = it.next();
			if (postParameter.isFormField()){
				if ("partitionCount".compareTo(postParameter.getFieldName())==0){
					partitionCount = Integer.parseInt(postParameter.getString());
				}else if ("partitionIndex".compareTo(postParameter.getFieldName())==0){
					partitionNumber = Integer.parseInt(postParameter.getString());
				}else if ("fileName".compareTo(postParameter.getFieldName())==0){
					partitionedFileName = postParameter.getString();
				}else if ("fileLength".compareTo(postParameter.getFieldName())==0){
					fileLength = Long.parseLong(postParameter.getString());
				}
			}
		}		
	}

}
