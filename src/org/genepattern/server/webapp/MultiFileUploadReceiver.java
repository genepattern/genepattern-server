package org.genepattern.server.webapp;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadBase;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.RequestContext;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.servlet.ServletRequestContext;
import org.genepattern.util.GPConstants;

public class MultiFileUploadReceiver extends HttpServlet {
	private static final long serialVersionUID = -6720003935924717973L;

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String userName =  (String) request.getSession().getAttribute(GPConstants.USERID);

		//Create a temporary directory for the uploaded files.
		String prefix = userName + "_run";
		//use createTempFile to guarantee a unique name, but then change it to a directory
		File tempDir = File.createTempFile(prefix, null);
		tempDir.delete();
		tempDir.mkdir();
		
		PrintWriter out = response.getWriter();
		
		RequestContext reqContext = new ServletRequestContext(request);
		if (FileUploadBase.isMultipartContent(reqContext)){
			FileItemFactory factory = new DiskFileItemFactory();
			ServletFileUpload upload = new ServletFileUpload(factory);
			try{
				List<FileItem> uploadedFiles = upload.parseRequest(reqContext);
				Iterator<FileItem> it = uploadedFiles.iterator();
				while (it.hasNext()){
					FileItem uploadedFile = it.next();
					if (!uploadedFile.isFormField()){
						File file = new File(tempDir, uploadedFile.getName());
						try {
							uploadedFile.write(file);
							out.println(file.getCanonicalPath());							
						} catch (Exception e) {
							e.printStackTrace();
						}
					} else{
						//Files submitted by our uploader should never be form fields.  Ignore them.
					}
				}		
			}
			catch (FileUploadException e) {
				e.printStackTrace();
			}		
			out.close();
		}else{
			//This servlet wasn't called by a multi-file uploader.  Return an error page.
			response.sendRedirect(request.getContextPath() + "/pages/internalError.jsf");
		}
	}

}
